package com.slotify.module.promotion.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;

/**
 * Body of {@code POST /coupons/validate}: pre-check a code before booking.
 *
 * @param salonId salon
 * @param code coupon code
 * @param serviceIds services the customer is about to book (for the subtotal)
 */
public record CouponValidationRequest(
    @NotNull Long salonId, @NotBlank String code, @NotEmpty List<Long> serviceIds) {}
