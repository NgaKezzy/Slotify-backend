package com.slotify.module.review.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Body of the owner reply endpoint.
 *
 * @param reply public answer shown under the review
 */
public record ReplyReviewRequest(@NotBlank @Size(max = 1000) String reply) {}
