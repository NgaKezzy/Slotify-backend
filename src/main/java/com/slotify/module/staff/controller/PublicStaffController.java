package com.slotify.module.staff.controller;

import com.slotify.common.api.ApiResponse;
import com.slotify.module.staff.dto.PublicStaffResponse;
import com.slotify.module.staff.service.StaffService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Public (unauthenticated) staff catalog of a salon, used by the customer app when choosing a staff
 * member during booking. {@code /api/v1/salons/**} is whitelisted in {@code SecurityConfig}.
 */
@Tag(name = "Public catalog", description = "Salons, services and staff visible to customers")
@RestController
@RequestMapping("/api/v1/salons/{salonId}/staff")
@RequiredArgsConstructor
public class PublicStaffController {

  private final StaffService staffService;

  @Operation(summary = "Active staff of an active salon with the services they perform")
  @GetMapping
  public ApiResponse<List<PublicStaffResponse>> list(@PathVariable Long salonId) {
    return ApiResponse.ok(staffService.listPublic(salonId));
  }
}
