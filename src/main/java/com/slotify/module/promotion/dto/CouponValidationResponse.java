package com.slotify.module.promotion.dto;

/**
 * Result of a coupon pre-check.
 *
 * @param code normalised coupon code
 * @param subtotalMinor price before discount
 * @param discountMinor discount that would be applied
 * @param totalMinor price after discount
 * @param currency salon currency
 */
public record CouponValidationResponse(
    String code, long subtotalMinor, long discountMinor, long totalMinor, String currency) {}
