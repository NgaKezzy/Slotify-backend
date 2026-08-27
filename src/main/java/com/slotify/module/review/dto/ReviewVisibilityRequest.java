package com.slotify.module.review.dto;

import jakarta.validation.constraints.NotNull;

/**
 * Body of the owner visibility endpoint.
 *
 * @param visible {@code false} hides the review from the public list and rating aggregates
 */
public record ReviewVisibilityRequest(@NotNull Boolean visible) {}
