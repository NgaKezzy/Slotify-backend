package com.slotify.module.payment.service;

import com.slotify.common.exception.AppException;
import com.slotify.common.exception.ErrorCode;
import com.slotify.config.AppProperties;
import com.slotify.module.payment.entity.Payment;
import com.slotify.module.payment.entity.PaymentProvider;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import tools.jackson.databind.JsonNode;

/**
 * PayPal implementation through the REST API v2 (no SDK): OAuth client-credentials token, Orders
 * (create → customer approves in the app → capture) and refunds of captures.
 *
 * <p>The mobile app opens {@code approveUrl} in a web view; after approval it calls {@code POST
 * /payments/paypal/capture}. Webhook {@code PAYMENT.CAPTURE.COMPLETED} is the authoritative
 * confirmation.
 */
@Slf4j
@Component
public class PayPalGateway implements PaymentGateway {

  private static final String SANDBOX = "https://api-m.sandbox.paypal.com";
  private static final String LIVE = "https://api-m.paypal.com";

  private final AppProperties.Paypal config;
  private final RestClient rest;
  private final Clock clock;
  private volatile String accessToken;
  private volatile Instant tokenExpiry = Instant.EPOCH;

  public PayPalGateway(AppProperties properties, Clock clock) {
    this.config = properties.paypal();
    this.clock = clock;
    String base = "live".equalsIgnoreCase(config.mode()) ? LIVE : SANDBOX;
    this.rest = RestClient.builder().baseUrl(base).build();
    if (!config.enabled()) {
      log.info("PayPal not configured – PayPal payments disabled");
    }
  }

  @Override
  public PaymentProvider provider() {
    return PaymentProvider.PAYPAL;
  }

  @Override
  public boolean enabled() {
    return config.enabled();
  }

  @Override
  public CreatedPayment create(Payment payment, String customerEmail, String description) {
    requireEnabled();
    Map<String, Object> body =
        Map.of(
            "intent", "CAPTURE",
            "purchase_units",
                List.of(
                    Map.of(
                        "reference_id", String.valueOf(payment.getId()),
                        "custom_id", String.valueOf(payment.getBooking().getId()),
                        "description", description,
                        "amount",
                            Map.of(
                                "currency_code", payment.getCurrency(),
                                "value", toDecimal(payment.getAmountMinor())))),
            "payment_source",
                Map.of(
                    "paypal",
                    Map.of(
                        "experience_context",
                        Map.of("user_action", "PAY_NOW", "shipping_preference", "NO_SHIPPING"))));
    try {
      JsonNode order =
          rest.post()
              .uri("/v2/checkout/orders")
              .header(HttpHeaders.AUTHORIZATION, "Bearer " + token())
              .contentType(MediaType.APPLICATION_JSON)
              .body(body)
              .retrieve()
              .body(JsonNode.class);
      String approveUrl = "";
      for (JsonNode link : order.path("links")) {
        if ("payer-action".equals(link.path("rel").asText())
            || "approve".equals(link.path("rel").asText())) {
          approveUrl = link.path("href").asText();
        }
      }
      return new CreatedPayment(
          order.path("id").asText(),
          Map.of("orderId", order.path("id").asText(), "approveUrl", approveUrl));
    } catch (RestClientException ex) {
      log.error("PayPal order creation failed: {}", ex.getMessage());
      throw new AppException(ErrorCode.PAYMENT_FAILED);
    }
  }

  /**
   * Captures an approved order.
   *
   * @return capture id when completed, {@code null} otherwise
   */
  public String capture(String orderId) {
    requireEnabled();
    try {
      JsonNode result =
          rest.post()
              .uri("/v2/checkout/orders/{id}/capture", orderId)
              .header(HttpHeaders.AUTHORIZATION, "Bearer " + token())
              .contentType(MediaType.APPLICATION_JSON)
              .retrieve()
              .body(JsonNode.class);
      if (!"COMPLETED".equals(result.path("status").asText())) {
        return null;
      }
      return result
          .path("purchase_units")
          .path(0)
          .path("payments")
          .path("captures")
          .path(0)
          .path("id")
          .asText();
    } catch (RestClientException ex) {
      log.error("PayPal capture failed for {}: {}", orderId, ex.getMessage());
      throw new AppException(ErrorCode.PAYMENT_FAILED);
    }
  }

