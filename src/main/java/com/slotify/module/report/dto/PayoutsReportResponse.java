package com.slotify.module.report.dto;

import java.math.BigDecimal;
import java.util.List;

/**
 * What the platform owes each active salon for a period (see plan §3.6: online payments land on the
 * platform's Stripe/PayPal account and are paid out manually).
 *
 * @param period the requested period (UTC days)
 * @param rows one row per ACTIVE salon
 */
public record PayoutsReportResponse(ReportPeriod.PeriodDto period, List<Row> rows) {

  /**
   * Payout figures of one salon.
   *
   * @param salonId salon id
   * @param salonName salon name
   * @param currency ISO-4217 currency of every amount
   * @param commissionPercent platform commission applied
   * @param onlineRevenueMinor succeeded Stripe/PayPal payments minus succeeded refunds
   * @param commissionMinor platform share, rounded half-up
   * @param payoutMinor amount to transfer to the salon (online revenue minus commission)
   * @param cashRevenueMinor revenue of COMPLETED cash bookings, informational only
   */
  public record Row(
      Long salonId,
      String salonName,
      String currency,
      BigDecimal commissionPercent,
      long onlineRevenueMinor,
      long commissionMinor,
      long payoutMinor,
      long cashRevenueMinor) {}
}
