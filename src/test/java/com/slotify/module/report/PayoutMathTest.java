package com.slotify.module.report;

import static org.assertj.core.api.Assertions.assertThat;

import com.slotify.module.report.service.PayoutMath;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

/** Commission rounding in minor units. */
class PayoutMathTest {

  @Test
  void commissionIsRoundedHalfUpToTheMinorUnit() {
    BigDecimal twelveAndAHalf = new BigDecimal("12.50");
    assertThat(PayoutMath.commission(1000, twelveAndAHalf)).isEqualTo(125);
    // 1001 * 12.5 % = 125.125 -> 125
    assertThat(PayoutMath.commission(1001, twelveAndAHalf)).isEqualTo(125);
    // 1004 * 12.5 % = 125.5 -> 126
    assertThat(PayoutMath.commission(1004, twelveAndAHalf)).isEqualTo(126);
  }

  @Test
  void zeroAndFullCommission() {
    assertThat(PayoutMath.commission(9999, BigDecimal.ZERO)).isZero();
    assertThat(PayoutMath.commission(9999, new BigDecimal("100.00"))).isEqualTo(9999);
    assertThat(PayoutMath.commission(0, new BigDecimal("15.00"))).isZero();
  }
}
