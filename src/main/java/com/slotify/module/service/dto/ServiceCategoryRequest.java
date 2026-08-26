package com.slotify.module.service.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Body for creating / updating a service category of a salon. */
public record ServiceCategoryRequest(@NotBlank @Size(max = 100) String name, int sortOrder) {}
