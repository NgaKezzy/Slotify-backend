package com.slotify.module.booking.entity;

/** How the customer chose to pay. */
public enum PaymentMethod {
  STRIPE,
  PAYPAL,
  /** Pay at the salon. */
  CASH
}
