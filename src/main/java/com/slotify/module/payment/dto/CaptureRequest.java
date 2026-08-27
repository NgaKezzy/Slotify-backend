package com.slotify.module.payment.dto;

import jakarta.validation.constraints.NotBlank;

/** Body of {@code POST /payments/paypal/capture}. */
public record CaptureRequest(@NotBlank String orderId) {}
