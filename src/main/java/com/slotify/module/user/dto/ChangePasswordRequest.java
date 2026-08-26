package com.slotify.module.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Body of {@code PUT /me/password}.
 *
 * @param currentPassword the password currently in use
 * @param newPassword the new password (8-72 characters)
 */
public record ChangePasswordRequest(
    @NotBlank String currentPassword, @NotBlank @Size(min = 8, max = 72) String newPassword) {}
