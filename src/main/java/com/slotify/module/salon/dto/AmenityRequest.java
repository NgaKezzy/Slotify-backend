package com.slotify.module.salon.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Body for creating / updating a global amenity (SUPER_ADMIN). */
public record AmenityRequest(
    @NotBlank @Size(max = 100) String name, @Size(max = 100) String icon) {}
