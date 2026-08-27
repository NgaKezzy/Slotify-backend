package com.slotify.module.review.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

/**
 * Body of {@code POST /bookings/{bookingId}/review}.
 *
 * @param rating 1 (worst) to 5 (best)
 * @param comment optional free text
 */
public record CreateReviewRequest(@Min(1) @Max(5) int rating, @Size(max = 1000) String comment) {}
