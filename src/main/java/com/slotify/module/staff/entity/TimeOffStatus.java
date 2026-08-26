package com.slotify.module.staff.entity;

/**
 * Approval state of a {@link StaffTimeOff} entry. Only {@link #APPROVED} entries block
 * availability. Stored in {@code staff_time_off.status}.
 */
public enum TimeOffStatus {
  /** Requested by the staff member from the Staff app, awaiting the owner's decision. */
  PENDING,
  /** Granted (default when the owner creates it directly); blocks bookings. */
  APPROVED,
  /** Declined by the owner; kept for history. */
  REJECTED
}
