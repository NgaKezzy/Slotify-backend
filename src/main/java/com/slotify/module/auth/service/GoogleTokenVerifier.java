package com.slotify.module.auth.service;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.slotify.common.exception.AppException;
import com.slotify.common.exception.ErrorCode;
import com.slotify.config.AppProperties;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Verifies Google ID tokens sent by the mobile apps ({@code google_sign_in}) and the admin panel.
 *
 * <p>The token signature, expiry and audience (must be one of {@code app.google.client-id}) are
 * checked with Google's public keys; nothing is stored. Disabled when no client id is configured.
 */
@Slf4j
@Component
public class GoogleTokenVerifier {

  private final GoogleIdTokenVerifier verifier;
  private final boolean enabled;

  public GoogleTokenVerifier(AppProperties properties) {
    List<String> clientIds =
        properties.google() == null || properties.google().clientId() == null
            ? List.of()
            : properties.google().clientId().stream().filter(id -> !id.isBlank()).toList();
    this.enabled = !clientIds.isEmpty();
    this.verifier =
        new GoogleIdTokenVerifier.Builder(new NetHttpTransport(), GsonFactory.getDefaultInstance())
            .setAudience(clientIds)
            .build();
  }

  /**
   * Verifies the token and extracts the identity.
   *
   * @throws AppException {@link ErrorCode#TOKEN_INVALID} when disabled or the token is rejected
   */
  public GoogleIdentity verify(String idToken) {
    if (!enabled) {
      log.warn("Google Sign-In attempted but GOOGLE_CLIENT_ID is not configured");
      throw new AppException(ErrorCode.TOKEN_INVALID);
    }
    try {
      GoogleIdToken token = verifier.verify(idToken);
      if (token == null) {
        throw new AppException(ErrorCode.TOKEN_INVALID);
      }
      GoogleIdToken.Payload payload = token.getPayload();
      if (!Boolean.TRUE.equals(payload.getEmailVerified())) {
        throw new AppException(ErrorCode.EMAIL_NOT_VERIFIED);
      }
      return new GoogleIdentity(
          payload.getSubject(),
          payload.getEmail(),
          (String) payload.get("name"),
          (String) payload.get("picture"));
    } catch (GeneralSecurityException | IOException ex) {
      log.warn("Google ID token verification failed: {}", ex.getMessage());
      throw new AppException(ErrorCode.TOKEN_INVALID);
    }
  }

  /**
   * Identity claims of a verified Google account.
   *
   * @param subject stable Google user id
   * @param email verified email address
   * @param name display name (may be null)
   * @param pictureUrl avatar URL (may be null)
   */
  public record GoogleIdentity(String subject, String email, String name, String pictureUrl) {}
}
