package com.slotify.module.booking.controller;

import com.slotify.common.api.ApiResponse;
import com.slotify.common.api.PageResponse;
import com.slotify.module.booking.dto.BookingActionRequest;
import com.slotify.module.booking.dto.BookingListFilter;
import com.slotify.module.booking.dto.BookingResponse;
import com.slotify.module.booking.dto.RescheduleBookingRequest;
import com.slotify.module.booking.dto.WalkInBookingRequest;
import com.slotify.module.booking.entity.BookingStatus;
import com.slotify.module.booking.service.BookingService;
import com.slotify.security.CurrentUser;
import com.slotify.security.UserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.time.Instant;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Booking management in the admin panel. */
@Tag(name = "Bookings (owner)", description = "Manage bookings of a salon")
@RestController
@RequestMapping("/api/v1/admin/salons/{salonId}/bookings")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('SALON_OWNER','SUPER_ADMIN')")
public class OwnerBookingController {

  private final BookingService bookingService;

  @Operation(summary = "List bookings with filters")
  @GetMapping
  public ApiResponse<PageResponse<BookingResponse>> list(
      @CurrentUser UserPrincipal principal,
      @PathVariable Long salonId,
      @ParameterObject @ModelAttribute BookingListFilter filter) {
    return ApiResponse.ok(bookingService.listForSalon(principal, salonId, filter));
  }

  @Operation(summary = "Calendar feed: bookings starting in the range")
  @GetMapping("/calendar")
  public ApiResponse<List<BookingResponse>> calendar(
      @CurrentUser UserPrincipal principal,
      @PathVariable Long salonId,
      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to) {
    return ApiResponse.ok(bookingService.calendar(principal, salonId, from, to));
  }

  @Operation(summary = "Create a walk-in / phone booking")
  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public ApiResponse<BookingResponse> walkIn(
      @CurrentUser UserPrincipal principal,
      @PathVariable Long salonId,
      @Valid @RequestBody WalkInBookingRequest request) {
    return ApiResponse.ok(bookingService.createWalkIn(principal, salonId, request));
  }

  @Operation(summary = "Get a booking")
  @GetMapping("/{bookingId}")
  public ApiResponse<BookingResponse> get(
      @CurrentUser UserPrincipal principal,
      @PathVariable Long salonId,
      @PathVariable Long bookingId) {
    return ApiResponse.ok(bookingService.getForSalon(principal, salonId, bookingId));
  }

  @Operation(summary = "Confirm a pending booking")
  @PostMapping("/{bookingId}/confirm")
  public ApiResponse<BookingResponse> confirm(
      @CurrentUser UserPrincipal principal,
      @PathVariable Long salonId,
      @PathVariable Long bookingId) {
    return ApiResponse.ok(
        bookingService.changeStatusBySalon(
            principal, salonId, bookingId, BookingStatus.CONFIRMED, null));
  }

  @Operation(summary = "Reject a pending booking")
  @PostMapping("/{bookingId}/reject")
  public ApiResponse<BookingResponse> reject(
      @CurrentUser UserPrincipal principal,
      @PathVariable Long salonId,
      @PathVariable Long bookingId,
      @Valid @RequestBody(required = false) BookingActionRequest request) {
    return ApiResponse.ok(
        bookingService.changeStatusBySalon(
            principal, salonId, bookingId, BookingStatus.REJECTED, reason(request)));
  }

  @Operation(summary = "Cancel a booking on behalf of the salon")
  @PostMapping("/{bookingId}/cancel")
  public ApiResponse<BookingResponse> cancel(
      @CurrentUser UserPrincipal principal,
      @PathVariable Long salonId,
      @PathVariable Long bookingId,
      @Valid @RequestBody(required = false) BookingActionRequest request) {
    return ApiResponse.ok(
        bookingService.changeStatusBySalon(
            principal, salonId, bookingId, BookingStatus.CANCELLED, reason(request)));
  }

  @Operation(summary = "Mark the customer as arrived / service started")
  @PostMapping("/{bookingId}/start")
  public ApiResponse<BookingResponse> start(
      @CurrentUser UserPrincipal principal,
      @PathVariable Long salonId,
      @PathVariable Long bookingId) {
    return ApiResponse.ok(
        bookingService.changeStatusBySalon(
            principal, salonId, bookingId, BookingStatus.IN_PROGRESS, null));
  }

  @Operation(summary = "Complete a booking")
  @PostMapping("/{bookingId}/complete")
  public ApiResponse<BookingResponse> complete(
      @CurrentUser UserPrincipal principal,
      @PathVariable Long salonId,
      @PathVariable Long bookingId) {
    return ApiResponse.ok(
        bookingService.changeStatusBySalon(
            principal, salonId, bookingId, BookingStatus.COMPLETED, null));
  }

  @Operation(summary = "Mark as no-show")
  @PostMapping("/{bookingId}/no-show")
  public ApiResponse<BookingResponse> noShow(
      @CurrentUser UserPrincipal principal,
      @PathVariable Long salonId,
      @PathVariable Long bookingId) {
    return ApiResponse.ok(
        bookingService.changeStatusBySalon(
            principal, salonId, bookingId, BookingStatus.NO_SHOW, null));
  }

  @Operation(summary = "Move a booking to another slot / staff member")
  @PostMapping("/{bookingId}/reschedule")
  public ApiResponse<BookingResponse> reschedule(
      @CurrentUser UserPrincipal principal,
      @PathVariable Long salonId,
      @PathVariable Long bookingId,
      @Valid @RequestBody RescheduleBookingRequest request) {
    return ApiResponse.ok(bookingService.rescheduleBySalon(principal, salonId, bookingId, request));
  }

  private static String reason(BookingActionRequest request) {
    return request == null ? null : request.reason();
  }
}
