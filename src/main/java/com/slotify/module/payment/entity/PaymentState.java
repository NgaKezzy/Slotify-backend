package com.slotify.module.payment.entity;

/** Lifecycle of a single payment attempt (column {@code payments.status}). */
public enum PaymentState {
  PENDING,
  SUCCEEDED,
  FAILED,
  REFUNDED,
  PARTIAL_REFUND
}