  @Override
  public String refund(Payment payment, long amountMinor, String reason) {
    requireEnabled();
    String captureId =
        String.valueOf(payment.getRaw() == null ? null : payment.getRaw().get("captureId"));
    try {
      JsonNode result =
          rest.post()
              .uri("/v2/payments/captures/{id}/refund", captureId)
              .header(HttpHeaders.AUTHORIZATION, "Bearer " + token())
              .contentType(MediaType.APPLICATION_JSON)
              .body(
                  Map.of(
                      "amount",
                      Map.of(
                          "currency_code", payment.getCurrency(), "value", toDecimal(amountMinor)),
                      "note_to_payer",
                      reason == null ? "Refund" : reason))
              .retrieve()
              .body(JsonNode.class);
      return result.path("id").asText();
    } catch (RestClientException ex) {
      log.error("PayPal refund failed for capture {}: {}", captureId, ex.getMessage());
      throw new AppException(ErrorCode.PAYMENT_FAILED);
    }
  }

  /** Verifies a webhook through PayPal's verification endpoint. */
  public boolean verifyWebhook(Map<String, String> headers, JsonNode event) {
    if (config.webhookId() == null || config.webhookId().isBlank()) {
      log.warn("PAYPAL_WEBHOOK_ID not set – accepting webhook without signature verification");
      return true;
    }
    Map<String, Object> body =
        Map.of(
            "auth_algo", headers.getOrDefault("paypal-auth-algo", ""),
            "cert_url", headers.getOrDefault("paypal-cert-url", ""),
            "transmission_id", headers.getOrDefault("paypal-transmission-id", ""),
            "transmission_sig", headers.getOrDefault("paypal-transmission-sig", ""),
            "transmission_time", headers.getOrDefault("paypal-transmission-time", ""),
            "webhook_id", config.webhookId(),
            "webhook_event", event);
    try {
      JsonNode result =
          rest.post()
              .uri("/v1/notifications/verify-webhook-signature")
              .header(HttpHeaders.AUTHORIZATION, "Bearer " + token())
              .contentType(MediaType.APPLICATION_JSON)
              .body(body)
              .retrieve()
              .body(JsonNode.class);
      return "SUCCESS".equals(result.path("verification_status").asText());
    } catch (RestClientException ex) {
      log.error("PayPal webhook verification failed: {}", ex.getMessage());
      return false;
    }
  }

  private synchronized String token() {
    if (accessToken != null && Instant.now(clock).isBefore(tokenExpiry)) {
      return accessToken;
    }
    JsonNode result =
        rest.post()
            .uri("/v1/oauth2/token")
            .headers(h -> h.setBasicAuth(config.clientId(), config.clientSecret()))
            .contentType(MediaType.APPLICATION_FORM_URLENCODED)
            .body("grant_type=client_credentials")
            .retrieve()
            .body(JsonNode.class);
    accessToken = result.path("access_token").asText();
    tokenExpiry =
        Instant.now(clock).plusSeconds(Math.max(60, result.path("expires_in").asLong() - 60));
    return accessToken;
  }

  private void requireEnabled() {
    if (!enabled()) {
      throw new AppException(ErrorCode.PAYMENT_METHOD_NOT_ACCEPTED);
    }
  }

  /** PayPal wants decimal strings ("45.00"); Slotify stores minor units. */
  public static String toDecimal(long minor) {
    return BigDecimal.valueOf(minor, 2).toPlainString();
  }
}
