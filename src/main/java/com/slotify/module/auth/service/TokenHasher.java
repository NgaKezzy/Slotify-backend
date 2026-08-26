package com.slotify.module.auth.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HexFormat;
import org.springframework.stereotype.Component;

/**
 * Generates opaque one-time tokens (refresh, email verification, password reset) and hashes them
 * for storage. Only the SHA-256 hash is persisted so a database leak does not expose live tokens.
 */
@Component
public class TokenHasher {

  private static final int TOKEN_BYTES = 48;

  private final SecureRandom random = new SecureRandom();

  /** Returns a new URL-safe random token (64 characters). */
  public String generate() {
    byte[] bytes = new byte[TOKEN_BYTES];
    random.nextBytes(bytes);
    return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
  }

  /** SHA-256 hex digest of a token. */
  public String hash(String token) {
    try {
      MessageDigest digest = MessageDigest.getInstance("SHA-256");
      return HexFormat.of().formatHex(digest.digest(token.getBytes(StandardCharsets.UTF_8)));
    } catch (NoSuchAlgorithmException ex) {
      throw new IllegalStateException("SHA-256 is required by the JVM specification", ex);
    }
  }
}
