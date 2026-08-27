package com.slotify.module.staff.controller;

import com.slotify.common.api.ApiResponse;
import com.slotify.module.staff.dto.StaffMeResponse;
import com.slotify.module.staff.dto.StaffScheduleResponse;
import com.slotify.module.staff.dto.StaffStatsResponse;
import com.slotify.module.staff.dto.StatsRange;
import com.slotify.module.staff.dto.TimeOffRequest;
import com.slotify.module.staff.dto.TimeOffResponse;
import com.slotify.module.staff.service.StaffSelfService;
import com.slotify.security.CurrentUser;
import com.slotify.security.UserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Endpoints of the Staff app for the signed-in {@code STAFF} account: own profile, schedule, time
 * off and personal statistics. Bookings live in {@code StaffBookingController}.
 */
@Tag(name = "Staff app", description = "Self-service for staff members")
@RestController
@RequestMapping("/api/v1/staff")
@PreAuthorize("hasRole('STAFF')")
@RequiredArgsConstructor
public class StaffAppController {

  private final StaffSelfService staffSelfService;

  @Operation(summary = "My staff profile and salon")
  @GetMapping("/me")
  public ApiResponse<StaffMeResponse> me(@CurrentUser UserPrincipal principal) {
    return ApiResponse.ok(staffSelfService.me(principal.id()));
  }

  @Operation(summary = "My weekly shifts and upcoming overrides")
  @GetMapping("/shifts")
  public ApiResponse<StaffScheduleResponse> shifts(@CurrentUser UserPrincipal principal) {
    return ApiResponse.ok(staffSelfService.schedule(principal.id()));
  }

  @Operation(summary = "My earnings and booking statistics for a preset range")
  @GetMapping("/stats")
  public ApiResponse<StaffStatsResponse> stats(
      @CurrentUser UserPrincipal principal,
      @RequestParam(defaultValue = "MONTH") StatsRange range) {
    return ApiResponse.ok(staffSelfService.stats(principal.id(), range));
  }

  @Operation(summary = "My time-off entries")
  @GetMapping("/time-off")
  public ApiResponse<List<TimeOffResponse>> listTimeOff(@CurrentUser UserPrincipal principal) {
    return ApiResponse.ok(staffSelfService.listTimeOff(principal.id()));
  }

  @Operation(summary = "Request time off (pending owner approval)")
  @PostMapping("/time-off")
  @ResponseStatus(HttpStatus.CREATED)
  public ApiResponse<TimeOffResponse> requestTimeOff(
      @CurrentUser UserPrincipal principal, @Valid @RequestBody TimeOffRequest request) {
    return ApiResponse.ok(staffSelfService.requestTimeOff(principal.id(), request));
  }

  @Operation(summary = "Withdraw one of my time-off entries")
  @DeleteMapping("/time-off/{timeOffId}")
  public ApiResponse<Void> deleteTimeOff(
      @CurrentUser UserPrincipal principal, @PathVariable Long timeOffId) {
    staffSelfService.deleteTimeOff(principal.id(), timeOffId);
    return ApiResponse.ok();
  }
}
