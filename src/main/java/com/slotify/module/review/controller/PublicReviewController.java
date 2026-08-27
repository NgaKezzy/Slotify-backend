package com.slotify.module.review.controller;

import com.slotify.common.api.ApiResponse;
import com.slotify.common.api.PageResponse;
import com.slotify.module.review.dto.ReviewResponse;
import com.slotify.module.review.dto.ReviewSummaryResponse;
import com.slotify.module.review.service.ReviewService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Public reviews shown on the salon page (no authentication required). */
@Tag(name = "Reviews", description = "Ratings of completed bookings")
@SecurityRequirements
@RestController
@RequestMapping("/api/v1/salons/{idOrSlug}/reviews")
@RequiredArgsConstructor
public class PublicReviewController {

  private final ReviewService reviewService;

  @Operation(summary = "Visible reviews of a salon, newest first")
  @GetMapping
  public ApiResponse<PageResponse<ReviewResponse>> list(
      @PathVariable String idOrSlug,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size) {
    return ApiResponse.ok(reviewService.listPublic(idOrSlug, page, size));
  }

  @Operation(summary = "Average rating, count and 1..5 distribution")
  @GetMapping("/summary")
  public ApiResponse<ReviewSummaryResponse> summary(@PathVariable String idOrSlug) {
    return ApiResponse.ok(reviewService.summary(idOrSlug));
  }
}
