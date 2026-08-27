package com.slotify.module.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Body of {@code POST /auth/logout}.
 *
 * @param refreshToken refresh token to revoke
 * @param deviceToken optional FCM token of this device, removed so the account stops receiving
 *     pushes here (sent in the same request because the session is gone right after)
 */
public record LogoutRequest(@NotBlank String refreshToken, @Size(max = 512) String deviceToken) {}
