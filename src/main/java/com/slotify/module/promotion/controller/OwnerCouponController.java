package com.slotify.module.promotion.controller;

import com.slotify.common.api.ApiResponse;
import com.slotify.module.promotion.dto.CouponRequest;
import com.slotify.module.promotion.dto.CouponResponse;
import com.slotify.module.promotion.service.CouponService;
import com.slotify.security.CurrentUser;
import com.slotify.security.UserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Promotion codes of a salon (owner). */
@Tag(name = "Promotions (owner)", description = "Manage coupons")
@RestController
@RequestMapping("/api/v1/admin/salons/{salonId}/coupons")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('SALON_OWNER','SUPER_ADMIN')")
public class OwnerCouponController {

  private final CouponService couponService;

  @Operation(summary = "List coupons")
  @GetMapping
  public ApiResponse<List<CouponResponse>> list(
      @CurrentUser UserPrincipal principal, @PathVariable Long salonId) {
    return ApiResponse.ok(couponService.list(salonId, principal));
  }

  @Operation(summary = "Create a coupon")
  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public ApiResponse<CouponResponse> create(
      @CurrentUser UserPrincipal principal,
      @PathVariable Long salonId,
      @Valid @RequestBody CouponRequest request) {
    return ApiResponse.ok(couponService.create(salonId, principal, request));
  }

  @Operation(summary = "Update a coupon")
  @PutMapping("/{couponId}")
  public ApiResponse<CouponResponse> update(
      @CurrentUser UserPrincipal principal,
      @PathVariable Long salonId,
      @PathVariable Long couponId,
      @Valid @RequestBody CouponRequest request) {
    return ApiResponse.ok(couponService.update(salonId, couponId, principal, request));
  }

  @Operation(summary = "Delete a coupon")
  @DeleteMapping("/{couponId}")
  public ApiResponse<Void> delete(
      @CurrentUser UserPrincipal principal,
      @PathVariable Long salonId,
      @PathVariable Long couponId) {
    couponService.delete(salonId, couponId, principal);
    return ApiResponse.ok();
  }
}
