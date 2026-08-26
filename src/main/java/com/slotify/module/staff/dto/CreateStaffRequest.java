package com.slotify.module.staff.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Body of {@code POST /admin/salons/{salonId}/staff}.
 *
 * @param displayName name shown to customers
 * @param title optional job title, e.g. "Senior Stylist"
 * @param bio optional short biography
 * @param avatarUrl optional profile image URL
 * @param inviteEmail optional email of the person; when given a {@code STAFF} login account is
 *     created (or linked) and an invitation email is sent
 */
public record CreateStaffRequest(
    @NotBlank @Size(max = 150) String displayName,
    @Size(max = 100) String title,
    @Size(max = 2000) String bio,
    @Size(max = 500) String avatarUrl,
    @Email @Size(max = 255) String inviteEmail) {}
