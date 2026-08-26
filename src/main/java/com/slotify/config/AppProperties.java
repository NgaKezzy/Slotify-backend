package com.slotify.config;

import jakarta.validation.constraints.NotBlank;
import java.time.Duration;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Type-safe view of the {@code app.*} section of {@code application.yml}.
 *
 * <p>Inject this record wherever application-level settings are needed instead of using
 * {@code @Value} so that every setting is documented and validated in one place.
 *
 * @param name display name of the platform (used in emails and API docs)
 * @param baseUrl public URL of this API, used to build links in emails
 * @param seedDemoData when true the {@code DataSeeder} inserts demo data on startup
 * @param cors CORS settings for browser clients (admin panel)
 * @param jwt JSON Web Token settings
 * @param mail outgoing email settings
 */
@Validated
@ConfigurationProperties(prefix = "app")
public record AppProperties(
    @NotBlank String name,
    @NotBlank String baseUrl,
    boolean seedDemoData,
    Cors cors,
    Jwt jwt,
    Mail mail) {

  /**
   * CORS configuration.
   *
   * @param allowedOrigins origins allowed to call the API from a browser
   */
  public record Cors(List<String> allowedOrigins) {}

  /**
   * JWT configuration.
   *
   * @param secret HMAC signing secret (at least 64 characters recommended)
   * @param accessTokenTtl lifetime of access tokens (e.g. {@code 15m})
   * @param refreshTokenTtl lifetime of refresh tokens (e.g. {@code 30d})
   */
  public record Jwt(@NotBlank String secret, Duration accessTokenTtl, Duration refreshTokenTtl) {}

  /**
   * Email configuration.
   *
   * @param from sender address used for all outgoing mail
   */
  public record Mail(@NotBlank String from) {}
}
