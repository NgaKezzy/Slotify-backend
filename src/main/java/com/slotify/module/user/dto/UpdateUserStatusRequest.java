package com.slotify.module.user.dto;

import com.slotify.module.user.entity.UserStatus;
import jakarta.validation.constraints.NotNull;

/**
 * Body of {@code PUT /admin/platform/users/{id}/status}.
 *
 * @param status ACTIVE or SUSPENDED (DELETED is reserved for the GDPR flow)
 */
public record UpdateUserStatusRequest(@NotNull UserStatus status) {}
