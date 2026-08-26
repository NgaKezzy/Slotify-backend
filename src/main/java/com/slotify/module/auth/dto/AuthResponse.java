package com.slotify.module.auth.dto;

import com.slotify.module.user.dto.UserResponse;

/**
 * Payload of every successful authentication call (login, register, google, refresh).
 *
 * @param accessToken short-lived JWT to send as {@code Authorization: Bearer}
 * @param refreshToken opaque token used to obtain a new pair; single use
 * @param expiresIn access token lifetime in seconds
 * @param user the authenticated user
 */
public record AuthResponse(
    String accessToken, String refreshToken, long expiresIn, UserResponse user) {}
