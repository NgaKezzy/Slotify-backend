package com.slotify.module.staff.dto;

import com.slotify.module.report.dto.ReportPeriod;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Payload of {@code GET /staff/stats}: personal figures of the signed-in staff member for a preset
 * period plus a per-day series for the earnings chart.
 *
 * @param range the preset the figures were computed for
 * @param period resolved first/last day of the range in the salon timezone
 * @param currency ISO-4217 currency of every amount
 * @param bookings bookings assigned to the staff member starting in the period (any status)
 * @param completed COMPLETED bookings in the period
 * @param noShows NO_SHOW bookings in the period
 * @param cancelled CANCELLED or REJECTED bookings in the period
 * @param upcoming open bookings (PENDING / CONFIRMED / IN_PROGRESS) from now on, not limited to the
 *     period
 * @param revenueMinor sum of {@code total_minor} of completed bookings (minor units)
 * @param serviceMinutes total booked minutes of completed bookings
 * @param ratingAvg current average review rating (0-5)
 * @param ratingCount number of reviews
 * @param series one point per day of the period, in order
 */
public record StaffStatsResponse(
    StatsRange range,
    ReportPeriod.PeriodDto period,
    String currency,
    long bookings,
    long completed,
    long noShows,
    long cancelled,
    long upcoming,
    long revenueMinor,
    long serviceMinutes,
    BigDecimal ratingAvg,
    int ratingCount,
    List<DayPoint> series) {

  /**
   * Figures of one salon-local day.
   *
   * @param date the day
   * @param bookings bookings starting on that day (any status)
   * @param completed completed bookings on that day
   * @param revenueMinor revenue of completed bookings on that day
   */
  @Schema(name = "StaffStatsDayPoint")
  public record DayPoint(LocalDate date, long bookings, long completed, long revenueMinor) {}
}
