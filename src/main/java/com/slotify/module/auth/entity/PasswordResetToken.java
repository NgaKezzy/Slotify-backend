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

/** One-time token emailed for the "forgot password" flow (hash stored only). */
@Entity
@Table(name = "password_reset_tokens")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PasswordResetToken extends BaseEntity {

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "user_id", nullable = false)
  private User user;

  @Column(name = "token_hash", nullable = false, length = 128)
  private String tokenHash;

  @Column(name = "expires_at", nullable = false)
  private Instant expiresAt;

  @Column(nullable = false)
  private boolean used;

  public static PasswordResetToken of(User user, String tokenHash, Instant expiresAt) {
    PasswordResetToken token = new PasswordResetToken();
    token.user = user;
    token.tokenHash = tokenHash;
    token.expiresAt = expiresAt;
    return token;
  }

  public boolean isUsable(Instant now) {
    return !used && expiresAt.isAfter(now);
  }
}
