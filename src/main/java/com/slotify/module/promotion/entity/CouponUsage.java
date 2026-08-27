package com.slotify.module.promotion.entity;

import com.slotify.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** Records that a coupon was applied to a booking (enforces per-user and total limits). */
@Entity
@Table(name = "coupon_usages")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CouponUsage extends BaseEntity {

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "coupon_id", nullable = false)
  private Coupon coupon;

  @Column(name = "user_id", nullable = false)
  private Long userId;

  @Column(name = "booking_id", nullable = false)
  private Long bookingId;

  public static CouponUsage of(Coupon coupon, Long userId, Long bookingId) {
    CouponUsage usage = new CouponUsage();
    usage.coupon = coupon;
    usage.userId = userId;
    usage.bookingId = bookingId;
    return usage;
  }
}
