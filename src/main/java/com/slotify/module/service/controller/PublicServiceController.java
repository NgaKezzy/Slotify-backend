package com.slotify.module.service.controller;

import com.slotify.common.api.ApiResponse;
import com.slotify.module.service.dto.ServiceCategoryResponse;
import com.slotify.module.service.dto.ServiceResponse;
import com.slotify.module.service.service.ServiceCatalogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Public service menu of an active salon. */
@Tag(name = "Salons (public)", description = "Browse salons")
@SecurityRequirements
@RestController
@RequestMapping("/api/v1/salons/{idOrSlug}")
@RequiredArgsConstructor
public class PublicServiceController {

  private final ServiceCatalogService catalogService;

  @Operation(summary = "List active services of a salon")
  @GetMapping("/services")
  public ApiResponse<List<ServiceResponse>> services(@PathVariable String idOrSlug) {
    return ApiResponse.ok(catalogService.listPublic(idOrSlug));
  }

  @Operation(summary = "List service categories of a salon")
  @GetMapping("/service-categories")
  public ApiResponse<List<ServiceCategoryResponse>> categories(@PathVariable String idOrSlug) {
    return ApiResponse.ok(catalogService.listPublicCategories(idOrSlug));
  }
}
