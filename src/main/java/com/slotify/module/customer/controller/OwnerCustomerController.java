package com.slotify.module.customer.controller;

import com.slotify.common.api.ApiResponse;
import com.slotify.common.api.PageResponse;
import com.slotify.module.customer.dto.AssignTagsRequest;
import com.slotify.module.customer.dto.CustomerDetailResponse;
import com.slotify.module.customer.dto.CustomerNoteRequest;
import com.slotify.module.customer.dto.CustomerNoteResponse;
import com.slotify.module.customer.dto.CustomerSummaryResponse;
import com.slotify.module.customer.dto.CustomerTagResponse;
import com.slotify.module.customer.service.CustomerService;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** CRM endpoints of the admin panel: customers of a salon, their notes and tags. */
@Tag(name = "Customers (owner)", description = "CRM: customers, notes and tags of a salon")
@RestController
@RequestMapping("/api/v1/admin/salons/{salonId}/customers")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('SALON_OWNER','SUPER_ADMIN')")
public class OwnerCustomerController {

  private final CustomerService customerService;

  @Operation(summary = "List customers with visit statistics (q filters name / email)")
  @GetMapping
  public ApiResponse<PageResponse<CustomerSummaryResponse>> list(
      @CurrentUser UserPrincipal principal,
      @PathVariable Long salonId,
      @RequestParam(required = false) String q,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size) {
    return ApiResponse.ok(customerService.list(principal, salonId, q, page, size));
  }

  @Operation(summary = "Customer detail: statistics, notes, tags and recent bookings")
  @GetMapping("/{customerId}")
  public ApiResponse<CustomerDetailResponse> get(
      @CurrentUser UserPrincipal principal,
      @PathVariable Long salonId,
      @PathVariable Long customerId) {
    return ApiResponse.ok(customerService.get(principal, salonId, customerId));
  }

  @Operation(summary = "Internal notes about a customer")
  @GetMapping("/{customerId}/notes")
  public ApiResponse<List<CustomerNoteResponse>> notes(
      @CurrentUser UserPrincipal principal,
      @PathVariable Long salonId,
      @PathVariable Long customerId) {
    return ApiResponse.ok(customerService.listNotes(principal, salonId, customerId));
  }

  @Operation(summary = "Add an internal note")
  @PostMapping("/{customerId}/notes")
  @ResponseStatus(HttpStatus.CREATED)
  public ApiResponse<CustomerNoteResponse> addNote(
      @CurrentUser UserPrincipal principal,
      @PathVariable Long salonId,
      @PathVariable Long customerId,
      @Valid @RequestBody CustomerNoteRequest request) {
    return ApiResponse.ok(
        customerService.addNote(principal, salonId, customerId, request.note().trim()));
  }

  @Operation(summary = "Delete an internal note")
  @DeleteMapping("/{customerId}/notes/{noteId}")
  public ApiResponse<Void> deleteNote(
      @CurrentUser UserPrincipal principal,
      @PathVariable Long salonId,
      @PathVariable Long customerId,
      @PathVariable Long noteId) {
    customerService.deleteNote(principal, salonId, customerId, noteId);
    return ApiResponse.ok();
  }

  @Operation(summary = "Replace the customer's tags")
  @PutMapping("/{customerId}/tags")
  public ApiResponse<List<CustomerTagResponse>> assignTags(
      @CurrentUser UserPrincipal principal,
      @PathVariable Long salonId,
      @PathVariable Long customerId,
      @Valid @RequestBody AssignTagsRequest request) {
    return ApiResponse.ok(
        customerService.assignTags(principal, salonId, customerId, request.tagIds()));
  }
}
