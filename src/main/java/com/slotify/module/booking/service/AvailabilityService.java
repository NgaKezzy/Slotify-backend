package com.slotify.module.booking.service;

import com.slotify.common.exception.AppException;
import com.slotify.common.exception.ErrorCode;
import com.slotify.module.booking.dto.AvailabilityResponse;
import com.slotify.module.booking.entity.Booking;
import com.slotify.module.booking.entity.BookingStatus;
import com.slotify.module.booking.repository.BookingRepository;
import com.slotify.module.salon.entity.Salon;
import com.slotify.module.salon.entity.SalonOpeningHour;
import com.slotify.module.salon.entity.SalonSettings;
import com.slotify.module.salon.service.SalonQueryService;
import com.slotify.module.service.entity.SalonService;
import com.slotify.module.service.repository.SalonServiceRepository;
import com.slotify.module.staff.entity.Staff;
import com.slotify.module.staff.entity.StaffShift;
import com.slotify.module.staff.entity.StaffShiftOverride;
import com.slotify.module.staff.entity.StaffTimeOff;
import com.slotify.module.staff.entity.TimeOffStatus;
import com.slotify.module.staff.repository.StaffRepository;
import com.slotify.module.staff.repository.StaffShiftOverrideRepository;
import com.slotify.module.staff.repository.StaffShiftRepository;
import com.slotify.module.staff.repository.StaffTimeOffRepository;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Real-time availability: combines salon opening hours, staff shifts / overrides / time off and
 * existing bookings into the slots a customer can book (plan §3.4). The pure math lives in {@link
 * AvailabilityEngine}; this class loads the data and applies the salon's booking rules.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AvailabilityService {

  private final SalonQueryService salonQueryService;
  private final SalonServiceRepository serviceRepository;
  private final StaffRepository staffRepository;
  private final StaffShiftRepository shiftRepository;
  private final StaffShiftOverrideRepository overrideRepository;
  private final StaffTimeOffRepository timeOffRepository;
  private final BookingRepository bookingRepository;
  private final AvailabilityCache cache;
  private final Clock clock;

  /** Public availability lookup (cached for 60 s). */
  public AvailabilityResponse availability(
      String salonIdOrSlug, LocalDate date, List<Long> serviceIds, Long staffId) {
    Salon salon = salonQueryService.requireActive(salonIdOrSlug);
    String key = AvailabilityCache.key(salon.getId(), date, serviceIds, staffId);
    return cache
        .get(key)
        .orElseGet(
            () -> {
              AvailabilityResponse response = compute(salon, date, serviceIds, staffId, null);
              cache.put(key, response);
              return response;
            });
  }

  /**
   * Uncached computation used by the booking flow. {@code ignoreBookingId} excludes a booking being
   * rescheduled from the busy periods.
   */
  public AvailabilityResponse compute(
      Salon salon, LocalDate date, List<Long> serviceIds, Long staffId, Long ignoreBookingId) {
    List<SalonService> services = requireServices(salon, serviceIds);
    Duration duration = totalDuration(services);
    List<Staff> candidates = candidateStaff(salon, services, staffId);
    List<AvailabilityResponse.StaffOption> options =
        candidates.stream()
            .map(
                s ->
                    new AvailabilityResponse.StaffOption(
                        s.getId(), s.getDisplayName(), s.getTitle(), s.getAvatarUrl()))
            .toList();

    if (candidates.isEmpty() || !withinBookingHorizon(salon, date)) {
      return new AvailabilityResponse(
          date, salon.getTimezone(), (int) duration.toMinutes(), List.of(), options);
    }

    Map<Instant, Set<Long>> slotsByStart = new TreeMap<>();
    for (Staff staff : candidates) {
      for (Instant start : slotsFor(salon, staff, date, duration, ignoreBookingId)) {
        slotsByStart.computeIfAbsent(start, k -> new HashSet<>()).add(staff.getId());
      }
    }
    List<AvailabilityResponse.Slot> slots =
        slotsByStart.entrySet().stream()
            .map(
                e ->
                    new AvailabilityResponse.Slot(
                        e.getKey(), e.getValue().stream().sorted().toList()))
            .toList();
    return new AvailabilityResponse(
        date, salon.getTimezone(), (int) duration.toMinutes(), slots, options);
  }

  /** Slots of one staff member for one day. */
  public List<Instant> slotsFor(
      Salon salon, Staff staff, LocalDate date, Duration duration, Long ignoreBookingId) {
    ZoneId zone = salon.zoneId();
    List<TimeWindow> working = workingWindows(salon, staff, date, zone);
    if (working.isEmpty()) {
      return List.of();
    }
    Instant dayStart = date.atStartOfDay(zone).toInstant();
    Instant dayEnd = date.plusDays(1).atStartOfDay(zone).toInstant();

    List<TimeWindow> busy = new ArrayList<>();
    for (Booking b :
        bookingRepository.findOverlapping(
            List.of(staff.getId()), BookingStatus.BLOCKING, dayStart, dayEnd)) {
      if (ignoreBookingId == null || !b.getId().equals(ignoreBookingId)) {
        busy.add(new TimeWindow(b.getStartAt(), b.getEndAt()));
      }
    }
    for (StaffTimeOff off :
        timeOffRepository.findAllOverlapping(
            List.of(staff.getId()), TimeOffStatus.APPROVED, dayStart, dayEnd)) {
      busy.add(new TimeWindow(off.getStartAt(), off.getEndAt()));
    }

    SalonSettings settings = salon.getSettings();
    AvailabilityEngine.Rules rules =
        new AvailabilityEngine.Rules(
            Duration.ofMinutes(settings.getSlotIntervalMin()),
            Duration.ofMinutes(settings.getMinAdvanceBookingMin()));
    return AvailabilityEngine.slots(working, busy, duration, rules, Instant.now(clock));
  }

  /** Total minutes the services block, including buffers. */
  public Duration totalDuration(List<SalonService> services) {
    return Duration.ofMinutes(services.stream().mapToInt(SalonService::totalMinutes).sum());
  }

  /** Loads active services of the salon, failing when any id is unknown. */
  public List<SalonService> requireServices(Salon salon, List<Long> serviceIds) {
    List<SalonService> services =
        serviceRepository.findAllByIdInAndSalonIdAndActiveTrue(
            new HashSet<>(serviceIds), salon.getId());
    if (services.size() != new HashSet<>(serviceIds).size()) {
      throw new AppException(ErrorCode.SERVICE_NOT_FOUND);
    }
    return services;
  }

  /** Staff able to perform all services (or the requested one, if qualified). */
  public List<Staff> candidateStaff(Salon salon, List<SalonService> services, Long staffId) {
    Set<Long> serviceIds = services.stream().map(SalonService::getId).collect(Collectors.toSet());
    if (staffId == null) {
      return staffRepository.findActiveBySalonIdPerformingAllServices(
          salon.getId(), serviceIds, serviceIds.size());
    }
    Staff staff =
        staffRepository
            .findByIdAndSalonId(staffId, salon.getId())
            .filter(Staff::isActive)
            .orElseThrow(() -> new AppException(ErrorCode.STAFF_NOT_FOUND));
    Set<Long> offered =
        staff.getServices().stream().map(SalonService::getId).collect(Collectors.toSet());
    if (!offered.containsAll(serviceIds)) {
      throw new AppException(ErrorCode.STAFF_CANNOT_PERFORM_SERVICE);
    }
    return List.of(staff);
  }

  private boolean withinBookingHorizon(Salon salon, LocalDate date) {
    LocalDate today = LocalDate.now(clock.withZone(salon.zoneId()));
    return !date.isBefore(today)
        && !date.isAfter(today.plusDays(salon.getSettings().getMaxAdvanceDays()));
  }

  /** Staff shift (or override) for the day, intersected with the salon opening hours. */
  private List<TimeWindow> workingWindows(Salon salon, Staff staff, LocalDate date, ZoneId zone) {
    int day = SalonOpeningHour.toStoredDay(date.getDayOfWeek());

    List<TimeWindow> staffWindows = new ArrayList<>();
    Optional<StaffShiftOverride> override =
        overrideRepository.findAllByStaffIdInAndDate(List.of(staff.getId()), date).stream()
            .findFirst();
    if (override.isPresent()) {
      StaffShiftOverride o = override.get();
      if (o.isOff()) {
        return List.of();
      }
      AvailabilityEngine.localWindow(date, o.getStartTime(), o.getEndTime(), zone)
          .ifPresent(staffWindows::add);
    } else {
      for (StaffShift shift :
          shiftRepository.findAllByStaffIdInAndDayOfWeekOrderByStartTimeAsc(
              List.of(staff.getId()), day)) {
        AvailabilityEngine.localWindow(date, shift.getStartTime(), shift.getEndTime(), zone)
            .ifPresent(staffWindows::add);
      }
    }
    if (staffWindows.isEmpty()) {
      return List.of();
    }

    List<TimeWindow> salonWindows =
        salon.getOpeningHours().stream()
            .filter(h -> h.getDayOfWeek() == day && !h.isClosed())
            .flatMap(
                h ->
                    AvailabilityEngine.localWindow(date, h.getOpenTime(), h.getCloseTime(), zone)
                        .stream())
            .toList();
    return TimeWindow.intersect(TimeWindow.merge(staffWindows), TimeWindow.merge(salonWindows));
  }
}
