package com.slotify.module.auth.service;

import com.slotify.common.exception.AppException;
import com.slotify.common.exception.ErrorCode;
import com.slotify.config.AppProperties;
import com.slotify.module.auth.entity.RefreshToken;
import com.slotify.module.auth.repository.RefreshTokenRepository;
import com.slotify.module.user.entity.User;
import java.time.Clock;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Issues, rotates and revokes opaque refresh tokens.
 *
 * <p>Rotation: every {@link #rotate(String)} revokes the presented token and returns a fresh one,
 * so a stolen token can be used at most once. Reuse of a revoked token revokes the whole user
 * session family as a precaution.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class RefreshTokenService {

  private final RefreshTokenRepository repository;
  private final TokenHasher hasher;
  private final AppProperties properties;
  private final Clock clock;

  /** Creates and stores a new refresh token for the user; returns the raw value. */
  public String issue(User user) {
    String raw = hasher.generate();
    Instant expiresAt = Instant.now(clock).plus(properties.jwt().refreshTokenTtl());
    repository.save(RefreshToken.of(user, hasher.hash(raw), expiresAt));
    return raw;
  }

  /**
   * Validates a raw token, revokes it and returns the owning user together with a new raw token.
   *
   * @throws AppException {@link ErrorCode#TOKEN_INVALID} or {@link ErrorCode#TOKEN_EXPIRED}
   */
  public Rotation rotate(String rawToken) {
    RefreshToken token =
        repository
            .findByTokenHash(hasher.hash(rawToken))
            .orElseThrow(() -> new AppException(ErrorCode.TOKEN_INVALID));
    Instant now = Instant.now(clock);

    if (token.isRevoked()) {
      // Replay of an already rotated token: assume theft and invalidate every session.
      log.warn("Refresh token reuse detected for user {}", token.getUser().getId());
      repository.revokeAllByUserId(token.getUser().getId());
      throw new AppException(ErrorCode.TOKEN_INVALID);
    }
    if (!token.isUsable(now)) {
      throw new AppException(ErrorCode.TOKEN_EXPIRED);
    }

    token.setRevoked(true);
    return new Rotation(token.getUser(), issue(token.getUser()));
  }

  /** Revokes a single token (logout on one device). Unknown tokens are ignored. */
  public void revoke(String rawToken) {
    repository.findByTokenHash(hasher.hash(rawToken)).ifPresent(t -> t.setRevoked(true));
  }

  /** Revokes every token of a user (password change, account suspension). */
  public void revokeAll(Long userId) {
    repository.revokeAllByUserId(userId);
  }

  /** Nightly cleanup of expired and revoked rows. */
  @Scheduled(cron = "0 30 3 * * *")
  public void purgeExpired() {
    int removed = repository.deleteExpiredOrRevoked(Instant.now(clock));
    log.info("Purged {} expired/revoked refresh tokens", removed);
  }

  /**
   * Result of a rotation.
   *
   * @param user owner of the token
   * @param newRawToken freshly issued raw refresh token
   */
  public record Rotation(User user, String newRawToken) {}
}
