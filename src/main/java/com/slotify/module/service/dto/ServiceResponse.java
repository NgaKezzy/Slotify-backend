package com.slotify.module.service.dto;

/**
 * A bookable service.
 *
 * @param priceMinor price in minor units (cents) of {@code currency}
 */
public record ServiceResponse(
    Long id,
    Long categoryId,
    String categoryName,
    String name,
    String description,
    int durationMin,
    int bufferAfterMin,
    long priceMinor,
    String currency,
    String imageUrl,
    boolean active,
    int sortOrder) {}
