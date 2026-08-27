package com.slotify.module.report.dto;

import com.slotify.module.booking.entity.BookingStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * Booking counts by status over time.
 *
 * @param period the requested period
 * @param granularity bucket size of {@code series}
 * @param totals counts of the whole period, one entry per status
 * @param series one point per bucket, empty buckets included
 */
public record BookingsReportResponse(
    ReportPeriod.PeriodDto period,
    Granularity granularity,
    Map<BookingStatus, Long> totals,
    List<Point> series) {

  /**
   * One bucket of the series.
   *
   * @param periodStart first day of the bucket
   * @param total bookings starting in the bucket
   * @param counts bookings per status (every status present, zero when none)
   */
  @Schema(name = "BookingsReportResponsePoint")
  public record Point(LocalDate periodStart, long total, Map<BookingStatus, Long> counts) {}
}
