package com.slotify.module.user.entity;

/** Platform-wide role of a user account. Stored in {@code users.role}. */
public enum Role {
  /** Owner of the platform: approves salons, manages global catalog. */
  SUPER_ADMIN,
  /** Owns one or more salons and manages them in the admin panel. */
  SALON_OWNER,
  /** Employee of a salon; signs in to the Staff app. */
  STAFF,
  /** End customer booking appointments; default role on registration. */
  CUSTOMER
}
