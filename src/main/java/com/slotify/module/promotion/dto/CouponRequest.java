package com.slotify.module.promotion.dto;

import com.slotify.module.promotion.entity.CouponType;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import java.time.Instant;

/**
 * Body for creating / updating a coupon.
 *
 * @param code 3-50 characters, letters/digits/dash; stored upper-case
 * @param type percent or fixed
 * @param value percent 1-100 or fixed amount in minor units
 * @param minOrderMinor minimum subtotal to apply
 * @param maxDiscountMinor cap for percent coupons (optional)
 * @param usageLimit total number of uses (optional)
 * @param perUserLimit uses per customer (optional)
 * @param startsAt validity start (optional)
 * @param endsAt validity end (optional)
 * @param active whether the coupon can be used
 */
public record CouponRequest(
    @NotBlank @Pattern(regexp = "^[A-Za-z0-9-]{3,50}$", message = "{validation.coupon_code}") String code,
    @NotNull CouponType type,
    @Min(1) long value,
    @Min(0) long minOrderMinor,
    @Min(1) Long maxDiscountMinor,
    @Min(1) Integer usageLimit,
    @Min(1) Integer perUserLimit,
    Instant startsAt,
    Instant endsAt,
    boolean active) {}
