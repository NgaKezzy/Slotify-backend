package com.slotify.module.payment.controller;

import com.slotify.common.api.ApiResponse;
import com.slotify.common.api.PageResponse;
import com.slotify.module.payment.dto.PaymentResponse;
import com.slotify.module.payment.dto.RefundRequest;
import com.slotify.module.payment.service.PaymentService;
import com.slotify.security.CurrentUser;
import com.slotify.security.UserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Payments and refunds in the admin panel. */
@Tag(name = "Payments (owner)", description = "Transactions of a salon")
@RestController
@RequestMapping("/api/v1/admin/salons/{salonId}")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('SALON_OWNER','SUPER_ADMIN')")
public class OwnerPaymentController {

  private final PaymentService paymentService;

  @Operation(summary = "List payments, newest first")
  @GetMapping("/payments")
  public ApiResponse<PageResponse<PaymentResponse>> list(
      @CurrentUser UserPrincipal principal,
      @PathVariable Long salonId,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size) {
    return ApiResponse.ok(paymentService.listForSalon(principal, salonId, page, size));
  }

  @Operation(summary = "Refund an online payment (partial or full)")
  @PostMapping("/payments/{paymentId}/refund")
  public ApiResponse<PaymentResponse> refund(
      @CurrentUser UserPrincipal principal,
      @PathVariable Long salonId,
      @PathVariable Long paymentId,
      @Valid @RequestBody(required = false) RefundRequest request) {
    Long amount = request == null ? null : request.amountMinor();
    String reason = request == null ? null : request.reason();
    return ApiResponse.ok(paymentService.refund(principal, salonId, paymentId, amount, reason));
  }

  @Operation(summary = "Record a cash payment received at the salon")
  @PostMapping("/bookings/{bookingId}/mark-paid")
  public ApiResponse<PaymentResponse> markPaid(
      @CurrentUser UserPrincipal principal,
      @PathVariable Long salonId,
      @PathVariable Long bookingId) {
    return ApiResponse.ok(paymentService.markPaidCash(principal, salonId, bookingId));
  }
}
