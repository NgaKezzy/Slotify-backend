package com.slotify.module.booking.controller;

import com.slotify.common.api.ApiResponse;
import com.slotify.module.booking.dto.BookingResponse;
import com.slotify.module.booking.entity.BookingStatus;
import com.slotify.module.booking.service.BookingService;
import com.slotify.security.CurrentUser;
import com.slotify.security.UserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Booking endpoints of the Staff app (own bookings only). */
@Tag(name = "Staff app", description = "Endpoints used by the Staff app")
@RestController
@RequestMapping("/api/v1/staff/bookings")
@RequiredArgsConstructor
@PreAuthorize("hasRole('STAFF')")
public class StaffBookingController {

  private final BookingService bookingService;

  @Operation(summary = "My bookings for a day (salon local date)")
  @GetMapping
  public ApiResponse<List<BookingResponse>> list(
      @CurrentUser UserPrincipal principal,
      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
    return ApiResponse.ok(bookingService.listForStaff(principal, date));
  }

  @Operation(summary = "Start the service")
  @PostMapping("/{bookingId}/start")
  public ApiResponse<BookingResponse> start(
      @CurrentUser UserPrincipal principal, @PathVariable Long bookingId) {
    return ApiResponse.ok(
        bookingService.changeStatusByStaff(principal, bookingId, BookingStatus.IN_PROGRESS));
  }

  @Operation(summary = "Complete the service")
  @PostMapping("/{bookingId}/complete")
  public ApiResponse<BookingResponse> complete(
      @CurrentUser UserPrincipal principal, @PathVariable Long bookingId) {
    return ApiResponse.ok(
        bookingService.changeStatusByStaff(principal, bookingId, BookingStatus.COMPLETED));
  }

  @Operation(summary = "Mark the customer as no-show")
  @PostMapping("/{bookingId}/no-show")
  public ApiResponse<BookingResponse> noShow(
      @CurrentUser UserPrincipal principal, @PathVariable Long bookingId) {
    return ApiResponse.ok(
        bookingService.changeStatusByStaff(principal, bookingId, BookingStatus.NO_SHOW));
  }
}
