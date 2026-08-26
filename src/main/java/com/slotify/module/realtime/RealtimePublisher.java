package com.slotify.module.realtime;

import com.slotify.config.WebSocketConfig;
import java.time.Clock;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

/**
 * Publishes real-time events to STOMP destinations (see {@link WebSocketConfig} for the list).
 *
 * <p>Users are addressed by their id: the principal name of the WebSocket session is the {@code
 * UserPrincipal}'s {@code toString()}, so we route by the explicit {@code userId} string instead.
 */
@Component
@RequiredArgsConstructor
public class RealtimePublisher {

  private final SimpMessagingTemplate messagingTemplate;
  private final Clock clock;

  /** Sends to everyone subscribed to a salon's booking feed (admin panel, staff app). */
  public void publishToSalon(Long salonId, String type, Object payload) {
    messagingTemplate.convertAndSend(
        WebSocketConfig.TOPIC_PREFIX + "/salon/" + salonId + "/bookings", event(type, payload));
  }

  /** Sends a dashboard refresh hint to a salon. */
  public void publishDashboard(Long salonId, Object payload) {
    messagingTemplate.convertAndSend(
        WebSocketConfig.TOPIC_PREFIX + "/salon/" + salonId + "/dashboard",
        event("DASHBOARD_UPDATED", payload));
  }

  /** Sends to one user's private queue. */
  public void publishToUser(Long userId, String queue, String type, Object payload) {
    messagingTemplate.convertAndSendToUser(
        String.valueOf(userId), WebSocketConfig.QUEUE_PREFIX + "/" + queue, event(type, payload));
  }

  private RealtimeEvent event(String type, Object payload) {
    return new RealtimeEvent(type, payload, Instant.now(clock));
  }
}
