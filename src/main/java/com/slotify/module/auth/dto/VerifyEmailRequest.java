package com.slotify.module.auth.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Body of {@code POST /auth/verify-email}.
 *
 * @param token token from the verification email
 */
public record VerifyEmailRequest(@NotBlank String token) {}
