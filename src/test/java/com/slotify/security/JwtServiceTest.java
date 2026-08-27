package com.slotify.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.slotify.common.exception.AppException;
import com.slotify.common.exception.ErrorCode;
import com.slotify.config.AppProperties;
import com.slotify.module.user.entity.Role;
import com.slotify.module.user.entity.User;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

/** Unit tests for {@link JwtService}: round trip, expiry and tampering. */
class JwtServiceTest {

  private static final Instant NOW = Instant.parse("2026-01-01T10:00:00Z");
  private static final String SECRET =
      "unit-test-secret-that-is-long-enough-for-hmac-sha256-signing-0123456789";

  private static AppProperties properties() {
    return new AppProperties(
        "Slotify",
        "http://localhost:8080",
        false,
        new AppProperties.Demo("admin@slotify.demo", "Password123!"),
        "http://localhost:3000",
        new AppProperties.Cors(List.of()),
        new AppProperties.Jwt(SECRET, Duration.ofMinutes(15), Duration.ofDays(30)),
        new AppProperties.Auth(false, Duration.ofHours(24), Duration.ofHours(1)),
        new AppProperties.Google(List.of()),
        new AppProperties.Mail("no-reply@slotify.local"),
        new AppProperties.Storage(
            "",
            "eu-central-1",
            "slotify",
            "",
            "",
            "http://localhost/slotify",
            Duration.ofMinutes(10)),
        new AppProperties.Firebase(""),
        new AppProperties.RateLimit(true),
        new AppProperties.Stripe("", "", ""),
        new AppProperties.Paypal("", "", "sandbox", ""),
        new AppProperties.Booking(Duration.ofMinutes(15)));
  }

  private static User user() {
    User user = User.local("jane@example.com", "hash", "Jane", Role.SALON_OWNER);
    ReflectionTestUtils.setField(user, "id", 42L);
    return user;
  }

  @Test
  void generatesTokenThatParsesBackToPrincipal() {
    JwtService service = new JwtService(properties(), Clock.fixed(NOW, ZoneOffset.UTC));

    UserPrincipal principal = service.parse(service.generateAccessToken(user()));

    assertThat(principal.id()).isEqualTo(42L);
    assertThat(principal.email()).isEqualTo("jane@example.com");
    assertThat(principal.role()).isEqualTo(Role.SALON_OWNER);
    assertThat(principal.authorities()).extracting("authority").containsExactly("ROLE_SALON_OWNER");
  }

  @Test
  void rejectsExpiredToken() {
    String token =
        new JwtService(properties(), Clock.fixed(NOW, ZoneOffset.UTC)).generateAccessToken(user());
    JwtService later =
        new JwtService(properties(), Clock.fixed(NOW.plus(Duration.ofMinutes(16)), ZoneOffset.UTC));

    assertThatThrownBy(() -> later.parse(token))
        .isInstanceOf(AppException.class)
        .extracting("errorCode")
        .isEqualTo(ErrorCode.TOKEN_EXPIRED);
  }

  @Test
  void rejectsTamperedToken() {
    JwtService service = new JwtService(properties(), Clock.fixed(NOW, ZoneOffset.UTC));
    String token = service.generateAccessToken(user());
    String tampered = token.substring(0, token.length() - 4) + "abcd";

    assertThatThrownBy(() -> service.parse(tampered))
        .isInstanceOf(AppException.class)
        .extracting("errorCode")
        .isEqualTo(ErrorCode.TOKEN_INVALID);
  }
}
