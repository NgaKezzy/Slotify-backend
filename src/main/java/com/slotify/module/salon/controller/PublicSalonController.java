package com.slotify.module.salon.controller;

import com.slotify.common.api.ApiResponse;
import com.slotify.common.api.PageResponse;
import com.slotify.module.salon.dto.AmenityResponse;
import com.slotify.module.salon.dto.CategoryResponse;
import com.slotify.module.salon.dto.SalonDetailResponse;
import com.slotify.module.salon.dto.SalonSearchRequest;
import com.slotify.module.salon.dto.SalonSummaryResponse;
import com.slotify.module.salon.service.CatalogService;
import com.slotify.module.salon.service.SalonQueryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Public catalog: categories, amenities, salon search and salon detail (no auth required). */
@Tag(name = "Salons (public)", description = "Browse salons")
@SecurityRequirements
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class PublicSalonController {

  private final SalonQueryService salonQueryService;
  private final CatalogService catalogService;

  @Operation(summary = "List global categories")
  @GetMapping("/categories")
  public ApiResponse<List<CategoryResponse>> categories() {
    return ApiResponse.ok(catalogService.listCategories());
  }

  @Operation(summary = "List global amenities")
  @GetMapping("/amenities")
  public ApiResponse<List<AmenityResponse>> amenities() {
    return ApiResponse.ok(catalogService.listAmenities());
  }

  @Operation(summary = "Search active salons")
  @GetMapping("/salons")
  public ApiResponse<PageResponse<SalonSummaryResponse>> search(
      @Valid @ModelAttribute SalonSearchRequest request) {
    return ApiResponse.ok(salonQueryService.search(request));
  }

  @Operation(summary = "Get an active salon by id or slug")
  @GetMapping("/salons/{idOrSlug}")
  public ApiResponse<SalonDetailResponse> get(@PathVariable String idOrSlug) {
    return ApiResponse.ok(salonQueryService.getPublic(idOrSlug));
  }
}
