package com.slotify.module.user.entity;

/** How the account authenticates. */
public enum AuthProvider {
  /** Email + password stored as a BCrypt hash. */
  LOCAL,
  /** Google Sign-In; {@code users.provider_id} holds the Google subject id. */
  GOOGLE
}
