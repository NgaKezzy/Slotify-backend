package com.slotify.module.salon.controller;

import com.slotify.common.api.ApiResponse;
import com.slotify.module.salon.dto.OpeningHourDto;
import com.slotify.module.salon.dto.SalonDetailResponse;
import com.slotify.module.salon.dto.SalonRequest;
import com.slotify.module.salon.dto.SalonSettingsDto;
import com.slotify.module.salon.dto.SalonSummaryResponse;
import com.slotify.module.salon.service.SalonManagementService;
import com.slotify.security.CurrentUser;
import com.slotify.security.UserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Salon administration for owners. Any signed-in user may create a salon (becoming SALON_OWNER);
 * everything else requires ownership of the salon (or SUPER_ADMIN).
 */
@Tag(name = "Salons (owner)", description = "Manage my salons")
@RestController
@RequestMapping("/api/v1/admin/salons")
@RequiredArgsConstructor
@Validated
public class OwnerSalonController {

  private final SalonManagementService salonService;

  @Operation(summary = "Create a salon (pending approval)")
  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  @PreAuthorize("isAuthenticated()")
  public ApiResponse<SalonDetailResponse> create(
      @CurrentUser UserPrincipal principal, @Valid @RequestBody SalonRequest request) {
    return ApiResponse.ok(salonService.create(principal, request));
  }

  @Operation(summary = "List my salons")
  @GetMapping
  @PreAuthorize("hasAnyRole('SALON_OWNER','SUPER_ADMIN')")
  public ApiResponse<List<SalonSummaryResponse>> list(@CurrentUser UserPrincipal principal) {
    return ApiResponse.ok(salonService.listOwned(principal));
  }

  @Operation(summary = "Get one of my salons")
  @GetMapping("/{salonId}")
  @PreAuthorize("hasAnyRole('SALON_OWNER','SUPER_ADMIN')")
  public ApiResponse<SalonDetailResponse> get(
      @CurrentUser UserPrincipal principal, @PathVariable Long salonId) {
    return ApiResponse.ok(salonService.getOwned(salonId, principal));
  }

  @Operation(summary = "Update salon profile")
  @PutMapping("/{salonId}")
  @PreAuthorize("hasAnyRole('SALON_OWNER','SUPER_ADMIN')")
  public ApiResponse<SalonDetailResponse> update(
      @CurrentUser UserPrincipal principal,
      @PathVariable Long salonId,
      @Valid @RequestBody SalonRequest request) {
    return ApiResponse.ok(salonService.update(salonId, principal, request));
  }

  @Operation(summary = "Replace opening hours (one entry per weekday)")
  @PutMapping("/{salonId}/opening-hours")
  @PreAuthorize("hasAnyRole('SALON_OWNER','SUPER_ADMIN')")
  public ApiResponse<List<OpeningHourDto>> openingHours(
      @CurrentUser UserPrincipal principal,
      @PathVariable Long salonId,
      @RequestBody @Size(max = 7) List<@Valid OpeningHourDto> hours) {
    return ApiResponse.ok(salonService.updateOpeningHours(salonId, principal, hours));
  }

  @Operation(summary = "Update booking & payment rules")
  @PutMapping("/{salonId}/settings")
  @PreAuthorize("hasAnyRole('SALON_OWNER','SUPER_ADMIN')")
  public ApiResponse<SalonSettingsDto> settings(
      @CurrentUser UserPrincipal principal,
      @PathVariable Long salonId,
      @Valid @RequestBody SalonSettingsDto settings) {
    return ApiResponse.ok(salonService.updateSettings(salonId, principal, settings));
  }

  @Operation(summary = "Replace gallery images (ordered URLs from /uploads/presign)")
  @PutMapping("/{salonId}/images")
  @PreAuthorize("hasAnyRole('SALON_OWNER','SUPER_ADMIN')")
  public ApiResponse<List<String>> images(
      @CurrentUser UserPrincipal principal,
      @PathVariable Long salonId,
      @RequestBody @Size(max = 20) List<String> urls) {
    return ApiResponse.ok(salonService.updateImages(salonId, principal, urls));
  }
}
