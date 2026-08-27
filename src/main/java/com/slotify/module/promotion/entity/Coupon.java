package com.slotify.module.promotion.entity;

import com.slotify.common.entity.BaseEntity;
import com.slotify.module.salon.entity.Salon;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Promotion code of a salon (table {@code coupons}). Codes are unique per salon. */
@Entity
@Table(name = "coupons")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Coupon extends BaseEntity {

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "salon_id", nullable = false)
  private Salon salon;

  @Column(nullable = false, length = 50)
  private String code;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private CouponType type;

  /** Percent (0-100) or fixed amount in minor units, depending on {@link #type}. */
  @Column(nullable = false)
  private long value;

  @Column(name = "min_order_minor", nullable = false)
  private long minOrderMinor;

  @Column(name = "max_discount_minor")
  private Long maxDiscountMinor;

  @Column(name = "usage_limit")
  private Integer usageLimit;

  @Column(name = "used_count", nullable = false)
  private int usedCount;

  @Column(name = "per_user_limit")
  private Integer perUserLimit;

  @Column(name = "starts_at")
  private Instant startsAt;

  @Column(name = "ends_at")
  private Instant endsAt;

  @Column(name = "is_active", nullable = false)
  private boolean active = true;

  public static Coupon create(Salon salon, String code, CouponType type, long value) {
    Coupon coupon = new Coupon();
    coupon.salon = salon;
    coupon.code = normalize(code);
    coupon.type = type;
    coupon.value = value;
    return coupon;
  }

  /** Codes are stored upper-case without surrounding whitespace. */
  public static String normalize(String code) {
    return code.trim().toUpperCase();
  }

  /** Whether the coupon can be used at {@code now} (ignoring per-user limits). */
  public boolean isUsable(Instant now) {
    boolean started = startsAt == null || !now.isBefore(startsAt);
    boolean notEnded = endsAt == null || now.isBefore(endsAt);
    boolean hasUses = usageLimit == null || usedCount < usageLimit;
    return active && started && notEnded && hasUses;
  }

  /**
   * Discount in minor units for a subtotal, capped by {@code maxDiscountMinor} and the subtotal.
   */
  public long discountFor(long subtotalMinor) {
    long discount =
        type == CouponType.PERCENT ? Math.round(subtotalMinor * (value / 100.0)) : value;
    if (maxDiscountMinor != null) {
      discount = Math.min(discount, maxDiscountMinor);
    }
    return Math.max(0, Math.min(discount, subtotalMinor));
  }
}
