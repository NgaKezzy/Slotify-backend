package com.slotify.module.promotion.dto;

import com.slotify.module.promotion.entity.CouponType;
import java.time.Instant;

/** Coupon as managed by the owner. */
public record CouponResponse(
    Long id,
    String code,
    CouponType type,
    long value,
    long minOrderMinor,
    Long maxDiscountMinor,
    Integer usageLimit,
    int usedCount,
    Integer perUserLimit,
    Instant startsAt,
    Instant endsAt,
    boolean active) {}
