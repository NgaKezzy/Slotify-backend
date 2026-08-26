package com.slotify.module.service.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Body for creating / updating a service.
 *
 * @param categoryId optional service category of the same salon
 * @param name display name
 * @param description free text
 * @param durationMin duration in minutes (5-600)
 * @param bufferAfterMin clean-up time after the service (0-120)
 * @param priceMinor price in minor units of the salon currency
 * @param imageUrl optional image
 * @param active whether customers can book it
 * @param sortOrder position on the menu
 */
public record ServiceRequest(
    Long categoryId,
    @NotBlank @Size(max = 150) String name,
    @Size(max = 5000) String description,
    @Min(5) @Max(600) int durationMin,
    @Min(0) @Max(120) int bufferAfterMin,
    @Min(0) long priceMinor,
    @Size(max = 500) String imageUrl,
    boolean active,
    int sortOrder) {}
