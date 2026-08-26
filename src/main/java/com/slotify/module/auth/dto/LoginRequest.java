package com.slotify.module.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/**
 * Body of {@code POST /auth/login}.
 *
 * @param email account email
 * @param password account password
 */
public record LoginRequest(@NotBlank @Email String email, @NotBlank String password) {}
