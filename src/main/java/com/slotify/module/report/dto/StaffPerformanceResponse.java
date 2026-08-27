package com.slotify.module.report.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.util.List;

/**
 * Per-staff figures of a period, sorted by revenue.
 *
 * @param period the requested period
 * @param currency ISO-4217 currency of every amount
 * @param staff one row per staff member of the salon (inactive members included)
 */
public record StaffPerformanceResponse(
    ReportPeriod.PeriodDto period, String currency, List<Row> staff) {

  /**
   * Figures of one staff member.
   *
   * @param staffId staff id
   * @param displayName staff display name
   * @param active whether the staff member is currently bookable
   * @param bookings bookings assigned to the staff member starting in the period
   * @param completed COMPLETED bookings
   * @param noShows NO_SHOW bookings
   * @param revenueMinor revenue of completed bookings
   * @param ratingAvg current average rating (0-5)
   * @param utilisationPercent booked minutes divided by scheduled shift minutes
   */
  @Schema(name = "StaffPerformanceResponseRow")
  public record Row(
      Long staffId,
      String displayName,
      boolean active,
      long bookings,
      long completed,
      long noShows,
      long revenueMinor,
      BigDecimal ratingAvg,
      double utilisationPercent) {}
}
