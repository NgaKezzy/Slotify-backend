package com.slotify.module.promotion;

import static org.assertj.core.api.Assertions.assertThat;

import com.slotify.module.promotion.entity.Coupon;
import com.slotify.module.promotion.entity.CouponType;
import java.time.Instant;
import org.junit.jupiter.api.Test;

/** Discount and validity rules of {@link Coupon}. */
class CouponTest {

  private static final Instant NOW = Instant.parse("2026-06-01T10:00:00Z");

  @Test
  void percentDiscountIsRoundedAndCapped() {
    Coupon coupon = Coupon.create(null, "SUMMER10", CouponType.PERCENT, 10);
    assertThat(coupon.discountFor(4550)).isEqualTo(455);

    coupon.setMaxDiscountMinor(300L);
    assertThat(coupon.discountFor(4550)).isEqualTo(300);
  }

  @Test
  void fixedDiscountNeverExceedsSubtotal() {
    Coupon coupon = Coupon.create(null, "FIVE", CouponType.FIXED, 500);
    assertThat(coupon.discountFor(300)).isEqualTo(300);
    assertThat(coupon.discountFor(2000)).isEqualTo(500);
  }

  @Test
  void validityWindowAndUsageLimitAreEnforced() {
    Coupon coupon = Coupon.create(null, "LIMITED", CouponType.FIXED, 100);
    assertThat(coupon.isUsable(NOW)).isTrue();

    coupon.setStartsAt(NOW.plusSeconds(60));
    assertThat(coupon.isUsable(NOW)).isFalse();
    coupon.setStartsAt(null);

    coupon.setEndsAt(NOW.minusSeconds(1));
    assertThat(coupon.isUsable(NOW)).isFalse();
    coupon.setEndsAt(null);

    coupon.setUsageLimit(2);
    coupon.setUsedCount(2);
    assertThat(coupon.isUsable(NOW)).isFalse();

    coupon.setUsedCount(1);
    coupon.setActive(false);
    assertThat(coupon.isUsable(NOW)).isFalse();
  }

  @Test
  void codesAreNormalisedUpperCase() {
    assertThat(Coupon.normalize("  welcome-20 ")).isEqualTo("WELCOME-20");
  }
}
