package com.slotify.module.salon.controller;

import com.slotify.common.api.ApiResponse;
import com.slotify.module.salon.dto.AmenityRequest;
import com.slotify.module.salon.dto.AmenityResponse;
import com.slotify.module.salon.dto.CategoryRequest;
import com.slotify.module.salon.dto.CategoryResponse;
import com.slotify.module.salon.dto.SalonSummaryResponse;
import com.slotify.module.salon.entity.SalonStatus;
import com.slotify.module.salon.service.CatalogService;
import com.slotify.module.salon.service.SalonManagementService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Platform administration (SUPER_ADMIN): salon approval and the global catalog. */
@Tag(name = "Platform", description = "Super admin: salons approval, categories, amenities")
@RestController
@RequestMapping("/api/v1/admin/platform")
@RequiredArgsConstructor
@PreAuthorize("hasRole('SUPER_ADMIN')")
public class PlatformSalonController {

  private final SalonManagementService salonService;
  private final CatalogService catalogService;

  /**
   * Body of the commission endpoint.
   *
   * @param commissionPercent platform commission in percent (0-100)
   */
  public record CommissionRequest(
      @NotNull @DecimalMin("0.00") @DecimalMax("100.00") BigDecimal commissionPercent) {}

  @Operation(summary = "List salons, optionally filtered by status")
  @GetMapping("/salons")
  public ApiResponse<List<SalonSummaryResponse>> salons(
      @RequestParam(required = false) SalonStatus status) {
    return ApiResponse.ok(salonService.listAll(status));
  }

  @Operation(summary = "Approve a salon (make it visible and bookable)")
  @PostMapping("/salons/{salonId}/approve")
  public ApiResponse<SalonSummaryResponse> approve(@PathVariable Long salonId) {
    return ApiResponse.ok(salonService.changeStatus(salonId, SalonStatus.ACTIVE));
  }

  @Operation(summary = "Suspend a salon (hide it)")
  @PostMapping("/salons/{salonId}/suspend")
  public ApiResponse<SalonSummaryResponse> suspend(@PathVariable Long salonId) {
    return ApiResponse.ok(salonService.changeStatus(salonId, SalonStatus.SUSPENDED));
  }

  @Operation(summary = "Set the platform commission of a salon")
  @PutMapping("/salons/{salonId}/commission")
  public ApiResponse<SalonSummaryResponse> commission(
      @PathVariable Long salonId, @Valid @RequestBody CommissionRequest request) {
    return ApiResponse.ok(salonService.updateCommission(salonId, request.commissionPercent()));
  }

  @Operation(summary = "Create a category")
  @PostMapping("/categories")
  @ResponseStatus(HttpStatus.CREATED)
  public ApiResponse<CategoryResponse> createCategory(@Valid @RequestBody CategoryRequest request) {
    return ApiResponse.ok(catalogService.createCategory(request));
  }

  @Operation(summary = "Update a category")
  @PutMapping("/categories/{id}")
  public ApiResponse<CategoryResponse> updateCategory(
      @PathVariable Long id, @Valid @RequestBody CategoryRequest request) {
    return ApiResponse.ok(catalogService.updateCategory(id, request));
  }

  @Operation(summary = "Delete a category")
  @DeleteMapping("/categories/{id}")
  public ApiResponse<Void> deleteCategory(@PathVariable Long id) {
    catalogService.deleteCategory(id);
    return ApiResponse.ok();
  }

  @Operation(summary = "Create an amenity")
  @PostMapping("/amenities")
  @ResponseStatus(HttpStatus.CREATED)
  public ApiResponse<AmenityResponse> createAmenity(@Valid @RequestBody AmenityRequest request) {
    return ApiResponse.ok(catalogService.createAmenity(request));
  }

  @Operation(summary = "Update an amenity")
  @PutMapping("/amenities/{id}")
  public ApiResponse<AmenityResponse> updateAmenity(
      @PathVariable Long id, @Valid @RequestBody AmenityRequest request) {
    return ApiResponse.ok(catalogService.updateAmenity(id, request));
  }

  @Operation(summary = "Delete an amenity")
  @DeleteMapping("/amenities/{id}")
  public ApiResponse<Void> deleteAmenity(@PathVariable Long id) {
    catalogService.deleteAmenity(id);
    return ApiResponse.ok();
  }
}
