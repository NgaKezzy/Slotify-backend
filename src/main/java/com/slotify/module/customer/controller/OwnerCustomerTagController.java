package com.slotify.module.customer.controller;

import com.slotify.common.api.ApiResponse;
import com.slotify.module.customer.dto.CustomerTagRequest;
import com.slotify.module.customer.dto.CustomerTagResponse;
import com.slotify.module.customer.service.CustomerTagService;
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

/** Tag catalogue of a salon (labels the owner can attach to customers). */
@Tag(name = "Customers (owner)", description = "CRM: customers, notes and tags of a salon")
@RestController
@RequestMapping("/api/v1/admin/salons/{salonId}/customer-tags")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('SALON_OWNER','SUPER_ADMIN')")
public class OwnerCustomerTagController {

  private final CustomerTagService tagService;

  @Operation(summary = "List tags")
  @GetMapping
  public ApiResponse<List<CustomerTagResponse>> list(
      @CurrentUser UserPrincipal principal, @PathVariable Long salonId) {
    return ApiResponse.ok(tagService.list(principal, salonId));
  }

  @Operation(summary = "Create a tag")
  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public ApiResponse<CustomerTagResponse> create(
      @CurrentUser UserPrincipal principal,
      @PathVariable Long salonId,
      @Valid @RequestBody CustomerTagRequest request) {
    return ApiResponse.ok(tagService.create(principal, salonId, request));
  }

  @Operation(summary = "Rename / recolour a tag")
  @PutMapping("/{tagId}")
  public ApiResponse<CustomerTagResponse> update(
      @CurrentUser UserPrincipal principal,
      @PathVariable Long salonId,
      @PathVariable Long tagId,
      @Valid @RequestBody CustomerTagRequest request) {
    return ApiResponse.ok(tagService.update(principal, salonId, tagId, request));
  }

  @Operation(summary = "Delete a tag (removes it from every customer)")
  @DeleteMapping("/{tagId}")
  public ApiResponse<Void> delete(
      @CurrentUser UserPrincipal principal, @PathVariable Long salonId, @PathVariable Long tagId) {
    tagService.delete(principal, salonId, tagId);
    return ApiResponse.ok();
  }
}
