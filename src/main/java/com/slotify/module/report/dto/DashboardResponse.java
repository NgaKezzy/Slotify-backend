package com.slotify.module.report.dto;

import com.slotify.module.booking.entity.BookingStatus;
import java.time.Instant;
import java.util.List;

/**
 * Owner dashboard: KPIs of the period compared with the previous period of equal length, plus
 * today's agenda.
 *
 * @param period the requested period
 * @param currency ISO-4217 currency of every amount
 * @param current KPIs of the requested period
 * @param previous KPIs of the period immediately before, for trend arrows
 * @param todayBookings bookings starting today (salon time), earliest first
 * @param upcomingCount open bookings starting after now
 */
public record DashboardResponse(
    ReportPeriod.PeriodDto period,
    String currency,
    Kpis current,
    Kpis previous,
    List<TodayBooking> todayBookings,
    long upcomingCount) {

  /**
   * Key figures of one period.
   *
   * @param revenueMinor sum of {@code total_minor} of COMPLETED bookings
   * @param bookingsCount every booking starting in the period, any status
   * @param completedCount COMPLETED bookings
   * @param cancelledCount CANCELLED plus REJECTED bookings
   * @param noShowCount NO_SHOW bookings
   * @param newCustomers customers whose first booking at this salon is in the period
   * @param averageTicketMinor revenue divided by completed bookings (0 when none)
   * @param occupancyPercent booked minutes divided by scheduled shift minutes of active staff
   */
  public record Kpis(
      long revenueMinor,
      long bookingsCount,
      long completedCount,
      long cancelledCount,
      long noShowCount,
      long newCustomers,
      long averageTicketMinor,
      double occupancyPercent) {}

  /**
   * One row of today's agenda.
   *
   * @param id booking id
   * @param code human-readable booking code
   * @param startAt start (UTC)
   * @param endAt end (UTC)
   * @param status current status
   * @param customerName customer display name
   * @param staffName assigned staff display name, {@code null} when unassigned
   */
  public record TodayBooking(
      Long id,
      String code,
      Instant startAt,
      Instant endAt,
      BookingStatus status,
      String customerName,
      String staffName) {}
}
