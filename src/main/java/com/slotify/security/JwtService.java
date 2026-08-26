package com.slotify.security;

import com.slotify.common.exception.AppException;
import com.slotify.common.exception.ErrorCode;
import com.slotify.config.AppProperties;
import com.slotify.module.user.entity.Role;
import com.slotify.module.user.entity.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.util.Date;
import javax.crypto.SecretKey;
import org.springframework.stereotype.Component;

/**
 * Issues and validates short-lived JWT access tokens (HMAC-SHA signed).
 *
 * <p>Claims: {@code sub} = user id, {@code email}, {@code role}. Refresh tokens are <em>not</em>
 * JWTs; they are opaque random strings handled by {@code RefreshTokenService}.
 */
@Component
public class JwtService {

  static final String CLAIM_EMAIL = "email";
  static final String CLAIM_ROLE = "role";

  private final SecretKey key;
  private final long accessTokenTtlSeconds;
  private final Clock clock;

  public JwtService(AppProperties properties, Clock clock) {
    this.key = Keys.hmacShaKeyFor(properties.jwt().secret().getBytes(StandardCharsets.UTF_8));
    this.accessTokenTtlSeconds = properties.jwt().accessTokenTtl().toSeconds();
    this.clock = clock;
  }

  /** Lifetime of access tokens in seconds (sent to clients as {@code expiresIn}). */
  public long accessTokenTtlSeconds() {
    return accessTokenTtlSeconds;
  }

  /** Creates a signed access token for the user. */
  public String generateAccessToken(User user) {
    Instant now = Instant.now(clock);
    return Jwts.builder()
        .subject(String.valueOf(user.getId()))
        .claim(CLAIM_EMAIL, user.getEmail())
        .claim(CLAIM_ROLE, user.getRole().name())
        .issuedAt(Date.from(now))
        .expiration(Date.from(now.plusSeconds(accessTokenTtlSeconds)))
        .signWith(key)
        .compact();
  }

  /**
   * Validates a token and extracts the principal.
   *
   * @throws AppException {@link ErrorCode#TOKEN_EXPIRED} or {@link ErrorCode#TOKEN_INVALID}
   */
  public UserPrincipal parse(String token) {
    try {
      Claims claims =
          Jwts.parser()
              .verifyWith(key)
              .clock(() -> Date.from(Instant.now(clock)))
              .build()
              .parseSignedClaims(token)
              .getPayload();
      return new UserPrincipal(
          Long.valueOf(claims.getSubject()),
          claims.get(CLAIM_EMAIL, String.class),
          Role.valueOf(claims.get(CLAIM_ROLE, String.class)));
    } catch (ExpiredJwtException ex) {
      throw new AppException(ErrorCode.TOKEN_EXPIRED);
    } catch (JwtException | IllegalArgumentException ex) {
      throw new AppException(ErrorCode.TOKEN_INVALID);
    }
  }
}
