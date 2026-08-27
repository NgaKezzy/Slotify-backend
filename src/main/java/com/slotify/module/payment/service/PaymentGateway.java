package com.slotify.module.payment.service;

import com.slotify.module.payment.entity.Payment;
import com.slotify.module.payment.entity.PaymentProvider;
import java.util.Map;

/**
 * Abstraction over an online payment processor. One implementation per {@link PaymentProvider}
 * (Stripe, PayPal); {@code PaymentService} orchestrates bookings and never talks to a provider
 * directly.
 */
public interface PaymentGateway {

  PaymentProvider provider();

  /** Whether credentials are configured. */
  boolean enabled();

  /**
   * Creates the provider-side payment object for a pending {@link Payment}.
   *
   * @return client-facing data (e.g. Stripe {@code clientSecret}, PayPal {@code orderId} + approval
   *     URL) and the provider reference to store
   */
  CreatedPayment create(Payment payment, String customerEmail, String description);

  /** Issues a (partial) refund; returns the provider refund id. */
  String refund(Payment payment, long amountMinor, String reason);

  /**
   * Result of {@link #create}.
   *
   * @param providerRef id used to correlate webhooks (PaymentIntent id / order id)
   * @param clientData values the client needs to complete the payment
   */
  record CreatedPayment(String providerRef, Map<String, String> clientData) {}
}
