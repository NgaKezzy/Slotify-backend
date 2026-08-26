package com.slotify.module.staff.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Body of {@code PUT /admin/salons/{salonId}/staff/{staffId}}. The linked account cannot be changed
 * here.
 *
 * @param displayName name shown to customers
 * @param title optional job title
 * @param bio optional short biography
 * @param avatarUrl optional profile image URL
 * @param active whether the staff member is bookable
 */
public record UpdateStaffRequest(
    @NotBlank @Size(max = 150) String displayName,
    @Size(max = 100) String title,
    @Size(max = 2000) String bio,
    @Size(max = 500) String avatarUrl,
    @NotNull Boolean active) {}
