package com.slotify.module.booking.controller;

import com.slotify.common.api.ApiResponse;
import com.slotify.common.api.PageResponse;
import com.slotify.module.booking.dto.BookingActionRequest;
import com.slotify.module.booking.dto.BookingResponse;
import com.slotify.module.booking.dto.CreateBookingRequest;
import com.slotify.module.booking.dto.RescheduleBookingRequest;
import com.slotify.module.booking.service.BookingService;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Booking endpoints of the customer app. */
@Tag(name = "Bookings", description = "Availability and bookings")
@RestController
@RequestMapping("/api/v1/bookings")
@RequiredArgsConstructor
public class CustomerBookingController {

  private final BookingService bookingService;

  @Operation(summary = "Book an available slot")
  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public ApiResponse<BookingResponse> create(
      @CurrentUser UserPrincipal principal, @Valid @RequestBody CreateBookingRequest request) {
    return ApiResponse.ok(bookingService.create(principal, request));
  }

  @Operation(summary = "List my bookings (status=upcoming|past)")
  @GetMapping
  public ApiResponse<PageResponse<BookingResponse>> list(
      @CurrentUser UserPrincipal principal,
      @RequestParam(defaultValue = "upcoming") String status,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size) {
    boolean upcoming = !"past".equalsIgnoreCase(status);
    return ApiResponse.ok(bookingService.listMine(principal, upcoming, page, size));
  }

  @Operation(summary = "Get one of my bookings")
  @GetMapping("/{bookingId}")
  public ApiResponse<BookingResponse> get(
      @CurrentUser UserPrincipal principal, @PathVariable Long bookingId) {
    return ApiResponse.ok(bookingService.getMine(principal, bookingId));
  }

  @Operation(summary = "Cancel my booking (until the salon's cancellation deadline)")
  @PostMapping("/{bookingId}/cancel")
  public ApiResponse<BookingResponse> cancel(
      @CurrentUser UserPrincipal principal,
      @PathVariable Long bookingId,
      @Valid @RequestBody(required = false) BookingActionRequest request) {
    String reason = request == null ? null : request.reason();
    return ApiResponse.ok(bookingService.cancelByCustomer(principal, bookingId, reason));
  }

  @Operation(summary = "Move my booking to another available slot")
  @PostMapping("/{bookingId}/reschedule")
  public ApiResponse<BookingResponse> reschedule(
      @CurrentUser UserPrincipal principal,
      @PathVariable Long bookingId,
      @Valid @RequestBody RescheduleBookingRequest request) {
    return ApiResponse.ok(bookingService.rescheduleByCustomer(principal, bookingId, request));
  }
}
