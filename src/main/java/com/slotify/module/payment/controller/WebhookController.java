package com.slotify.module.payment.controller;

import com.slotify.module.payment.service.PayPalGateway;
import com.slotify.module.payment.service.PaymentService;
import com.slotify.module.payment.service.StripeGateway;
import com.stripe.model.Event;
import com.stripe.model.PaymentIntent;
import io.swagger.v3.oas.annotations.Hidden;
import java.util.HashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

/**
 * Provider webhooks (public, verified by signature). Always answer 2xx once the event was
 * understood so the provider stops retrying; unknown event types are ignored.
 *
 * <p>Local testing: {@code stripe listen --forward-to localhost:8080/api/v1/webhooks/stripe}.
 */
@Hidden
@Slf4j
@RestController
@RequestMapping("/api/v1/webhooks")
@RequiredArgsConstructor
public class WebhookController {

  private final StripeGateway stripeGateway;
  private final PayPalGateway payPalGateway;
  private final PaymentService paymentService;
  private final ObjectMapper objectMapper;

  @PostMapping("/stripe")
  public ResponseEntity<Void> stripe(
      @RequestBody String payload, @RequestHeader("Stripe-Signature") String signature) {
    Event event = stripeGateway.parseWebhook(payload, signature);
    PaymentIntent intent =
        event
            .getDataObjectDeserializer()
            .getObject()
            .filter(PaymentIntent.class::isInstance)
            .map(PaymentIntent.class::cast)
            .orElse(null);
    if (intent == null) {
      return ResponseEntity.ok().build();
    }
    Map<String, Object> raw = Map.of("eventId", event.getId(), "eventType", event.getType());
    switch (event.getType()) {
      case "payment_intent.succeeded" -> paymentService.handleSucceeded(intent.getId(), raw);
      case "payment_intent.payment_failed", "payment_intent.canceled" ->
          paymentService.handleFailed(intent.getId(), raw);
      default -> log.debug("Ignoring Stripe event {}", event.getType());
    }
    return ResponseEntity.ok().build();
  }

  @PostMapping("/paypal")
  public ResponseEntity<Void> paypal(
      @RequestBody JsonNode event, @RequestHeader HttpHeaders headers) {
    Map<String, String> flat = new HashMap<>();
    headers.forEach((k, v) -> flat.put(k.toLowerCase(), v.isEmpty() ? "" : v.getFirst()));
    if (!payPalGateway.verifyWebhook(flat, event)) {
      return ResponseEntity.badRequest().build();
    }
    String type = event.path("event_type").asText();
    JsonNode resource = event.path("resource");
    Map<String, Object> raw = Map.of("eventId", event.path("id").asText(), "eventType", type);
    switch (type) {
      case "PAYMENT.CAPTURE.COMPLETED" -> {
        String orderId =
            resource.path("supplementary_data").path("related_ids").path("order_id").asText();
        Map<String, Object> withCapture = new HashMap<>(raw);
        withCapture.put("captureId", resource.path("id").asText());
        paymentService.handleSucceeded(orderId, withCapture);
      }
      case "PAYMENT.CAPTURE.DENIED", "CHECKOUT.ORDER.VOIDED" ->
          paymentService.handleFailed(
              resource.path("supplementary_data").path("related_ids").path("order_id").asText(),
              raw);
      default -> log.debug("Ignoring PayPal event {}", type);
    }
    return ResponseEntity.ok().build();
  }
}
