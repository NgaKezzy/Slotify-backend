package com.slotify.module.auth.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Body of {@code POST /auth/refresh} and {@code POST /auth/logout}.
 *
 * @param refreshToken the opaque refresh token issued at login
 */
public record RefreshTokenRequest(@NotBlank String refreshToken) {}
