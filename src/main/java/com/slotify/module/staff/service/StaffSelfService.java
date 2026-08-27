package com.slotify.module.staff.service;

import com.slotify.common.exception.AppException;
import com.slotify.common.exception.ErrorCode;
import com.slotify.module.booking.entity.Booking;
import com.slotify.module.booking.entity.BookingItem;
import com.slotify.module.booking.entity.BookingStatus;
import com.slotify.module.booking.repository.BookingRepository;
import com.slotify.module.report.dto.ReportPeriod;
import com.slotify.module.staff.dto.StaffMeResponse;
import com.slotify.module.staff.dto.StaffScheduleResponse;
import com.slotify.module.staff.dto.StaffStatsResponse;
import com.slotify.module.staff.dto.StatsRange;
import com.slotify.module.staff.dto.TimeOffRequest;
import com.slotify.module.staff.dto.TimeOffResponse;
import com.slotify.module.staff.entity.Staff;
import com.slotify.module.staff.entity.TimeOffStatus;
import com.slotify.module.staff.mapper.StaffMapper;
import com.slotify.module.staff.repository.StaffRepository;
import com.slotify.module.staff.repository.StaffShiftOverrideRepository;
import com.slotify.module.staff.repository.StaffShiftRepository;
import com.slotify.module.staff.repository.StaffTimeOffRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Self-service operations of the Staff app ({@code /api/v1/staff/**}): profile, schedule and time
 * off of the staff member linked to the signed-in {@code STAFF} account.
 *
 * <p>Time off created here is {@link TimeOffStatus#PENDING} until the owner approves it. {@link
 * #stats} aggregates the staff member's own bookings for the earnings screen.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class StaffSelfService {

  private final StaffRepository staffRepository;
  private final StaffShiftRepository shiftRepository;
  private final StaffShiftOverrideRepository overrideRepository;
  private final StaffTimeOffRepository timeOffRepository;
  private final BookingRepository bookingRepository;
  private final StaffService staffService;
  private final StaffMapper mapper;
  private final Clock clock;

  /** Profile and salon summary of the signed-in staff member. */
  @Transactional(readOnly = true)
  public StaffMeResponse me(Long userId) {
    Staff staff = requireStaff(userId);
    return new StaffMeResponse(mapper.toResponse(staff), mapper.toSalonSummary(staff.getSalon()));
  }

  /** Weekly shifts plus per-date exceptions from today (salon local date) onwards. */
  @Transactional(readOnly = true)
  public StaffScheduleResponse schedule(Long userId) {
    Staff staff = requireStaff(userId);
    LocalDate today = LocalDate.now(clock.withZone(staff.getSalon().zoneId()));
    return new StaffScheduleResponse(
        mapper.toShiftResponses(
            shiftRepository.findAllByStaffIdOrderByDayOfWeekAscStartTimeAsc(staff.getId())),
        mapper.toOverrideResponses(
            overrideRepository.findAllByStaffIdAndDateGreaterThanEqualOrderByDateAsc(
                staff.getId(), today)));
  }

  @Transactional(readOnly = true)
  public List<TimeOffResponse> listTimeOff(Long userId) {
    Staff staff = requireStaff(userId);
    return mapper.toTimeOffResponses(
        timeOffRepository.findAllByStaffIdOrderByStartAtDesc(staff.getId()));
  }

  /** Requests an absence; it stays {@code PENDING} until the owner approves it. */
  public TimeOffResponse requestTimeOff(Long userId, TimeOffRequest request) {
    Staff staff = requireStaff(userId);
    return mapper.toResponse(staffService.createTimeOff(staff, request, TimeOffStatus.PENDING));
  }

  /** Withdraws one of the staff member's own absences. */
  public void deleteTimeOff(Long userId, Long timeOffId) {
    staffService.deleteTimeOff(requireStaff(userId), timeOffId);
  }

  /**
   * Personal statistics for a preset range (today / this week / this month) in the salon timezone.
   *
   * <p>Counts and revenue are computed from the bookings loaded once for the period (v1 volumes: at
   * most a month of one staff member's bookings). {@code upcoming} is a separate count that is not
   * limited to the period so the staff member always sees what is still ahead.
   */
  @Transactional(readOnly = true)
  public StaffStatsResponse stats(Long userId, StatsRange range) {
    Staff staff = requireStaff(userId);
    ZoneId zone = staff.getSalon().zoneId();
    ReportPeriod period = new ReportPeriod(range.from(clock, zone), range.to(clock, zone), zone);
    List<Booking> bookings =
        bookingRepository.findAllForStaffInWindow(
            staff.getId(), period.startInstant(), period.endInstant());
    Map<LocalDate, List<Booking>> byDay =
        bookings.stream()
            .collect(Collectors.groupingBy(b -> b.getStartAt().atZone(zone).toLocalDate()));

    List<StaffStatsResponse.DayPoint> series = new ArrayList<>();
    for (LocalDate date : period.dates()) {
      List<Booking> ofDay = byDay.getOrDefault(date, List.of());
      series.add(
          new StaffStatsResponse.DayPoint(
              date, ofDay.size(), countStatus(ofDay, COMPLETED_ONLY), revenue(ofDay)));
    }
    long upcoming =
        bookingRepository.countByStaffIdAndStatusInAndStartAtGreaterThanEqual(
            staff.getId(), OPEN_STATUSES, Instant.now(clock));
    return new StaffStatsResponse(
        range,
        period.toDto(),
        staff.getSalon().getCurrency(),
        bookings.size(),
        countStatus(bookings, COMPLETED_ONLY),
        countStatus(bookings, EnumSet.of(BookingStatus.NO_SHOW)),
        countStatus(bookings, EnumSet.of(BookingStatus.CANCELLED, BookingStatus.REJECTED)),
        upcoming,
        revenue(bookings),
        serviceMinutes(bookings),
        staff.getRatingAvg(),
        staff.getRatingCount(),
        series);
  }

  /** Statuses that still occupy the staff member's calendar. */
  private static final Set<BookingStatus> OPEN_STATUSES =
      EnumSet.of(BookingStatus.PENDING, BookingStatus.CONFIRMED, BookingStatus.IN_PROGRESS);

  private static final Set<BookingStatus> COMPLETED_ONLY = EnumSet.of(BookingStatus.COMPLETED);

  private static long countStatus(List<Booking> bookings, Set<BookingStatus> statuses) {
    return bookings.stream().filter(b -> statuses.contains(b.getStatus())).count();
  }

  /** Revenue only counts completed bookings, like the owner reports. */
  private static long revenue(List<Booking> bookings) {
    return bookings.stream()
        .filter(b -> b.getStatus() == BookingStatus.COMPLETED)
        .mapToLong(Booking::getTotalMinor)
        .sum();
  }

  private static long serviceMinutes(List<Booking> bookings) {
    return bookings.stream()
        .filter(b -> b.getStatus() == BookingStatus.COMPLETED)
        .flatMap(b -> b.getItems().stream())
        .mapToLong(BookingItem::getDurationMin)
        .sum();
  }

  /**
   * Resolves the staff row linked to the account.
   *
   * @throws AppException {@link ErrorCode#STAFF_NOT_FOUND} when the account is not linked to any
   *     staff member
   */
  private Staff requireStaff(Long userId) {
    return staffRepository
        .findByUserId(userId)
        .orElseThrow(() -> new AppException(ErrorCode.STAFF_NOT_FOUND));
  }
}
