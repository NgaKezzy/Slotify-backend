package com.slotify.module.payment.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

/**
 * Body of {@code POST /admin/salons/{salonId}/payments/{paymentId}/refund}.
 *
 * @param amountMinor amount to refund; null = everything still refundable
 * @param reason free text shown to the customer
 */
public record RefundRequest(@Min(1) Long amountMinor, @Size(max = 255) String reason) {}
