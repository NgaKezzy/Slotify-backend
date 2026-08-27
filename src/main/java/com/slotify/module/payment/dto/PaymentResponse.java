package com.slotify.module.payment.dto;

import com.slotify.module.payment.entity.PaymentProvider;
import com.slotify.module.payment.entity.PaymentState;
import com.slotify.module.payment.entity.PaymentType;
import java.time.Instant;

/** A payment attempt as shown to owners and customers. */
public record PaymentResponse(
    Long id,
    Long bookingId,
    String bookingCode,
    PaymentProvider provider,
    String providerRef,
    long amountMinor,
    long refundedMinor,
    String currency,
    PaymentType type,
    PaymentState status,
    Instant paidAt,
    Instant createdAt) {}
