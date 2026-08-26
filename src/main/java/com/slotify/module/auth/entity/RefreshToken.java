package com.slotify.module.auth.entity;

import com.slotify.common.entity.BaseEntity;
import com.slotify.module.user.entity.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Long-lived, single-use refresh token (one row per issued token).
 *
 * <p>Only the SHA-256 hash is stored; the raw value is returned to the client once. Tokens are
 * rotated on every refresh and revoked on logout.
 */
@Entity
@Table(name = "refresh_tokens")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RefreshToken extends BaseEntity {

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "user_id", nullable = false)
  private User user;

  @Column(name = "token_hash", nullable = false, length = 128)
  private String tokenHash;

  @Column(name = "expires_at", nullable = false)
  private Instant expiresAt;

  @Column(nullable = false)
  private boolean revoked;

  public static RefreshToken of(User user, String tokenHash, Instant expiresAt) {
    RefreshToken token = new RefreshToken();
    token.user = user;
    token.tokenHash = tokenHash;
    token.expiresAt = expiresAt;
    return token;
  }

  /** A token is usable when it is neither revoked nor expired at {@code now}. */
  public boolean isUsable(Instant now) {
    return !revoked && expiresAt.isAfter(now);
  }
}
