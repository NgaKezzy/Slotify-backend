package com.slotify.module.auth.service;

import com.slotify.common.exception.AppException;
import com.slotify.common.exception.ErrorCode;
import com.slotify.config.AppProperties;
import com.slotify.module.auth.dto.AuthResponse;
import com.slotify.module.auth.dto.LoginRequest;
import com.slotify.module.auth.dto.RegisterRequest;
import com.slotify.module.auth.dto.ResetPasswordRequest;
import com.slotify.module.auth.entity.EmailVerificationToken;
import com.slotify.module.auth.entity.PasswordResetToken;
import com.slotify.module.auth.repository.EmailVerificationTokenRepository;
import com.slotify.module.auth.repository.PasswordResetTokenRepository;
import com.slotify.module.notification.service.MailService;
import com.slotify.module.user.entity.AuthProvider;
import com.slotify.module.user.entity.Role;
import com.slotify.module.user.entity.User;
import com.slotify.module.user.mapper.UserMapper;
import com.slotify.module.user.repository.UserRepository;
import com.slotify.security.JwtService;
import java.time.Clock;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Registration, login (email/password and Google), token refresh, logout and the email-based
 * verification / password-reset flows.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class AuthService {

  private final UserRepository userRepository;
  private final EmailVerificationTokenRepository emailVerificationTokenRepository;
  private final PasswordResetTokenRepository passwordResetTokenRepository;
  private final PasswordEncoder passwordEncoder;
  private final JwtService jwtService;
  private final RefreshTokenService refreshTokenService;
  private final GoogleTokenVerifier googleTokenVerifier;
  private final TokenHasher tokenHasher;
  private final MailService mailService;
  private final UserMapper userMapper;
  private final AppProperties properties;
  private final Clock clock;

  /** Creates a CUSTOMER account and emails a verification link. */
  public AuthResponse register(RegisterRequest request) {
    if (userRepository.existsByEmailIgnoreCase(request.email())) {
      throw new AppException(ErrorCode.EMAIL_ALREADY_USED);
    }
    User user =
        User.local(
            request.email(),
            passwordEncoder.encode(request.password()),
            request.fullName().trim(),
            Role.CUSTOMER);
    user.setPhone(request.phone());
    userRepository.save(user);

    sendVerificationEmail(user);
    return issueSession(user);
  }

  /** Email + password login. */
  public AuthResponse login(LoginRequest request) {
    User user =
        userRepository
            .findByEmailIgnoreCase(request.email())
            .filter(User::hasPassword)
            .filter(u -> passwordEncoder.matches(request.password(), u.getPasswordHash()))
            .orElseThrow(() -> new AppException(ErrorCode.INVALID_CREDENTIALS));
    assertCanSignIn(user);
    if (properties.auth().requireEmailVerification() && !user.isEmailVerified()) {
      throw new AppException(ErrorCode.EMAIL_NOT_VERIFIED);
    }
    return issueSession(user);
  }

  /** Google Sign-In: links to an existing account by email or creates a new CUSTOMER. */
  public AuthResponse loginWithGoogle(String idToken) {
    GoogleTokenVerifier.GoogleIdentity identity = googleTokenVerifier.verify(idToken);
    User user =
        userRepository
            .findByEmailIgnoreCase(identity.email())
            .map(existing -> linkGoogle(existing, identity))
            .orElseGet(() -> userRepository.save(newGoogleUser(identity)));
    assertCanSignIn(user);
    return issueSession(user);
  }

  /** Exchanges a refresh token for a new access/refresh pair (rotation). */
  public AuthResponse refresh(String refreshToken) {
    RefreshTokenService.Rotation rotation = refreshTokenService.rotate(refreshToken);
    assertCanSignIn(rotation.user());
    return new AuthResponse(
        jwtService.generateAccessToken(rotation.user()),
        rotation.newRawToken(),
        jwtService.accessTokenTtlSeconds(),
        userMapper.toResponse(rotation.user()));
  }

  /** Revokes the given refresh token (logout on this device). */
  public void logout(String refreshToken) {
    refreshTokenService.revoke(refreshToken);
  }

  /** Confirms the email address behind a verification token. */
  public void verifyEmail(String rawToken) {
    EmailVerificationToken token =
        emailVerificationTokenRepository
            .findByTokenHash(tokenHasher.hash(rawToken))
            .orElseThrow(() -> new AppException(ErrorCode.TOKEN_INVALID));
    if (token.getExpiresAt().isBefore(Instant.now(clock))) {
      throw new AppException(ErrorCode.TOKEN_EXPIRED);
    }
    User user = token.getUser();
    user.setEmailVerified(true);
    emailVerificationTokenRepository.deleteAllByUserId(user.getId());
  }

  /**
   * Emails a password reset link when the account exists. Always succeeds from the caller's point
   * of view so that email addresses cannot be enumerated.
   */
  public void forgotPassword(String email) {
    userRepository
        .findByEmailIgnoreCase(email)
        .filter(User::hasPassword)
        .ifPresent(
            user -> {
              String raw = tokenHasher.generate();
              Instant expiresAt = Instant.now(clock).plus(properties.auth().passwordResetTtl());
              passwordResetTokenRepository.save(
                  PasswordResetToken.of(user, tokenHasher.hash(raw), expiresAt));
              mailService.sendPasswordReset(user, raw);
            });
  }

  /** Sets a new password from a reset token and signs the user out everywhere. */
  public void resetPassword(ResetPasswordRequest request) {
    PasswordResetToken token =
        passwordResetTokenRepository
            .findByTokenHash(tokenHasher.hash(request.token()))
            .orElseThrow(() -> new AppException(ErrorCode.TOKEN_INVALID));
    if (!token.isUsable(Instant.now(clock))) {
      throw new AppException(ErrorCode.TOKEN_EXPIRED);
    }
    User user = token.getUser();
    user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
    token.setUsed(true);
    refreshTokenService.revokeAll(user.getId());
  }

  // ---------------------------------------------------------------------------------------------

  private AuthResponse issueSession(User user) {
    return new AuthResponse(
        jwtService.generateAccessToken(user),
        refreshTokenService.issue(user),
        jwtService.accessTokenTtlSeconds(),
        userMapper.toResponse(user));
  }

  private void sendVerificationEmail(User user) {
    String raw = tokenHasher.generate();
    Instant expiresAt = Instant.now(clock).plus(properties.auth().emailVerificationTtl());
    emailVerificationTokenRepository.save(
        EmailVerificationToken.of(user, tokenHasher.hash(raw), expiresAt));
    mailService.sendEmailVerification(user, raw);
  }

  private static void assertCanSignIn(User user) {
    if (!user.isActive()) {
      throw new AppException(ErrorCode.ACCOUNT_SUSPENDED);
    }
  }

  private static User linkGoogle(User existing, GoogleTokenVerifier.GoogleIdentity identity) {
    if (existing.getProviderId() == null) {
      existing.setProviderId(identity.subject());
    }
    if (existing.getProvider() == AuthProvider.LOCAL && !existing.hasPassword()) {
      existing.setProvider(AuthProvider.GOOGLE);
    }
    existing.setEmailVerified(true);
    if (existing.getAvatarUrl() == null) {
      existing.setAvatarUrl(identity.pictureUrl());
    }
    return existing;
  }

  private static User newGoogleUser(GoogleTokenVerifier.GoogleIdentity identity) {
    String name = identity.name() != null ? identity.name() : identity.email();
    return User.google(identity.email(), identity.subject(), name, identity.pictureUrl());
  }
}
