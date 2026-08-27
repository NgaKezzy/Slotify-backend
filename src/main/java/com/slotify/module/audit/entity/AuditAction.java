package com.slotify.module.audit.entity;

/**
 * Kinds of auditable events stored in {@code audit_logs.action}.
 *
 * <p>Add a constant here (never a free-form string) when a new flow becomes auditable, so that the
 * admin panel can offer the action as a filter.
 */
public enum AuditAction {
  BOOKING_CREATED,
  BOOKING_STATUS_CHANGED,
  BOOKING_RESCHEDULED,
  PAYMENT_REFUNDED,
  SALON_UPDATED,
  SALON_SETTINGS_UPDATED,
  SALON_STATUS_CHANGED,
  STAFF_UPDATED,
  USER_STATUS_CHANGED,
  USER_DELETED
}
