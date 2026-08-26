package com.slotify.module.staff.controller;

import com.slotify.common.api.ApiResponse;
import com.slotify.module.staff.dto.CreateStaffRequest;
import com.slotify.module.staff.dto.ReplaceShiftsRequest;
import com.slotify.module.staff.dto.ShiftOverrideRequest;
import com.slotify.module.staff.dto.ShiftOverrideResponse;
import com.slotify.module.staff.dto.ShiftResponse;
import com.slotify.module.staff.dto.StaffResponse;
import com.slotify.module.staff.dto.StaffServicesRequest;
import com.slotify.module.staff.dto.TimeOffRequest;
import com.slotify.module.staff.dto.TimeOffResponse;
import com.slotify.module.staff.dto.UpdateStaffRequest;
import com.slotify.module.staff.service.StaffService;
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
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Salon-owner endpoints for managing staff, their services, shifts, overrides and time off. The
 * salon must belong to the caller ({@code SUPER_ADMIN} may manage any salon).
 */
@Tag(name = "Admin · Staff", description = "Staff management for salon owners")
@RestController
@RequestMapping("/api/v1/admin/salons/{salonId}/staff")
@PreAuthorize("hasAnyRole('SALON_OWNER', 'SUPER_ADMIN')")
@RequiredArgsConstructor
public class AdminStaffController {

  private final StaffService staffService;

  @Operation(summary = "List staff of the salon (active and inactive)")
  @GetMapping
  public ApiResponse<List<StaffResponse>> list(
      @PathVariable Long salonId, @CurrentUser UserPrincipal principal) {
    return ApiResponse.ok(staffService.list(salonId, principal));
  }

  @Operation(summary = "Create a staff member, optionally inviting them by email")
  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public ApiResponse<StaffResponse> create(
      @PathVariable Long salonId,
      @CurrentUser UserPrincipal principal,
      @Valid @RequestBody CreateStaffRequest request) {
    return ApiResponse.ok(staffService.create(salonId, principal, request));
  }

  @Operation(summary = "Get a staff member")
  @GetMapping("/{staffId}")
  public ApiResponse<StaffResponse> get(
      @PathVariable Long salonId,
      @PathVariable Long staffId,
      @CurrentUser UserPrincipal principal) {
    return ApiResponse.ok(staffService.get(salonId, staffId, principal));
  }

  @Operation(summary = "Update a staff member's profile or (de)activate them")
  @PutMapping("/{staffId}")
  public ApiResponse<StaffResponse> update(
      @PathVariable Long salonId,
      @PathVariable Long staffId,
      @CurrentUser UserPrincipal principal,
      @Valid @RequestBody UpdateStaffRequest request) {
    return ApiResponse.ok(staffService.update(salonId, staffId, principal, request));
  }

  @Operation(summary = "Remove a staff member (soft delete)")
  @DeleteMapping("/{staffId}")
  public ApiResponse<Void> delete(
      @PathVariable Long salonId,
      @PathVariable Long staffId,
      @CurrentUser UserPrincipal principal) {
    staffService.delete(salonId, staffId, principal);
    return ApiResponse.ok();
  }

  @Operation(summary = "Replace the services a staff member can perform")
  @PutMapping("/{staffId}/services")
  public ApiResponse<StaffResponse> replaceServices(
      @PathVariable Long salonId,
      @PathVariable Long staffId,
      @CurrentUser UserPrincipal principal,
      @Valid @RequestBody StaffServicesRequest request) {
    return ApiResponse.ok(staffService.replaceServices(salonId, staffId, principal, request));
  }

  @Operation(summary = "Get the weekly shift schedule")
  @GetMapping("/{staffId}/shifts")
  public ApiResponse<List<ShiftResponse>> getShifts(
      @PathVariable Long salonId,
      @PathVariable Long staffId,
      @CurrentUser UserPrincipal principal) {
    return ApiResponse.ok(staffService.getShifts(salonId, staffId, principal));
  }

  @Operation(summary = "Replace the weekly shift schedule")
  @PutMapping("/{staffId}/shifts")
  public ApiResponse<List<ShiftResponse>> replaceShifts(
      @PathVariable Long salonId,
      @PathVariable Long staffId,
      @CurrentUser UserPrincipal principal,
      @Valid @RequestBody ReplaceShiftsRequest request) {
    return ApiResponse.ok(staffService.replaceShifts(salonId, staffId, principal, request));
  }

  @Operation(summary = "Create or replace a per-date shift override (day off or custom window)")
  @PostMapping("/{staffId}/shift-overrides")
  @ResponseStatus(HttpStatus.CREATED)
  public ApiResponse<ShiftOverrideResponse> createOverride(
      @PathVariable Long salonId,
      @PathVariable Long staffId,
      @CurrentUser UserPrincipal principal,
      @Valid @RequestBody ShiftOverrideRequest request) {
    return ApiResponse.ok(staffService.upsertOverride(salonId, staffId, principal, request));
  }

  @Operation(summary = "Delete a shift override")
  @DeleteMapping("/{staffId}/shift-overrides/{overrideId}")
  public ApiResponse<Void> deleteOverride(
      @PathVariable Long salonId,
      @PathVariable Long staffId,
      @PathVariable Long overrideId,
      @CurrentUser UserPrincipal principal) {
    staffService.deleteOverride(salonId, staffId, overrideId, principal);
    return ApiResponse.ok();
  }

  @Operation(summary = "List time off of a staff member")
  @GetMapping("/{staffId}/time-off")
  public ApiResponse<List<TimeOffResponse>> listTimeOff(
      @PathVariable Long salonId,
      @PathVariable Long staffId,
      @CurrentUser UserPrincipal principal) {
    return ApiResponse.ok(staffService.listTimeOff(salonId, staffId, principal));
  }

  @Operation(summary = "Add approved time off for a staff member")
  @PostMapping("/{staffId}/time-off")
  @ResponseStatus(HttpStatus.CREATED)
  public ApiResponse<TimeOffResponse> createTimeOff(
      @PathVariable Long salonId,
      @PathVariable Long staffId,
      @CurrentUser UserPrincipal principal,
      @Valid @RequestBody TimeOffRequest request) {
    return ApiResponse.ok(staffService.createTimeOff(salonId, staffId, principal, request));
  }

  @Operation(summary = "Delete a time-off entry")
  @DeleteMapping("/{staffId}/time-off/{timeOffId}")
  public ApiResponse<Void> deleteTimeOff(
      @PathVariable Long salonId,
      @PathVariable Long staffId,
      @PathVariable Long timeOffId,
      @CurrentUser UserPrincipal principal) {
    staffService.deleteTimeOff(salonId, staffId, timeOffId, principal);
    return ApiResponse.ok();
  }
}
