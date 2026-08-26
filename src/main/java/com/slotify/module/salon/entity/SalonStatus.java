package com.slotify.module.salon.entity;

/** Approval state of a salon on the platform. */
public enum SalonStatus {
  /** Created by an owner, waiting for SUPER_ADMIN approval; hidden from customers. */
  PENDING,
  /** Visible and bookable. */
  ACTIVE,
  /** Hidden by a SUPER_ADMIN (e.g. policy violation). */
  SUSPENDED
}
