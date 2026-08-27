package com.slotify.module.payment.entity;

/** Payment processor that handled (or will handle) a payment. */
public enum PaymentProvider {
  STRIPE,
  PAYPAL,
  /** Paid at the salon; recorded by the owner. */
  CASH
}
