package com.slotify.module.notification.entity;

/**
 * Kinds of notifications. Each has message keys {@code notification.<lowercase>.title} and {@code
 * .body} in {@code i18n/messages*.properties}.
 */
public enum NotificationType {
  BOOKING_CREATED,
  BOOKING_CONFIRMED,
  BOOKING_REJECTED,
  BOOKING_CANCELLED,
  BOOKING_RESCHEDULED,
  BOOKING_REMINDER,
  BOOKING_COMPLETED,
  BOOKING_NO_SHOW,
  PAYMENT_RECEIVED,
  PAYMENT_REFUNDED,
  REVIEW_RECEIVED,
  STAFF_INVITED,
  /** Free-text message sent by the platform admin to an audience (see {@code /broadcast}). */
  ANNOUNCEMENT;

  /** Base message key, e.g. {@code notification.booking_confirmed}. */
  public String messageKey() {
    return "notification." + name().toLowerCase();
  }
}
