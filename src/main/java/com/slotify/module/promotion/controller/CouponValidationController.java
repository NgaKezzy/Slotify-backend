package com.slotify.module.promotion.controller;

import com.slotify.common.api.ApiResponse;
import com.slotify.common.exception.AppException;
import com.slotify.common.exception.ErrorCode;
import com.slotify.module.booking.service.AvailabilityService;
import com.slotify.module.promotion.dto.CouponValidationRequest;
import com.slotify.module.promotion.dto.CouponValidationResponse;
import com.slotify.module.promotion.service.CouponService;
import com.slotify.module.salon.entity.Salon;
import com.slotify.module.salon.repository.SalonRepository;
import com.slotify.module.service.entity.SalonService;
import com.slotify.security.CurrentUser;
import com.slotify.security.UserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Coupon pre-check used by the booking confirmation screen. */
@Tag(name = "Bookings", description = "Availability and bookings")
@RestController
@RequestMapping("/api/v1/coupons")
@RequiredArgsConstructor
public class CouponValidationController {

  private final CouponService couponService;
  private final AvailabilityService availabilityService;
  private final SalonRepository salonRepository;

  @Operation(summary = "Check a coupon code and preview the discount")
  @PostMapping("/validate")
  public ApiResponse<CouponValidationResponse> validate(
      @CurrentUser UserPrincipal principal, @Valid @RequestBody CouponValidationRequest request) {
    Salon salon =
        salonRepository
            .findById(request.salonId())
            .filter(Salon::isActive)
            .orElseThrow(() -> new AppException(ErrorCode.SALON_NOT_FOUND));
    long subtotal =
        availabilityService.requireServices(salon, request.serviceIds()).stream()
            .mapToLong(SalonService::getPriceMinor)
            .sum();
    CouponService.Applied applied =
        couponService.resolve(salon, request.code(), principal.id(), subtotal);
    return ApiResponse.ok(
        new CouponValidationResponse(
            applied.coupon().getCode(),
            subtotal,
            applied.discountMinor(),
            subtotal - applied.discountMinor(),
            salon.getCurrency()));
  }
}
