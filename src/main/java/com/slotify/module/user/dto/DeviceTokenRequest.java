package com.slotify.module.user.dto;

import com.slotify.module.user.entity.DeviceToken;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Body of {@code POST /me/device-token}.
 *
 * @param token FCM registration token
 * @param platform device platform
 * @param appType which app registered the token
 */
public record DeviceTokenRequest(
    @NotBlank @Size(max = 512) String token,
    @NotNull DeviceToken.Platform platform,
    @NotNull DeviceToken.AppType appType) {}
