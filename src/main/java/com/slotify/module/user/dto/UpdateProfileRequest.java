package com.slotify.module.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Body of {@code PUT /me}. Only profile fields are editable here; email and role are not.
 *
 * @param fullName display name
 * @param phone optional phone in international format
 * @param avatarUrl optional avatar image URL
 * @param locale preferred language code, e.g. {@code en}, {@code de}
 */
public record UpdateProfileRequest(
    @NotBlank @Size(max = 150) String fullName,
    @Pattern(regexp = "^\\+?[0-9 ()-]{6,32}$", message = "must be a valid phone number")
        String phone,
    @Size(max = 500) String avatarUrl,
    @Pattern(regexp = "^[a-z]{2}(-[A-Z]{2})?$", message = "must be a language code like en or de")
        String locale) {}
