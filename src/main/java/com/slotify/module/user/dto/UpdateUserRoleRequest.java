package com.slotify.module.user.dto;

import com.slotify.module.user.entity.Role;
import jakarta.validation.constraints.NotNull;

/**
 * Body of {@code PUT /admin/platform/users/{id}/role}.
 *
 * @param role new platform role
 */
public record UpdateUserRoleRequest(@NotNull Role role) {}
