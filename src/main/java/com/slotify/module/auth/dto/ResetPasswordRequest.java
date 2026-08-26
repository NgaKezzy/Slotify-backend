package com.slotify.module.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Body of {@code POST /auth/reset-password}.
 *
 * @param token token from the reset email
 * @param newPassword new password (8-72 characters)
 */
public record ResetPasswordRequest(
    @NotBlank String token, @NotBlank @Size(min = 8, max = 72) String newPassword) {}
