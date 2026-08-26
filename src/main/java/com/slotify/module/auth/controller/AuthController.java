package com.slotify.module.auth.controller;

import com.slotify.common.api.ApiResponse;
import com.slotify.module.auth.dto.AuthResponse;
import com.slotify.module.auth.dto.ForgotPasswordRequest;
import com.slotify.module.auth.dto.GoogleLoginRequest;
import com.slotify.module.auth.dto.LoginRequest;
import com.slotify.module.auth.dto.RefreshTokenRequest;
import com.slotify.module.auth.dto.RegisterRequest;
import com.slotify.module.auth.dto.ResetPasswordRequest;
import com.slotify.module.auth.dto.VerifyEmailRequest;
import com.slotify.module.auth.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Public authentication endpoints (no bearer token required). */
@Tag(name = "Auth", description = "Registration, login and session management")
@SecurityRequirements
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

  private final AuthService authService;

  @Operation(summary = "Register a new customer account")
  @PostMapping("/register")
  @ResponseStatus(HttpStatus.CREATED)
  public ApiResponse<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
    return ApiResponse.ok(authService.register(request));
  }

  @Operation(summary = "Sign in with email and password")
  @PostMapping("/login")
  public ApiResponse<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
    return ApiResponse.ok(authService.login(request));
  }

  @Operation(summary = "Sign in with a Google ID token")
  @PostMapping("/google")
  public ApiResponse<AuthResponse> google(@Valid @RequestBody GoogleLoginRequest request) {
    return ApiResponse.ok(authService.loginWithGoogle(request.idToken()));
  }

  @Operation(summary = "Exchange a refresh token for a new token pair")
  @PostMapping("/refresh")
  public ApiResponse<AuthResponse> refresh(@Valid @RequestBody RefreshTokenRequest request) {
    return ApiResponse.ok(authService.refresh(request.refreshToken()));
  }

  @Operation(summary = "Sign out (revoke the refresh token)")
  @PostMapping("/logout")
  public ApiResponse<Void> logout(@Valid @RequestBody RefreshTokenRequest request) {
    authService.logout(request.refreshToken());
    return ApiResponse.ok();
  }

  @Operation(summary = "Confirm an email address with the token from the verification email")
  @PostMapping("/verify-email")
  public ApiResponse<Void> verifyEmail(@Valid @RequestBody VerifyEmailRequest request) {
    authService.verifyEmail(request.token());
    return ApiResponse.ok();
  }

  @Operation(summary = "Send a password reset email (always returns success)")
  @PostMapping("/forgot-password")
  public ApiResponse<Void> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
    authService.forgotPassword(request.email());
    return ApiResponse.ok();
  }

  @Operation(summary = "Set a new password using the token from the reset email")
  @PostMapping("/reset-password")
  public ApiResponse<Void> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
    authService.resetPassword(request);
    return ApiResponse.ok();
  }
}
