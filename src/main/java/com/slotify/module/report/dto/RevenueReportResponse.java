package com.slotify.module.report.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;
import java.util.List;

/**
 * Revenue report built from COMPLETED bookings grouped by their start time.
 *
 * @param period the requested period
 * @param granularity bucket size of {@code series}
 * @param currency ISO-4217 currency of every amount
 * @param totalRevenueMinor revenue of the whole period
 * @param series one point per bucket, empty buckets included
 * @param byPaymentMethod revenue split by payment method ({@code UNKNOWN} when not recorded)
 * @param byService top 10 services by revenue
 */
public record RevenueReportResponse(
    ReportPeriod.PeriodDto period,
    Granularity granularity,
    String currency,
    long totalRevenueMinor,
    List<Point> series,
    List<PaymentMethodRevenue> byPaymentMethod,
    List<ServiceRevenue> byService) {

  /**
   * One bucket of the series.
   *
   * @param periodStart first day of the bucket
   * @param revenueMinor revenue of completed bookings starting in the bucket
   * @param bookings number of completed bookings in the bucket
   */
  @Schema(name = "RevenueReportResponsePoint")
  public record Point(LocalDate periodStart, long revenueMinor, long bookings) {}

  /**
   * Revenue of one payment method.
   *
   * @param paymentMethod STRIPE, PAYPAL, CASH or UNKNOWN
   * @param revenueMinor revenue
   * @param bookings number of completed bookings
   */
  public record PaymentMethodRevenue(String paymentMethod, long revenueMinor, long bookings) {}

  /**
   * Revenue of one service (by the name snapshotted on the booking item).
   *
   * @param serviceId service id, {@code null} when the service was deleted
   * @param serviceName service name at booking time
   * @param revenueMinor sum of item prices
   * @param bookings number of completed bookings containing the service
   */
  public record ServiceRevenue(
      Long serviceId, String serviceName, long revenueMinor, long bookings) {}
}
