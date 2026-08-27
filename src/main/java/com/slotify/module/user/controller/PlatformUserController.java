package com.slotify.module.user.controller;

import com.slotify.common.api.ApiResponse;
import com.slotify.common.api.PageResponse;
import com.slotify.module.user.dto.PlatformUserResponse;
import com.slotify.module.user.dto.UpdateUserRoleRequest;
import com.slotify.module.user.dto.UpdateUserStatusRequest;
import com.slotify.module.user.entity.Role;
import com.slotify.module.user.service.PlatformUserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** User administration (SUPER_ADMIN). */
@Tag(name = "Platform users", description = "Super admin: search, suspend and re-role users")
@RestController
@RequestMapping("/api/v1/admin/platform/users")
@RequiredArgsConstructor
@PreAuthorize("hasRole('SUPER_ADMIN')")
public class PlatformUserController {

  private final PlatformUserService platformUserService;

  @Operation(summary = "Search users by email / name and role")
  @GetMapping
  public ApiResponse<PageResponse<PlatformUserResponse>> search(
      @RequestParam(required = false) String q,
      @RequestParam(required = false) Role role,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size) {
    return ApiResponse.ok(platformUserService.search(q, role, page, size));
  }

  @Operation(summary = "Get a user")
  @GetMapping("/{id}")
  public ApiResponse<PlatformUserResponse> get(@PathVariable Long id) {
    return ApiResponse.ok(platformUserService.get(id));
  }

  @Operation(summary = "Activate or suspend a user (suspension ends all sessions)")
  @PutMapping("/{id}/status")
  public ApiResponse<PlatformUserResponse> updateStatus(
      @PathVariable Long id, @Valid @RequestBody UpdateUserStatusRequest request) {
    return ApiResponse.ok(platformUserService.updateStatus(id, request.status()));
  }

  @Operation(summary = "Change a user's role (the last super admin cannot be demoted)")
  @PutMapping("/{id}/role")
  public ApiResponse<PlatformUserResponse> updateRole(
      @PathVariable Long id, @Valid @RequestBody UpdateUserRoleRequest request) {
    return ApiResponse.ok(platformUserService.updateRole(id, request.role()));
  }
}
