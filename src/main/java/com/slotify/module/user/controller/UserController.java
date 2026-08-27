package com.slotify.module.user.controller;

import com.slotify.common.api.ApiResponse;
import com.slotify.module.notification.dto.PushTestResponse;
import com.slotify.module.notification.service.FcmService;
import com.slotify.module.user.dto.ChangePasswordRequest;
import com.slotify.module.user.dto.DeleteAccountRequest;
import com.slotify.module.user.dto.DeviceTokenRequest;
import com.slotify.module.user.dto.UpdateProfileRequest;
import com.slotify.module.user.dto.UserDataExport;
import com.slotify.module.user.dto.UserResponse;
import com.slotify.module.user.service.GdprService;
import com.slotify.module.user.service.UserService;
import com.slotify.security.CurrentUser;
import com.slotify.security.UserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import tools.jackson.databind.ObjectMapper;

/** Endpoints for the signed-in user's own account. */
@Tag(name = "Me", description = "Profile of the signed-in user")
@RestController
@RequestMapping("/api/v1/me")
@RequiredArgsConstructor
public class UserController {

  private final UserService userService;
  private final FcmService fcmService;
  private final MessageSource messageSource;
  private final GdprService gdprService;
  private final ObjectMapper objectMapper;

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

  @Operation(
      summary = "Delete my account (GDPR)",
      description =
          "Anonymises the account and removes devices, sessions, favourites and notifications. "
              + "Bookings are kept as financial history. Local accounts must confirm the password.")
  @DeleteMapping
  public ApiResponse<Void> deleteAccount(
      @CurrentUser UserPrincipal principal,
      @RequestBody(required = false) DeleteAccountRequest request) {
    gdprService.deleteAccount(principal, request);
    return ApiResponse.ok();
  }

  @Operation(
      summary = "Export my data (GDPR)",
      description = "Downloads a JSON file with everything stored about the account.")
  @GetMapping(value = "/export", produces = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<byte[]> export(@CurrentUser UserPrincipal principal) {
    UserDataExport export = gdprService.export(principal.id());
    byte[] body = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsBytes(export);
    ContentDisposition disposition =
        ContentDisposition.attachment()
            .filename("slotify-data-" + principal.id() + ".json")
            .build();
    return ResponseEntity.ok()
        .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
        .contentType(MediaType.APPLICATION_JSON)
        .body(body);
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

  @Operation(
      summary = "Send a test push notification to my devices",
      description =
          "Verifies the Firebase setup end to end: reports whether the server has credentials,"
              + " how many devices are registered for the caller and how many accepted the push.")
  @PostMapping("/push-test")
  public ApiResponse<PushTestResponse> pushTest(@CurrentUser UserPrincipal principal) {
    Locale locale = LocaleContextHolder.getLocale();
    String title = messageSource.getMessage("notification.test.title", null, locale);
    String body = messageSource.getMessage("notification.test.body", null, locale);
    return ApiResponse.ok(fcmService.sendTestToUser(principal.id(), title, body));
  }
}
