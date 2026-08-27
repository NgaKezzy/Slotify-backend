package com.slotify.module.payment.dto;

import jakarta.validation.constraints.NotNull;

/**
 * Body of {@code POST /payments/stripe/intent} and {@code POST /payments/paypal/order}.
 *
 * @param bookingId booking to pay for (must belong to the caller and be unpaid)
 */
public record CreatePaymentRequest(@NotNull Long bookingId) {}
