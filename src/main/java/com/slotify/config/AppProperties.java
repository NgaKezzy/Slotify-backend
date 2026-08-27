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
 * @param demo credentials of the seeded platform admin (only used when seeding)
 * @param cors CORS settings for browser clients (admin panel)
 * @param webUrl public URL of the web front-end used for links in emails (verify, reset)
 * @param jwt JSON Web Token settings
 * @param auth authentication rules
 * @param google Google Sign-In settings
 * @param mail outgoing email settings
 * @param storage S3-compatible object storage for images
 * @param firebase Firebase Cloud Messaging settings
 * @param rateLimit request throttling of the auth endpoints
 * @param stripe Stripe keys
 * @param paypal PayPal REST credentials
 * @param booking booking-related timers
 */
@Validated
@ConfigurationProperties(prefix = "app")
public record AppProperties(
    @NotBlank String name,
    @NotBlank String baseUrl,
    boolean seedDemoData,
    Demo demo,
    @NotBlank String webUrl,
    Cors cors,
    Jwt jwt,
    Auth auth,
    Google google,
    Mail mail,
    Storage storage,
    Firebase firebase,
    RateLimit rateLimit,
    Stripe stripe,
    Paypal paypal,
    Booking booking) {

  /**
   * Seeded platform-admin account, overridable per environment so a demo box can use its own
   * login (e.g. {@code admin@admin.com} / {@code admin}).
   *
   * @param adminEmail email of the SUPER_ADMIN demo account
   * @param adminPassword password shared by every seeded demo account
   */
  public record Demo(@NotBlank String adminEmail, @NotBlank String adminPassword) {}

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
   * Authentication rules.
   *
   * @param requireEmailVerification when true, local accounts must verify their email before login
   * @param emailVerificationTtl validity of email verification links
   * @param passwordResetTtl validity of password reset links
   */
  public record Auth(
      boolean requireEmailVerification, Duration emailVerificationTtl, Duration passwordResetTtl) {}

  /**
   * Google Sign-In configuration.
   *
   * @param clientId OAuth client id(s) accepted as audience of Google ID tokens (comma separated in
   *     the environment); empty disables Google login
   */
  public record Google(List<String> clientId) {}

  /**
   * Object storage configuration (AWS S3 or MinIO).
   *
   * @param endpoint custom endpoint (MinIO); blank for AWS
   * @param region bucket region
   * @param bucket bucket name
   * @param accessKey access key id
   * @param secretKey secret access key
   * @param publicBaseUrl base URL under which objects are publicly readable
   * @param presignTtl validity of pre-signed upload URLs
   */
  public record Storage(
      String endpoint,
      @NotBlank String region,
      @NotBlank String bucket,
      String accessKey,
      String secretKey,
      @NotBlank String publicBaseUrl,
      Duration presignTtl) {}

  /**
   * Firebase configuration.
   *
   * @param credentialsPath path to the service-account JSON; blank disables push notifications
   */
  public record Firebase(String credentialsPath) {}

  /**
   * Stripe configuration (test or live keys).
   *
   * @param secretKey secret API key ({@code sk_test_...}); blank disables Stripe
   * @param webhookSecret signing secret of the webhook endpoint ({@code whsec_...})
   * @param publishableKey publishable key handed to the mobile apps
   */
  public record Stripe(String secretKey, String webhookSecret, String publishableKey) {

    public boolean enabled() {
      return secretKey != null && !secretKey.isBlank();
    }
  }

  /**
   * PayPal configuration.
   *
   * @param clientId REST app client id; blank disables PayPal
   * @param clientSecret REST app secret
   * @param mode {@code sandbox} or {@code live}
   * @param webhookId id of the webhook registered in the PayPal dashboard (signature check)
   */
  public record Paypal(String clientId, String clientSecret, String mode, String webhookId) {

    public boolean enabled() {
      return clientId != null && !clientId.isBlank();
    }
  }

  /**
   * Booking timers.
   *
   * @param unpaidTimeout how long an online-payment booking may stay unpaid before auto-cancel
   */
  public record Booking(Duration unpaidTimeout) {}

  /**
   * Rate limiting configuration.
   *
   * @param enabled disable only for automated tests
   */
  public record RateLimit(boolean enabled) {}

  /**
   * Email configuration.
   *
   * @param from sender address used for all outgoing mail
   */
  public record Mail(@NotBlank String from) {}
}
