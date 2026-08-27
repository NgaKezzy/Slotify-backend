package com.slotify.module.notification.controller;

import com.slotify.common.api.ApiResponse;
import com.slotify.module.notification.dto.BroadcastRequest;
import com.slotify.module.notification.dto.BroadcastResponse;
import com.slotify.module.notification.service.NotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Platform-wide announcements (SUPER_ADMIN). */
@Tag(name = "Platform notifications", description = "Super admin: push announcements to app users")
@RestController
@RequestMapping("/api/v1/admin/platform/notifications")
@RequiredArgsConstructor
@PreAuthorize("hasRole('SUPER_ADMIN')")
public class PlatformNotificationController {

  private final NotificationService notificationService;

  @Operation(
      summary = "Send an announcement to customers, staff or both",
      description =
          "Stores an in-app notification for every user of the audience and pushes it to their"
              + " registered devices (when Firebase is configured).")
  @PostMapping("/broadcast")
  public ApiResponse<BroadcastResponse> broadcast(@Valid @RequestBody BroadcastRequest request) {
    return ApiResponse.ok(notificationService.broadcast(request));
  }
}
