package com.slotify.config;

import com.slotify.security.JwtService;
import com.slotify.security.UserPrincipal;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

/**
 * STOMP over WebSocket for real-time updates.
 *
 * <p>Endpoint {@code /ws} (raw WebSocket; SockJS fallback on {@code /ws-sockjs} for old browsers).
 * Clients authenticate by sending {@code Authorization: Bearer <jwt>} as a STOMP header on CONNECT.
 * Destinations:
 *
 * <ul>
 *   <li>{@code /topic/salon/{salonId}/bookings} – booking events for admin panel & staff app
 *   <li>{@code /topic/salon/{salonId}/dashboard} – live KPI updates
 *   <li>{@code /user/queue/bookings} – booking events for the customer who owns them
 *   <li>{@code /user/queue/notifications} – new in-app notifications
 * </ul>
 */
@Configuration
@EnableWebSocketMessageBroker
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

  public static final String TOPIC_PREFIX = "/topic";
  public static final String QUEUE_PREFIX = "/queue";
  public static final String USER_PREFIX = "/user";

  private final JwtService jwtService;
  private final AppProperties properties;

  @Override
  public void registerStompEndpoints(StompEndpointRegistry registry) {
    String[] origins = properties.cors().allowedOrigins().toArray(String[]::new);
    registry.addEndpoint("/ws").setAllowedOriginPatterns(origins);
    registry.addEndpoint("/ws-sockjs").setAllowedOriginPatterns(origins).withSockJS();
  }

  @Override
  public void configureMessageBroker(MessageBrokerRegistry registry) {
    registry.enableSimpleBroker(TOPIC_PREFIX, QUEUE_PREFIX);
    registry.setApplicationDestinationPrefixes("/app");
    registry.setUserDestinationPrefix(USER_PREFIX);
  }

  @Override
  public void configureClientInboundChannel(ChannelRegistration registration) {
    registration.interceptors(new JwtChannelInterceptor(jwtService));
  }

  /** Authenticates the CONNECT frame with the bearer token and attaches the principal. */
  @RequiredArgsConstructor
  static class JwtChannelInterceptor implements ChannelInterceptor {

    private static final String BEARER_PREFIX = "Bearer ";
    private final JwtService jwtService;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
      StompHeaderAccessor accessor =
          MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
      if (accessor != null && StompCommand.CONNECT.equals(accessor.getCommand())) {
        List<String> auth = accessor.getNativeHeader("Authorization");
        if (auth != null && !auth.isEmpty() && auth.getFirst().startsWith(BEARER_PREFIX)) {
          UserPrincipal principal =
              jwtService.parse(auth.getFirst().substring(BEARER_PREFIX.length()));
          accessor.setUser(
              new UsernamePasswordAuthenticationToken(principal, null, principal.authorities()));
        }
      }
      return message;
    }
  }
}
