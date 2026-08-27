package com.slotify.module.report.service;

import com.slotify.module.booking.entity.Booking;
import com.slotify.module.booking.entity.BookingItem;
import com.slotify.module.booking.entity.BookingStatus;
import com.slotify.module.report.dto.BookingsReportResponse;
import com.slotify.module.report.dto.DashboardResponse;
import com.slotify.module.report.dto.Granularity;
import com.slotify.module.report.dto.ReportPeriod;
import com.slotify.module.report.dto.RevenueReportResponse;
import com.slotify.module.report.dto.StaffPerformanceResponse;
import com.slotify.module.report.repository.BookedInterval;
import com.slotify.module.report.repository.ReportRepository;
import com.slotify.module.report.repository.StatusCount;
import com.slotify.module.salon.entity.Salon;
import com.slotify.module.salon.service.SalonAccess;
import com.slotify.module.staff.entity.Staff;
import com.slotify.module.staff.entity.StaffShift;
import com.slotify.module.staff.repository.StaffRepository;
import com.slotify.module.staff.repository.StaffShiftRepository;
import com.slotify.security.UserPrincipal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Salon-scoped reports for the owner admin panel (plan §3.7 {@code reports/*}).
 *
 * <p>Revenue is always the {@code total_minor} of COMPLETED bookings attributed to the booking's
 * start time in the salon timezone. Figures are computed on demand; the dashboard uses database
 * aggregates while the series endpoints load the period's bookings once (see {@link
 * ReportRepository#findInPeriod}).
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReportService {

  /** Statuses that occupy staff time for occupancy and utilisation. */
  static final Set<BookingStatus> OCCUPYING =
      EnumSet.of(
          BookingStatus.PENDING,
          BookingStatus.CONFIRMED,
          BookingStatus.IN_PROGRESS,
          BookingStatus.COMPLETED);

  private static final int TOP_SERVICES = 10;
  private static final String UNKNOWN_PAYMENT_METHOD = "UNKNOWN";

  private final SalonAccess salonAccess;
  private final ReportRepository reportRepository;
  private final StaffRepository staffRepository;
  private final StaffShiftRepository staffShiftRepository;
  private final Clock clock;

  /** Resolves the period in the salon timezone after checking access. */
  public ReportPeriod period(Salon salon, LocalDate from, LocalDate to) {
    return ReportPeriod.resolve(from, to, salon.zoneId(), clock);
  }

  /** KPIs of the period versus the previous one, today's agenda and the upcoming count. */
  public DashboardResponse dashboard(
      UserPrincipal principal, Long salonId, LocalDate from, LocalDate to) {
    Salon salon = salonAccess.requireOwned(salonId, principal);
    ReportPeriod current = period(salon, from, to);
    ReportPeriod previous = current.previous();

    List<StaffShift> shifts = activeStaffShifts(salonId);
    List<BookedInterval> intervals =
        reportRepository.findBookedIntervals(
            salonId, OCCUPYING, previous.startInstant(), current.endInstant());

    LocalDate today = LocalDate.now(clock.withZone(salon.zoneId()));
    ReportPeriod todayPeriod = new ReportPeriod(today, today, salon.zoneId());
    List<DashboardResponse.TodayBooking> todayBookings =
        reportRepository
            .findInPeriod(salonId, todayPeriod.startInstant(), todayPeriod.endInstant())
            .stream()
            .map(ReportService::toTodayBooking)
            .toList();
    long upcoming =
        reportRepository.countBySalonIdAndStatusInAndStartAtGreaterThanEqual(
            salonId, BookingStatus.UPCOMING, Instant.now(clock));

    return new DashboardResponse(
        current.toDto(),
        salon.getCurrency(),
        kpis(salonId, current, shifts, intervals),
        kpis(salonId, previous, shifts, intervals),
        todayBookings,
        upcoming);
  }

  /** Revenue series plus payment-method and service breakdowns. */
  public RevenueReportResponse revenue(
      UserPrincipal principal, Long salonId, LocalDate from, LocalDate to, Granularity g) {
    Salon salon = salonAccess.requireOwned(salonId, principal);
    ReportPeriod period = period(salon, from, to);
    List<Booking> completed =
        reportRepository.findInPeriod(salonId, period.startInstant(), period.endInstant()).stream()
            .filter(b -> b.getStatus() == BookingStatus.COMPLETED)
            .toList();

    Map<LocalDate, List<Booking>> byBucket =
        completed.stream()
            .collect(
                Collectors.groupingBy(
                    b -> SeriesBucketer.bucketOf(b.getStartAt(), period.zone(), g)));
    List<RevenueReportResponse.Point> series =
        SeriesBucketer.buckets(period, g).stream()
            .map(
                bucket -> {
                  List<Booking> inBucket = byBucket.getOrDefault(bucket, List.of());
                  return new RevenueReportResponse.Point(
                      bucket, sumTotal(inBucket), inBucket.size());
                })
            .toList();

    return new RevenueReportResponse(
        period.toDto(),
        g,
        salon.getCurrency(),
        sumTotal(completed),
        series,
        byPaymentMethod(completed),
        byService(completed));
  }

  /** Booking counts per status over time. */
  public BookingsReportResponse bookings(
      UserPrincipal principal, Long salonId, LocalDate from, LocalDate to, Granularity g) {
    Salon salon = salonAccess.requireOwned(salonId, principal);
    ReportPeriod period = period(salon, from, to);
    List<Booking> all =
        reportRepository.findInPeriod(salonId, period.startInstant(), period.endInstant());

    Map<LocalDate, List<Booking>> byBucket =
        all.stream()
            .collect(
                Collectors.groupingBy(
                    b -> SeriesBucketer.bucketOf(b.getStartAt(), period.zone(), g)));
    List<BookingsReportResponse.Point> series =
        SeriesBucketer.buckets(period, g).stream()
            .map(
                bucket -> {
                  List<Booking> inBucket = byBucket.getOrDefault(bucket, List.of());
                  return new BookingsReportResponse.Point(
                      bucket, inBucket.size(), countByStatus(inBucket));
                })
            .toList();
    return new BookingsReportResponse(period.toDto(), g, countByStatus(all), series);
  }

  /** Per-staff bookings, revenue, rating and utilisation, best earner first. */
  public StaffPerformanceResponse staffPerformance(
      UserPrincipal principal, Long salonId, LocalDate from, LocalDate to) {
    Salon salon = salonAccess.requireOwned(salonId, principal);
    ReportPeriod period = period(salon, from, to);
    List<Staff> staffMembers = staffRepository.findAllBySalonIdOrderByDisplayNameAsc(salonId);
    Map<Long, List<StaffShift>> shiftsByStaff =
        staffShiftRepository
            .findAllByStaffIdIn(staffMembers.stream().map(Staff::getId).toList())
            .stream()
            .collect(Collectors.groupingBy(s -> s.getStaff().getId()));
    Map<Long, List<BookedInterval>> intervalsByStaff =
        reportRepository
            .findBookedIntervals(salonId, OCCUPYING, period.startInstant(), period.endInstant())
            .stream()
            .collect(Collectors.groupingBy(BookedInterval::staffId));
    Map<Long, List<Booking>> bookingsByStaff =
        reportRepository.findInPeriod(salonId, period.startInstant(), period.endInstant()).stream()
            .filter(b -> b.getStaff() != null)
            .collect(Collectors.groupingBy(b -> b.getStaff().getId()));

    List<StaffPerformanceResponse.Row> rows = new ArrayList<>();
    for (Staff staff : staffMembers) {
      List<Booking> mine = bookingsByStaff.getOrDefault(staff.getId(), List.of());
      List<Booking> completed =
          mine.stream().filter(b -> b.getStatus() == BookingStatus.COMPLETED).toList();
      long available =
          OccupancyCalculator.availableMinutes(
              shiftsByStaff.getOrDefault(staff.getId(), List.of()), period);
      long booked =
          OccupancyCalculator.bookedMinutes(
              intervalsByStaff.getOrDefault(staff.getId(), List.of()), period);
      rows.add(
          new StaffPerformanceResponse.Row(
              staff.getId(),
              staff.getDisplayName(),
              staff.isActive(),
              mine.size(),
              completed.size(),
              mine.stream().filter(b -> b.getStatus() == BookingStatus.NO_SHOW).count(),
              sumTotal(completed),
              staff.getRatingAvg(),
              OccupancyCalculator.percent(booked, available)));
    }
    rows.sort(Comparator.comparingLong(StaffPerformanceResponse.Row::revenueMinor).reversed());
    return new StaffPerformanceResponse(period.toDto(), salon.getCurrency(), rows);
  }

  // ---------------------------------------------------------------------------------------------

  private DashboardResponse.Kpis kpis(
      Long salonId, ReportPeriod period, List<StaffShift> shifts, List<BookedInterval> intervals) {
    Instant from = period.startInstant();
    Instant to = period.endInstant();
    Map<BookingStatus, Long> counts = new EnumMap<>(BookingStatus.class);
    for (StatusCount row : reportRepository.countByStatus(salonId, from, to)) {
      counts.put(row.status(), row.count());
    }
    long revenue = reportRepository.sumRevenue(salonId, BookingStatus.COMPLETED, from, to);
    long completed = counts.getOrDefault(BookingStatus.COMPLETED, 0L);
    long booked = OccupancyCalculator.bookedMinutes(intervals, period);
    long available = OccupancyCalculator.availableMinutes(shifts, period);
    return new DashboardResponse.Kpis(
        revenue,
        counts.values().stream().mapToLong(Long::longValue).sum(),
        completed,
        counts.getOrDefault(BookingStatus.CANCELLED, 0L)
            + counts.getOrDefault(BookingStatus.REJECTED, 0L),
        counts.getOrDefault(BookingStatus.NO_SHOW, 0L),
        reportRepository.countNewCustomers(salonId, from, to),
        completed == 0 ? 0 : Math.round((double) revenue / completed),
        OccupancyCalculator.percent(booked, available));
  }

  private List<StaffShift> activeStaffShifts(Long salonId) {
    List<Long> staffIds =
        staffRepository.findAllBySalonIdAndActiveTrueOrderByDisplayNameAsc(salonId).stream()
            .map(Staff::getId)
            .toList();
    return staffIds.isEmpty() ? List.of() : staffShiftRepository.findAllByStaffIdIn(staffIds);
  }

  private static List<RevenueReportResponse.PaymentMethodRevenue> byPaymentMethod(
      List<Booking> completed) {
    Map<String, List<Booking>> groups =
        completed.stream()
            .collect(
                Collectors.groupingBy(
                    b ->
                        b.getPaymentMethod() == null
                            ? UNKNOWN_PAYMENT_METHOD
                            : b.getPaymentMethod().name(),
                    LinkedHashMap::new,
                    Collectors.toList()));
    return groups.entrySet().stream()
        .map(
            e ->
                new RevenueReportResponse.PaymentMethodRevenue(
                    e.getKey(), sumTotal(e.getValue()), e.getValue().size()))
        .sorted(
            Comparator.comparingLong(RevenueReportResponse.PaymentMethodRevenue::revenueMinor)
                .reversed())
        .toList();
  }

  private static List<RevenueReportResponse.ServiceRevenue> byService(List<Booking> completed) {
    Map<String, List<BookingItem>> byName =
        completed.stream()
            .flatMap(b -> b.getItems().stream())
            .collect(Collectors.groupingBy(BookingItem::getServiceName));
    return byName.entrySet().stream()
        .map(
            e -> {
              List<BookingItem> items = e.getValue();
              Long serviceId =
                  items.stream()
                      .map(BookingItem::getService)
                      .filter(s -> s != null)
                      .map(s -> s.getId())
                      .findFirst()
                      .orElse(null);
              long revenue = items.stream().mapToLong(BookingItem::getPriceMinor).sum();
              long bookings = items.stream().map(i -> i.getBooking().getId()).distinct().count();
              return new RevenueReportResponse.ServiceRevenue(
                  serviceId, e.getKey(), revenue, bookings);
            })
        .sorted(
            Comparator.comparingLong(RevenueReportResponse.ServiceRevenue::revenueMinor).reversed())
        .limit(TOP_SERVICES)
        .toList();
  }

  private static Map<BookingStatus, Long> countByStatus(List<Booking> bookings) {
    Map<BookingStatus, Long> counts = new EnumMap<>(BookingStatus.class);
    for (BookingStatus status : BookingStatus.values()) {
      counts.put(status, 0L);
    }
    for (Booking booking : bookings) {
      counts.merge(booking.getStatus(), 1L, Long::sum);
    }
    return counts;
  }

  private static long sumTotal(List<Booking> bookings) {
    return bookings.stream().mapToLong(Booking::getTotalMinor).sum();
  }

  private static DashboardResponse.TodayBooking toTodayBooking(Booking booking) {
    return new DashboardResponse.TodayBooking(
        booking.getId(),
        booking.getCode(),
        booking.getStartAt(),
        booking.getEndAt(),
        booking.getStatus(),
        booking.getCustomer().getFullName(),
        booking.getStaff() == null ? null : booking.getStaff().getDisplayName());
  }
}
