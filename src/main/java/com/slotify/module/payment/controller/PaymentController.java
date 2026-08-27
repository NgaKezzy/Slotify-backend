package com.slotify.module.payment.controller;

import com.slotify.common.api.ApiResponse;
import com.slotify.module.payment.dto.CaptureRequest;
import com.slotify.module.payment.dto.CreatePaymentRequest;
import com.slotify.module.payment.dto.PaymentConfigResponse;
import com.slotify.module.payment.dto.PaymentInitResponse;
import com.slotify.module.payment.dto.PaymentResponse;
import com.slotify.module.payment.entity.PaymentProvider;
import com.slotify.module.payment.service.PaymentService;
import com.slotify.security.CurrentUser;
import com.slotify.security.UserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Customer-side payment endpoints. */
@Tag(name = "Payments", description = "Online payments for bookings")
@RestController
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
public class PaymentController {

  private final PaymentService paymentService;

  @Operation(summary = "Public payment configuration (keys, enabled providers)")
  @SecurityRequirements
  @GetMapping("/config")
  public ApiResponse<PaymentConfigResponse> config() {
    return ApiResponse.ok(paymentService.config());
  }

  @Operation(summary = "Create a Stripe PaymentIntent for a booking (returns clientSecret)")
  @PostMapping("/stripe/intent")
  public ApiResponse<PaymentInitResponse> stripeIntent(
      @CurrentUser UserPrincipal principal, @Valid @RequestBody CreatePaymentRequest request) {
    return ApiResponse.ok(
        paymentService.initiate(principal, request.bookingId(), PaymentProvider.STRIPE));
  }

  @Operation(summary = "Create a PayPal order for a booking (returns approveUrl)")
  @PostMapping("/paypal/order")
  public ApiResponse<PaymentInitResponse> paypalOrder(
      @CurrentUser UserPrincipal principal, @Valid @RequestBody CreatePaymentRequest request) {
    return ApiResponse.ok(
        paymentService.initiate(principal, request.bookingId(), PaymentProvider.PAYPAL));
  }

  @Operation(summary = "Capture an approved PayPal order")
  @PostMapping("/paypal/capture")
  public ApiResponse<PaymentResponse> paypalCapture(
      @CurrentUser UserPrincipal principal, @Valid @RequestBody CaptureRequest request) {
    return ApiResponse.ok(paymentService.capturePayPal(principal, request.orderId()));
  }
}
