package com.slotify.module.user.controller;

import com.slotify.common.api.ApiResponse;
import com.slotify.module.user.dto.ChangePasswordRequest;
import com.slotify.module.user.dto.DeviceTokenRequest;
import com.slotify.module.user.dto.UpdateProfileRequest;
import com.slotify.module.user.dto.UserResponse;
import com.slotify.module.user.service.UserService;
import com.slotify.security.CurrentUser;
import com.slotify.security.UserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Endpoints for the signed-in user's own account. */
@Tag(name = "Me", description = "Profile of the signed-in user")
@RestController
@RequestMapping("/api/v1/me")
@RequiredArgsConstructor
public class UserController {

  private final UserService userService;

  @Operation(summary = "Get my profile")
  @GetMapping
  public ApiResponse<UserResponse> me(@CurrentUser UserPrincipal principal) {
    return ApiResponse.ok(userService.getProfile(principal.id()));
  }

  @Operation(summary = "Update my profile")
  @PutMapping
  public ApiResponse<UserResponse> update(
      @CurrentUser UserPrincipal principal, @Valid @RequestBody UpdateProfileRequest request) {
    return ApiResponse.ok(userService.updateProfile(principal.id(), request));
  }

  @Operation(summary = "Change my password")
  @PutMapping("/password")
  public ApiResponse<Void> changePassword(
      @CurrentUser UserPrincipal principal, @Valid @RequestBody ChangePasswordRequest request) {
    userService.changePassword(principal.id(), request);
    return ApiResponse.ok();
  }

  @Operation(summary = "Register a push notification (FCM) token for this device")
  @PostMapping("/device-token")
  public ApiResponse<Void> registerDeviceToken(
      @CurrentUser UserPrincipal principal, @Valid @RequestBody DeviceTokenRequest request) {
    userService.registerDeviceToken(principal.id(), request);
    return ApiResponse.ok();
  }

  @Operation(summary = "Remove a push notification token (sign-out on this device)")
  @DeleteMapping("/device-token")
  public ApiResponse<Void> removeDeviceToken(@RequestParam("token") String token) {
    userService.removeDeviceToken(token);
    return ApiResponse.ok();
  }
}
