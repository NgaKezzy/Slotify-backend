package com.slotify.module.booking.entity;

/** Aggregate payment state of a booking (derived from its payments). */
public enum PaymentStatus {
  UNPAID,
  PAID,
  PARTIALLY_PAID,
  REFUNDED
}
