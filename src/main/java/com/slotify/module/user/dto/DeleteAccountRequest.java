package com.slotify.module.user.dto;

/**
 * Body of {@code DELETE /me} (GDPR account deletion).
 *
 * @param password current password; required for local accounts, ignored for social logins
 */
public record DeleteAccountRequest(String password) {}
