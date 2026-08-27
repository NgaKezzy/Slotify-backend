package com.slotify.module.promotion.entity;

/** How a coupon reduces the price. */
public enum CouponType {
  /** {@code value} is a percentage 0-100 of the subtotal. */
  PERCENT,
  /** {@code value} is a fixed amount in minor units of the salon currency. */
  FIXED
}
