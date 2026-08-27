package com.slotify.module.payment.service;

import com.slotify.common.exception.AppException;
import com.slotify.common.exception.ErrorCode;
import com.slotify.config.AppProperties;
import com.slotify.module.payment.entity.Payment;
import com.slotify.module.payment.entity.PaymentProvider;
import com.stripe.StripeClient;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.exception.StripeException;
import com.stripe.model.Event;
import com.stripe.model.PaymentIntent;
import com.stripe.model.Refund;
import com.stripe.net.Webhook;
import com.stripe.param.PaymentIntentCreateParams;
import com.stripe.param.RefundCreateParams;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Stripe implementation: PaymentIntents (used by the Flutter PaymentSheet, which supports cards,
 * Apple/Google Pay, SEPA, iDEAL, Bancontact via automatic payment methods), refunds and webhook
 * signature verification.
 */
@Slf4j
@Component
public class StripeGateway implements PaymentGateway {

  private final AppProperties.Stripe config;
  private final StripeClient client;

  public StripeGateway(AppProperties properties) {
    this.config = properties.stripe();
    this.client = config.enabled() ? new StripeClient(config.secretKey()) : null;
    if (!config.enabled()) {
      log.info("Stripe not configured – card payments disabled");
    }
  }

  @Override
  public PaymentProvider provider() {
    return PaymentProvider.STRIPE;
  }

  @Override
  public boolean enabled() {
    return config.enabled();
  }

  @Override
  public CreatedPayment create(Payment payment, String customerEmail, String description) {
    requireEnabled();
    PaymentIntentCreateParams params =
        PaymentIntentCreateParams.builder()
            .setAmount(payment.getAmountMinor())
            .setCurrency(payment.getCurrency().toLowerCase())
            .setDescription(description)
            .setReceiptEmail(customerEmail)
            .putMetadata("bookingId", String.valueOf(payment.getBooking().getId()))
            .putMetadata("paymentId", String.valueOf(payment.getId()))
            .setAutomaticPaymentMethods(
                PaymentIntentCreateParams.AutomaticPaymentMethods.builder()
                    .setEnabled(true)
                    .build())
            .build();
    try {
      PaymentIntent intent = client.paymentIntents().create(params);
      return new CreatedPayment(
          intent.getId(),
          Map.of(
              "clientSecret",
              intent.getClientSecret(),
              "publishableKey",
              config.publishableKey() == null ? "" : config.publishableKey()));
    } catch (StripeException ex) {
      log.error("Stripe PaymentIntent creation failed: {}", ex.getMessage());
      throw new AppException(ErrorCode.PAYMENT_FAILED);
    }
  }

  @Override
  public String refund(Payment payment, long amountMinor, String reason) {
    requireEnabled();
    try {
      Refund refund =
          client
              .refunds()
              .create(
                  RefundCreateParams.builder()
                      .setPaymentIntent(payment.getProviderRef())
                      .setAmount(amountMinor)
                      .setReason(RefundCreateParams.Reason.REQUESTED_BY_CUSTOMER)
                      .putMetadata("reason", reason == null ? "" : reason)
                      .build());
      return refund.getId();
    } catch (StripeException ex) {
      log.error("Stripe refund failed for {}: {}", payment.getProviderRef(), ex.getMessage());
      throw new AppException(ErrorCode.PAYMENT_FAILED);
    }
  }

  /** Verifies the {@code Stripe-Signature} header and parses the event. */
  public Event parseWebhook(String payload, String signature) {
    try {
      return Webhook.constructEvent(payload, signature, config.webhookSecret());
    } catch (SignatureVerificationException ex) {
      throw new AppException(ErrorCode.TOKEN_INVALID);
    }
  }

  private void requireEnabled() {
    if (!enabled()) {
      throw new AppException(ErrorCode.PAYMENT_METHOD_NOT_ACCEPTED);
    }
  }
}
