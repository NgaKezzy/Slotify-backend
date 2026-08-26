package com.slotify.module.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Body of {@code POST /auth/register}. New accounts always get the CUSTOMER role.
 *
 * @param fullName display name
 * @param email unique email address
 * @param password 8-72 characters
 * @param phone optional phone number
 */
public record RegisterRequest(
    @NotBlank @Size(max = 150) String fullName,
    @NotBlank @Email @Size(max = 255) String email,
    @NotBlank @Size(min = 8, max = 72) String password,
    @Pattern(regexp = "^\\+?[0-9 ()-]{6,32}$", message = "must be a valid phone number")
        String phone) {}
