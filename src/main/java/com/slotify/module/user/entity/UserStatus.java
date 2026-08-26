package com.slotify.module.user.entity;

/** Lifecycle status of a user account. */
public enum UserStatus {
  ACTIVE,
  /** Blocked by a super admin; login is refused. */
  SUSPENDED,
  /** Anonymised after a GDPR deletion request. */
  DELETED
}
