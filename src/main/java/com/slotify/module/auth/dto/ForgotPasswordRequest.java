package com.slotify.module.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/**
 * Body of {@code POST /auth/forgot-password}. Always answers 200 to avoid leaking whether the email
 * exists.
 *
 * @param email account email
 */
public record ForgotPasswordRequest(@NotBlank @Email String email) {}
