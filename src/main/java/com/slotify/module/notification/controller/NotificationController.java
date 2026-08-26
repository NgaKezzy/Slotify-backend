package com.slotify.module.notification.controller;

import com.slotify.common.api.ApiResponse;
import com.slotify.common.api.PageResponse;
import com.slotify.module.notification.dto.NotificationResponse;
import com.slotify.module.notification.service.NotificationService;
import com.slotify.security.CurrentUser;
import com.slotify.security.UserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Notification centre of the signed-in user. */
@Tag(name = "Me", description = "Profile of the signed-in user")
@RestController
@RequestMapping("/api/v1/me/notifications")
@RequiredArgsConstructor
public class NotificationController {

  private final NotificationService notificationService;

  @Operation(summary = "List my notifications, newest first")
  @GetMapping
  public ApiResponse<PageResponse<NotificationResponse>> list(
      @CurrentUser UserPrincipal principal,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size) {
    return ApiResponse.ok(notificationService.list(principal.id(), page, size));
  }

  @Operation(summary = "Number of unread notifications (badge)")
  @GetMapping("/unread-count")
  public ApiResponse<Map<String, Long>> unreadCount(@CurrentUser UserPrincipal principal) {
    return ApiResponse.ok(Map.of("count", notificationService.unreadCount(principal.id())));
  }

  @Operation(summary = "Mark one notification as read")
  @PutMapping("/{id}/read")
  public ApiResponse<Void> markRead(@CurrentUser UserPrincipal principal, @PathVariable Long id) {
    notificationService.markRead(principal.id(), id);
    return ApiResponse.ok();
  }

  @Operation(summary = "Mark all notifications as read")
  @PutMapping("/read-all")
  public ApiResponse<Void> markAllRead(@CurrentUser UserPrincipal principal) {
    notificationService.markAllRead(principal.id());
    return ApiResponse.ok();
  }
}
