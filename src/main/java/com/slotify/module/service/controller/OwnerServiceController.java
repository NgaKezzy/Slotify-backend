package com.slotify.module.service.controller;

import com.slotify.common.api.ApiResponse;
import com.slotify.module.service.dto.ServiceCategoryRequest;
import com.slotify.module.service.dto.ServiceCategoryResponse;
import com.slotify.module.service.dto.ServiceRequest;
import com.slotify.module.service.dto.ServiceResponse;
import com.slotify.module.service.service.ServiceCatalogService;
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

/** Service menu management for salon owners. */
@Tag(name = "Services (owner)", description = "Manage the service menu of a salon")
@RestController
@RequestMapping("/api/v1/admin/salons/{salonId}")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('SALON_OWNER','SUPER_ADMIN')")
public class OwnerServiceController {

  private final ServiceCatalogService catalogService;

  @Operation(summary = "List service categories")
  @GetMapping("/service-categories")
  public ApiResponse<List<ServiceCategoryResponse>> listCategories(
      @CurrentUser UserPrincipal principal, @PathVariable Long salonId) {
    return ApiResponse.ok(catalogService.listCategories(salonId, principal));
  }

  @Operation(summary = "Create a service category")
  @PostMapping("/service-categories")
  @ResponseStatus(HttpStatus.CREATED)
  public ApiResponse<ServiceCategoryResponse> createCategory(
      @CurrentUser UserPrincipal principal,
      @PathVariable Long salonId,
      @Valid @RequestBody ServiceCategoryRequest request) {
    return ApiResponse.ok(catalogService.createCategory(salonId, principal, request));
  }

  @Operation(summary = "Update a service category")
  @PutMapping("/service-categories/{categoryId}")
  public ApiResponse<ServiceCategoryResponse> updateCategory(
      @CurrentUser UserPrincipal principal,
      @PathVariable Long salonId,
      @PathVariable Long categoryId,
      @Valid @RequestBody ServiceCategoryRequest request) {
    return ApiResponse.ok(catalogService.updateCategory(salonId, categoryId, principal, request));
  }

  @Operation(summary = "Delete a service category")
  @DeleteMapping("/service-categories/{categoryId}")
  public ApiResponse<Void> deleteCategory(
      @CurrentUser UserPrincipal principal,
      @PathVariable Long salonId,
      @PathVariable Long categoryId) {
    catalogService.deleteCategory(salonId, categoryId, principal);
    return ApiResponse.ok();
  }

  @Operation(summary = "List all services (including inactive)")
  @GetMapping("/services")
  public ApiResponse<List<ServiceResponse>> listServices(
      @CurrentUser UserPrincipal principal, @PathVariable Long salonId) {
    return ApiResponse.ok(catalogService.listServices(salonId, principal));
  }

  @Operation(summary = "Create a service")
  @PostMapping("/services")
  @ResponseStatus(HttpStatus.CREATED)
  public ApiResponse<ServiceResponse> createService(
      @CurrentUser UserPrincipal principal,
      @PathVariable Long salonId,
      @Valid @RequestBody ServiceRequest request) {
    return ApiResponse.ok(catalogService.createService(salonId, principal, request));
  }

  @Operation(summary = "Update a service")
  @PutMapping("/services/{serviceId}")
  public ApiResponse<ServiceResponse> updateService(
      @CurrentUser UserPrincipal principal,
      @PathVariable Long salonId,
      @PathVariable Long serviceId,
      @Valid @RequestBody ServiceRequest request) {
    return ApiResponse.ok(catalogService.updateService(salonId, serviceId, principal, request));
  }

  @Operation(summary = "Delete a service (soft delete)")
  @DeleteMapping("/services/{serviceId}")
  public ApiResponse<Void> deleteService(
      @CurrentUser UserPrincipal principal,
      @PathVariable Long salonId,
      @PathVariable Long serviceId) {
    catalogService.deleteService(salonId, serviceId, principal);
    return ApiResponse.ok();
  }
}
