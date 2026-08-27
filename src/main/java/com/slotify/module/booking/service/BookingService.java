package com.slotify.module.booking.service;

import com.slotify.common.api.PageResponse;
import com.slotify.common.exception.AppException;
import com.slotify.common.exception.ErrorCode;
import com.slotify.module.booking.dto.BookingListFilter;
import com.slotify.module.booking.dto.BookingResponse;
import com.slotify.module.booking.dto.CreateBookingRequest;
import com.slotify.module.booking.dto.RescheduleBookingRequest;
import com.slotify.module.booking.dto.WalkInBookingRequest;
import com.slotify.module.booking.entity.Booking;
import com.slotify.module.booking.entity.BookingItem;
import com.slotify.module.booking.entity.BookingStatus;
import com.slotify.module.booking.entity.CancelledBy;
import com.slotify.module.booking.entity.PaymentMethod;
import com.slotify.module.booking.mapper.BookingMapper;
import com.slotify.module.booking.repository.BookingRepository;
import com.slotify.module.booking.repository.BookingSpecifications;
import com.slotify.module.notification.entity.NotificationType;
import com.slotify.module.payment.service.PaymentService;
import com.slotify.module.promotion.service.CouponService;
import com.slotify.module.salon.entity.Salon;
import com.slotify.module.salon.entity.SalonSettings;
import com.slotify.module.salon.repository.SalonRepository;
import com.slotify.module.salon.service.SalonAccess;
import com.slotify.module.service.entity.SalonService;
import com.slotify.module.staff.entity.Staff;
import com.slotify.module.staff.repository.StaffRepository;
import com.slotify.module.user.entity.Role;
import com.slotify.module.user.entity.User;
import com.slotify.module.user.repository.UserRepository;
import com.slotify.security.UserPrincipal;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Booking use cases for customers, salon owners and staff: create (with double-booking protection),
 * reschedule, and every status transition of plan §3.5.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class BookingService {

  private final BookingRepository bookingRepository;
  private final SalonRepository salonRepository;
  private final StaffRepository staffRepository;
  private final UserRepository userRepository;
  private final AvailabilityService availabilityService;
  private final BookingCodeGenerator codeGenerator;
  private final BookingLock bookingLock;
  private final BookingEvents events;
  private final SalonAccess salonAccess;
  private final BookingMapper mapper;
  private final CouponService couponService;
  private final PaymentService paymentService;
  private final Clock clock;

  // ---- Customer ------------------------------------------------------------------------------

  /** Creates a booking for the signed-in customer at an available slot. */
  public BookingResponse create(UserPrincipal principal, CreateBookingRequest request) {
    Salon salon =
        salonRepository
            .findById(request.salonId())
            .filter(Salon::isActive)
            .orElseThrow(() -> new AppException(ErrorCode.SALON_NOT_FOUND));
    User customer = requireUser(principal.id());
    assertPaymentMethodAccepted(salon.getSettings(), request.paymentMethod());

    List<SalonService> services = availabilityService.requireServices(salon, request.serviceIds());
    Duration duration = availabilityService.totalDuration(services);
    Staff staff = pickStaff(salon, services, request.staffId(), request.startAt(), duration, null);

    long subtotal = services.stream().mapToLong(SalonService::getPriceMinor).sum();
    CouponService.Applied coupon =
        request.couponCode() == null || request.couponCode().isBlank()
            ? null
            : couponService.resolve(salon, request.couponCode(), customer.getId(), subtotal);

    Booking booking =
        persistWithLock(
            new BookingDraft(
                salon, customer, staff, services, request.startAt(), duration, false, null),
            b -> {
              b.setStaffAutoAssigned(request.staffId() == null);
              b.setPaymentMethod(request.paymentMethod());
              b.setNote(request.note());
              b.setStatus(
                  salon.getSettings().isAutoConfirm()
                      ? BookingStatus.CONFIRMED
                      : BookingStatus.PENDING);
              if (coupon != null) {
                b.setCoupon(coupon.coupon());
                b.setDiscountMinor(coupon.discountMinor());
                b.recalculateTotals();
              }
            });
    if (coupon != null) {
      couponService.recordUsage(coupon.coupon(), customer.getId(), booking.getId());
    }
    events.created(booking);
    return mapper.forCustomer(booking);
  }

  @Transactional(readOnly = true)
  public PageResponse<BookingResponse> listMine(
      UserPrincipal principal, boolean upcoming, int page, int size) {
    PageRequest pageable =
        PageRequest.of(
            page,
            Math.min(size, 50),
            upcoming ? Sort.by("startAt").ascending() : Sort.by("startAt").descending());
    var result =
        upcoming
            ? bookingRepository.findAllByCustomerIdAndStatusIn(
                principal.id(), BookingStatus.UPCOMING, pageable)
            : bookingRepository.findAllByCustomerIdAndStatusNotIn(
                principal.id(), BookingStatus.UPCOMING, pageable);
    return PageResponse.from(result, mapper::forCustomer);
  }

  @Transactional(readOnly = true)
  public BookingResponse getMine(UserPrincipal principal, Long bookingId) {
    return mapper.forCustomer(requireMine(principal, bookingId));
  }

  /** Customer cancellation, allowed until {@code cancelBeforeMin} before the start. */
  public BookingResponse cancelByCustomer(UserPrincipal principal, Long bookingId, String reason) {
    Booking booking = requireMine(principal, bookingId);
    assertTransition(booking, BookingStatus.CANCELLED);
    Duration notice = Duration.ofMinutes(booking.getSalon().getSettings().getCancelBeforeMin());
    if (Instant.now(clock).plus(notice).isAfter(booking.getStartAt())) {
      throw new AppException(ErrorCode.BOOKING_CANCEL_TOO_LATE);
    }
    cancel(booking, CancelledBy.CUSTOMER, reason);
    paymentService.refundBooking(booking, "Cancelled by customer");
    events.statusChanged(booking, NotificationType.BOOKING_CANCELLED);
    return mapper.forCustomer(booking);
  }

  /** Customer reschedule to another available slot (same services, optional new staff). */
  public BookingResponse rescheduleByCustomer(
      UserPrincipal principal, Long bookingId, RescheduleBookingRequest request) {
    Booking booking = requireMine(principal, bookingId);
    if (!booking.getStatus().blocksCalendar()) {
      throw new AppException(ErrorCode.BOOKING_INVALID_STATE);
    }
    reschedule(booking, request.startAt(), request.staffId());
    events.rescheduled(booking);
    return mapper.forCustomer(booking);
  }

  // ---- Owner ---------------------------------------------------------------------------------

  /** Owner creates a booking for a walk-in / phone customer (no advance-notice rules). */
  public BookingResponse createWalkIn(
      UserPrincipal principal, Long salonId, WalkInBookingRequest request) {
    Salon salon = salonAccess.requireOwned(salonId, principal);
    User customer = resolveWalkInCustomer(request);
    List<SalonService> services = availabilityService.requireServices(salon, request.serviceIds());
    Duration duration = availabilityService.totalDuration(services);
    Staff staff = availabilityService.candidateStaff(salon, services, request.staffId()).getFirst();

    Booking booking =
        persistWithLock(
            new BookingDraft(
                salon, customer, staff, services, request.startAt(), duration, true, null),
            b -> {
              b.setPaymentMethod(
                  request.paymentMethod() == null ? PaymentMethod.CASH : request.paymentMethod());
              b.setNote(request.note());
              b.setStatus(BookingStatus.CONFIRMED);
            });
    events.created(booking);
    return mapper.forSalon(booking);
  }

  @Transactional(readOnly = true)
  public PageResponse<BookingResponse> listForSalon(
      UserPrincipal principal, Long salonId, BookingListFilter filter) {
    salonAccess.requireOwned(salonId, principal);
    var result =
        bookingRepository.findAll(
            BookingSpecifications.filter(
                salonId, filter.status(), filter.staffId(), filter.from(), filter.to(), filter.q()),
            PageRequest.of(
                filter.pageOrDefault(), filter.sizeOrDefault(), Sort.by("startAt").descending()));
    return PageResponse.from(result, mapper::forSalon);
  }

  /** Calendar feed: every booking of the salon in the range (for the timeline view). */
  @Transactional(readOnly = true)
  public List<BookingResponse> calendar(
      UserPrincipal principal, Long salonId, Instant from, Instant to) {
    salonAccess.requireOwned(salonId, principal);
    return bookingRepository
        .findAllBySalonIdAndStartAtBetweenOrderByStartAtAsc(salonId, from, to)
        .stream()
        .map(mapper::forSalon)
        .toList();
  }

  @Transactional(readOnly = true)
  public BookingResponse getForSalon(UserPrincipal principal, Long salonId, Long bookingId) {
    return mapper.forSalon(requireForSalon(principal, salonId, bookingId));
  }

  /** Owner-side status change (confirm, reject, cancel, start, complete, no-show). */
  public BookingResponse changeStatusBySalon(
      UserPrincipal principal, Long salonId, Long bookingId, BookingStatus target, String reason) {
    Booking booking = requireForSalon(principal, salonId, bookingId);
    applyTransition(booking, target, CancelledBy.SALON, reason);
    if (target == BookingStatus.CANCELLED || target == BookingStatus.REJECTED) {
      paymentService.refundBooking(booking, "Cancelled by the salon");
    }
    events.statusChanged(booking, notificationFor(target));
    return mapper.forSalon(booking);
  }

  public BookingResponse rescheduleBySalon(
      UserPrincipal principal, Long salonId, Long bookingId, RescheduleBookingRequest request) {
    Booking booking = requireForSalon(principal, salonId, bookingId);
    if (!booking.getStatus().blocksCalendar()) {
      throw new AppException(ErrorCode.BOOKING_INVALID_STATE);
    }
    reschedule(booking, request.startAt(), request.staffId());
    events.rescheduled(booking);
    return mapper.forSalon(booking);
  }

  // ---- Staff ---------------------------------------------------------------------------------

  @Transactional(readOnly = true)
  public List<BookingResponse> listForStaff(UserPrincipal principal, LocalDate date) {
    Staff staff = requireStaff(principal);
    var zone = staff.getSalon().zoneId();
    return bookingRepository
        .findAllByStaffIdAndStartAtBetweenOrderByStartAtAsc(
            staff.getId(),
            date.atStartOfDay(zone).toInstant(),
            date.plusDays(1).atStartOfDay(zone).toInstant())
        .stream()
        .map(mapper::forSalon)
        .toList();
  }

  /** Staff may start, complete or mark no-show on their own bookings. */
  public BookingResponse changeStatusByStaff(
      UserPrincipal principal, Long bookingId, BookingStatus target) {
    if (target != BookingStatus.IN_PROGRESS
        && target != BookingStatus.COMPLETED
        && target != BookingStatus.NO_SHOW) {
      throw new AppException(ErrorCode.FORBIDDEN);
    }
    Staff staff = requireStaff(principal);
    Booking booking =
        bookingRepository
            .findByIdAndStaffId(bookingId, staff.getId())
            .orElseThrow(() -> new AppException(ErrorCode.BOOKING_NOT_FOUND));
    applyTransition(booking, target, CancelledBy.SALON, null);
    events.statusChanged(booking, notificationFor(target));
    return mapper.forSalon(booking);
  }

  // ---- Core -----------------------------------------------------------------------------------

  /**
   * Everything needed to insert a booking.
   *
   * @param skipCustomerRules true for owner-created bookings (no advance-notice / horizon rules)
   * @param ignoreBookingId booking being rescheduled, excluded from the overlap check
   */
  private record BookingDraft(
      Salon salon,
      User customer,
      Staff staff,
      List<SalonService> services,
      Instant startAt,
      Duration duration,
      boolean skipCustomerRules,
      Long ignoreBookingId) {

    Instant endAt() {
      return startAt.plus(duration);
    }

    LocalDate day() {
      return startAt.atZone(salon.zoneId()).toLocalDate();
    }
  }

  /**
   * Inserts the booking under the staff/day lock after a final, row-locked overlap check. This is
   * the only place a booking can be created, so the double-booking guarantee lives here.
   */
  private Booking persistWithLock(
      BookingDraft draft, java.util.function.Consumer<Booking> customizer) {
    if (!draft.skipCustomerRules()) {
      assertSlotAvailable(
          draft.salon(),
          draft.staff(),
          draft.day(),
          draft.services(),
          draft.startAt(),
          draft.ignoreBookingId());
    }
    return bookingLock.withLock(
        draft.staff().getId(),
        draft.day(),
        () -> {
          List<Booking> clashes =
              bookingRepository.findOverlappingForUpdate(
                  draft.staff().getId(), BookingStatus.BLOCKING, draft.startAt(), draft.endAt());
          boolean clash =
              clashes.stream()
                  .anyMatch(
                      b ->
                          draft.ignoreBookingId() == null
                              || !b.getId().equals(draft.ignoreBookingId()));
          if (clash) {
            throw new AppException(ErrorCode.SLOT_UNAVAILABLE);
          }
          Booking booking =
              Booking.create(
                  uniqueCode(),
                  draft.salon(),
                  draft.customer(),
                  draft.staff(),
                  draft.startAt(),
                  draft.endAt());
          draft.services().forEach(s -> booking.addItem(BookingItem.from(s)));
          customizer.accept(booking);
          return bookingRepository.save(booking);
        });
  }

  private void reschedule(Booking booking, Instant newStart, Long newStaffId) {
    Salon salon = booking.getSalon();
    List<SalonService> services =
        booking.getItems().stream()
            .map(BookingItem::getService)
            .filter(java.util.Objects::nonNull)
            .toList();
    if (services.size() != booking.getItems().size()) {
      throw new AppException(ErrorCode.BOOKING_INVALID_STATE);
    }
    Duration duration = booking.duration();
    Long staffId = newStaffId != null ? newStaffId : booking.getStaff().getId();
    Staff staff = pickStaff(salon, services, staffId, newStart, duration, booking.getId());
    Instant newEnd = newStart.plus(duration);
    LocalDate day = newStart.atZone(salon.zoneId()).toLocalDate();

    bookingLock.withLock(
        staff.getId(),
        day,
        () -> {
          boolean clash =
              bookingRepository
                  .findOverlappingForUpdate(staff.getId(), BookingStatus.BLOCKING, newStart, newEnd)
                  .stream()
                  .anyMatch(b -> !b.getId().equals(booking.getId()));
          if (clash) {
            throw new AppException(ErrorCode.SLOT_UNAVAILABLE);
          }
          booking.setStaff(staff);
          booking.setStaffAutoAssigned(false);
          booking.setStartAt(newStart);
          booking.setEndAt(newEnd);
          return null;
        });
  }

  /** Chooses the staff member: the requested one, or the least busy one free at that time. */
  private Staff pickStaff(
      Salon salon,
      List<SalonService> services,
      Long staffId,
      Instant startAt,
      Duration duration,
      Long ignore) {
    List<Staff> candidates = availabilityService.candidateStaff(salon, services, staffId);
    LocalDate day = startAt.atZone(salon.zoneId()).toLocalDate();
    List<Staff> free =
        candidates.stream()
            .filter(
                s ->
                    availabilityService.slotsFor(salon, s, day, duration, ignore).contains(startAt))
            .toList();
    if (free.isEmpty()) {
      throw new AppException(ErrorCode.SLOT_UNAVAILABLE);
    }
    if (free.size() == 1) {
      return free.getFirst();
    }
    Instant dayStart = day.atStartOfDay(salon.zoneId()).toInstant();
    Instant dayEnd = day.plusDays(1).atStartOfDay(salon.zoneId()).toInstant();
    return free.stream()
        .min(
            Comparator.comparingLong(
                s ->
                    bookingRepository.countByStaffIdAndStatusInAndStartAtBetween(
                        s.getId(), BookingStatus.BLOCKING, dayStart, dayEnd)))
        .orElseThrow();
  }

  private void assertSlotAvailable(
      Salon salon,
      Staff staff,
      LocalDate day,
      List<SalonService> services,
      Instant startAt,
      Long ignore) {
    Duration duration = availabilityService.totalDuration(services);
    if (!availabilityService.slotsFor(salon, staff, day, duration, ignore).contains(startAt)) {
      throw new AppException(ErrorCode.SLOT_UNAVAILABLE);
    }
  }

  private void applyTransition(
      Booking booking, BookingStatus target, CancelledBy by, String reason) {
    assertTransition(booking, target);
    if (target == BookingStatus.CANCELLED) {
      cancel(booking, by, reason);
    } else {
      booking.transitionTo(target);
      if (target == BookingStatus.REJECTED) {
        booking.setCancelReason(reason);
      }
    }
  }

  private static void cancel(Booking booking, CancelledBy by, String reason) {
    booking.transitionTo(BookingStatus.CANCELLED);
    booking.setCancelledBy(by);
    booking.setCancelReason(reason);
  }

  private static void assertTransition(Booking booking, BookingStatus target) {
    if (!booking.getStatus().canTransitionTo(target)) {
      throw new AppException(ErrorCode.BOOKING_INVALID_STATE);
    }
  }

  private static NotificationType notificationFor(BookingStatus status) {
    return switch (status) {
      case CONFIRMED -> NotificationType.BOOKING_CONFIRMED;
      case REJECTED -> NotificationType.BOOKING_REJECTED;
      case CANCELLED -> NotificationType.BOOKING_CANCELLED;
      case COMPLETED -> NotificationType.BOOKING_COMPLETED;
      case NO_SHOW -> NotificationType.BOOKING_NO_SHOW;
      case PENDING, IN_PROGRESS -> null;
    };
  }

  private static void assertPaymentMethodAccepted(SalonSettings settings, PaymentMethod method) {
    boolean accepted =
        switch (method) {
          case STRIPE -> settings.isAcceptStripe();
          case PAYPAL -> settings.isAcceptPaypal();
          case CASH -> settings.isAcceptCash();
        };
    if (!accepted) {
      throw new AppException(ErrorCode.PAYMENT_METHOD_NOT_ACCEPTED);
    }
  }

  private String uniqueCode() {
    String code;
    do {
      code = codeGenerator.next();
    } while (bookingRepository.existsByCode(code));
    return code;
  }

  /** Finds the customer by id / email or creates a guest record for walk-ins. */
  private User resolveWalkInCustomer(WalkInBookingRequest request) {
    if (request.customerId() != null) {
      return requireUser(request.customerId());
    }
    if (request.customerEmail() != null) {
      return userRepository
          .findByEmailIgnoreCase(request.customerEmail())
          .orElseGet(
              () -> userRepository.save(guest(request.customerEmail(), request.customerName())));
    }
    String guestEmail = "guest-" + UUID.randomUUID() + "@walk-in.local";
    return userRepository.save(guest(guestEmail, request.customerName()));
  }

  private static User guest(String email, String name) {
    String displayName = name == null || name.isBlank() ? "Walk-in customer" : name.trim();
    return User.local(email, null, displayName, Role.CUSTOMER);
  }

  private User requireUser(Long id) {
    return userRepository
        .findById(id)
        .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
  }

  private Booking requireMine(UserPrincipal principal, Long bookingId) {
    return bookingRepository
        .findByIdAndCustomerId(bookingId, principal.id())
        .orElseThrow(() -> new AppException(ErrorCode.BOOKING_NOT_FOUND));
  }

  private Booking requireForSalon(UserPrincipal principal, Long salonId, Long bookingId) {
    salonAccess.requireOwned(salonId, principal);
    return bookingRepository
        .findByIdAndSalonId(bookingId, salonId)
        .orElseThrow(() -> new AppException(ErrorCode.BOOKING_NOT_FOUND));
  }

  private Staff requireStaff(UserPrincipal principal) {
    return staffRepository
        .findByUserId(principal.id())
        .orElseThrow(() -> new AppException(ErrorCode.STAFF_NOT_FOUND));
  }
}
