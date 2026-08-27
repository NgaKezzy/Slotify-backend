package com.slotify.module.review.controller;

import com.slotify.common.api.ApiResponse;
import com.slotify.module.review.dto.CreateReviewRequest;
import com.slotify.module.review.dto.ReviewResponse;
import com.slotify.module.review.service.ReviewService;
import com.slotify.security.CurrentUser;
import com.slotify.security.UserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Review endpoints of the customer app (one review per completed booking). */
@Tag(name = "Reviews", description = "Ratings of completed bookings")
@RestController
@RequestMapping("/api/v1/bookings/{bookingId}/review")
@RequiredArgsConstructor
public class CustomerReviewController {

  private final ReviewService reviewService;

  @Operation(summary = "Review one of my completed bookings")
  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public ApiResponse<ReviewResponse> create(
      @CurrentUser UserPrincipal principal,
      @PathVariable Long bookingId,
      @Valid @RequestBody CreateReviewRequest request) {
    return ApiResponse.ok(reviewService.create(principal, bookingId, request));
  }

  @Operation(summary = "Get my review of a booking")
  @GetMapping
  public ApiResponse<ReviewResponse> get(
      @CurrentUser UserPrincipal principal, @PathVariable Long bookingId) {
    return ApiResponse.ok(reviewService.getMine(principal, bookingId));
  }
}
