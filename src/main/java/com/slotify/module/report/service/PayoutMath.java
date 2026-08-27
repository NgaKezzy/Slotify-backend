package com.slotify.module.report.service;

import java.math.BigDecimal;
import java.math.RoundingMode;

/** Commission arithmetic in minor units; all rounding is half-up to the nearest minor unit. */
public final class PayoutMath {

  private static final BigDecimal ONE_HUNDRED = BigDecimal.valueOf(100);

  private PayoutMath() {}

  /**
   * Platform share of an online amount.
   *
   * @param onlineRevenueMinor net online revenue in minor units
   * @param commissionPercent commission in percent (0-100)
   * @return commission in minor units, rounded half-up
   */
  public static long commission(long onlineRevenueMinor, BigDecimal commissionPercent) {
    return BigDecimal.valueOf(onlineRevenueMinor)
        .multiply(commissionPercent)
        .divide(ONE_HUNDRED, 0, RoundingMode.HALF_UP)
        .longValueExact();
  }
}
