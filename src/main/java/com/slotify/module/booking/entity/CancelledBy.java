package com.slotify.module.booking.entity;

/** Who cancelled a booking. */
public enum CancelledBy {
  CUSTOMER,
  SALON,
  /** Automatic cancellation, e.g. unpaid deposit timeout. */
  SYSTEM
}
