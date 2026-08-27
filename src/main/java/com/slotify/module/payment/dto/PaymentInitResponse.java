package com.slotify.module.payment.dto;

import com.slotify.module.payment.entity.PaymentProvider;
import com.slotify.module.payment.entity.PaymentType;
import java.util.Map;

/**
 * Data the client needs to complete an online payment.
 *
 * @param paymentId Slotify payment id
 * @param provider provider
 * @param type full or deposit
 * @param amountMinor amount charged now
 * @param currency currency
 * @param clientData Stripe: {@code clientSecret}, {@code publishableKey}; PayPal: {@code orderId},
 *     {@code approveUrl}
 */
public record PaymentInitResponse(
    Long paymentId,
    PaymentProvider provider,
    PaymentType type,
    long amountMinor,
    String currency,
    Map<String, String> clientData) {}
