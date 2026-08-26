package com.slotify.module.auth.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Body of {@code POST /auth/google}.
 *
 * @param idToken Google ID token obtained on the device with the {@code google_sign_in} package
 */
public record GoogleLoginRequest(@NotBlank String idToken) {}
