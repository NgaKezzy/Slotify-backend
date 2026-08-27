package com.slotify.module.review.controller;

import com.slotify.common.api.ApiResponse;
import com.slotify.common.api.PageResponse;
import com.slotify.module.review.dto.ReplyReviewRequest;
import com.slotify.module.review.dto.ReviewResponse;
import com.slotify.module.review.dto.ReviewVisibilityRequest;
import com.slotify.module.review.service.ReviewService;
import com.slotify.security.CurrentUser;
import com.slotify.security.UserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Review moderation in the admin panel: list, reply, hide / show. */
@Tag(name = "Reviews (owner)", description = "Reply to and moderate reviews of a salon")
@RestController
@RequestMapping("/api/v1/admin/salons/{salonId}/reviews")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('SALON_OWNER','SUPER_ADMIN')")
public class OwnerReviewController {

  private final ReviewService reviewService;

  @Operation(summary = "List reviews (optionally only visible / hidden ones)")
  @GetMapping
  public ApiResponse<PageResponse<ReviewResponse>> list(
      @CurrentUser UserPrincipal principal,
      @PathVariable Long salonId,
      @RequestParam(required = false) Boolean visible,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size) {
    return ApiResponse.ok(reviewService.listForSalon(principal, salonId, visible, page, size));
  }

  @Operation(summary = "Reply to a review")
  @PostMapping("/{reviewId}/reply")
  public ApiResponse<ReviewResponse> reply(
      @CurrentUser UserPrincipal principal,
      @PathVariable Long salonId,
      @PathVariable Long reviewId,
      @Valid @RequestBody ReplyReviewRequest request) {
    return ApiResponse.ok(reviewService.reply(principal, salonId, reviewId, request.reply()));
  }

  @Operation(summary = "Show or hide a review")
  @PutMapping("/{reviewId}/visibility")
  public ApiResponse<ReviewResponse> visibility(
      @CurrentUser UserPrincipal principal,
      @PathVariable Long salonId,
      @PathVariable Long reviewId,
      @Valid @RequestBody ReviewVisibilityRequest request) {
    return ApiResponse.ok(
        reviewService.setVisibility(principal, salonId, reviewId, request.visible()));
  }
}
