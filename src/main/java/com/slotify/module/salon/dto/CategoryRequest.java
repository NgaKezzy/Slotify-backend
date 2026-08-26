package com.slotify.module.salon.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Body for creating / updating a global category (SUPER_ADMIN). */
public record CategoryRequest(
    @NotBlank @Size(max = 100) String name, @Size(max = 500) String iconUrl, int sortOrder) {}
