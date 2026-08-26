package com.slotify.security;

import com.slotify.common.api.ApiResponse;
import com.slotify.common.exception.ErrorCode;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import lombok.RequiredArgsConstructor;
import org.springframework.context.MessageSource;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import tools.jackson.databind.ObjectMapper;

/**
 * Per-client-IP rate limiting for the authentication endpoints (Bucket4j token buckets).
 *
 * <p>Limits: login / register / google 10 per minute, forgot-password 3 per hour, other {@code
 * /auth/*} endpoints 30 per minute. Buckets live in memory, which is sufficient for a single
 * instance; switch to the Bucket4j Redis extension when running several API instances.
 */
@Component
@RequiredArgsConstructor
public class RateLimitFilter extends OncePerRequestFilter {

  private static final String AUTH_PREFIX = "/api/v1/auth/";

  private static final Map<String, Bandwidth> LIMITS =
      Map.of(
          "login", Bandwidth.builder().capacity(10).refillGreedy(10, Duration.ofMinutes(1)).build(),
          "register",
              Bandwidth.builder().capacity(10).refillGreedy(10, Duration.ofMinutes(1)).build(),
          "google",
              Bandwidth.builder().capacity(10).refillGreedy(10, Duration.ofMinutes(1)).build(),
          "forgot-password",
              Bandwidth.builder().capacity(3).refillGreedy(3, Duration.ofHours(1)).build());

  private static final Bandwidth DEFAULT_LIMIT =
      Bandwidth.builder().capacity(30).refillGreedy(30, Duration.ofMinutes(1)).build();

  private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();
  private final ObjectMapper objectMapper;
  private final MessageSource messageSource;

  @Override
  protected boolean shouldNotFilter(HttpServletRequest request) {
    return !request.getRequestURI().startsWith(AUTH_PREFIX);
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain chain)
      throws ServletException, IOException {
    String action = request.getRequestURI().substring(AUTH_PREFIX.length());
    String bucketKey = clientIp(request) + ":" + action;
    Bucket bucket =
        buckets.computeIfAbsent(
            bucketKey,
            k -> Bucket.builder().addLimit(LIMITS.getOrDefault(action, DEFAULT_LIMIT)).build());

    if (bucket.tryConsume(1)) {
      chain.doFilter(request, response);
      return;
    }
    reject(request, response);
  }

  private void reject(HttpServletRequest request, HttpServletResponse response) throws IOException {
    ErrorCode code = ErrorCode.TOO_MANY_REQUESTS;
    String message =
        messageSource.getMessage(
            code.messageKey(), null, code.defaultMessage(), request.getLocale());
    response.setStatus(code.status().value());
    response.setContentType(MediaType.APPLICATION_JSON_VALUE);
    response.setCharacterEncoding("UTF-8");
    response.getWriter().write(objectMapper.writeValueAsString(ApiResponse.error(code, message)));
  }

  /** Honours {@code X-Forwarded-For} so limits work behind a reverse proxy. */
  private static String clientIp(HttpServletRequest request) {
    String forwarded = request.getHeader("X-Forwarded-For");
    if (forwarded != null && !forwarded.isBlank()) {
      return forwarded.split(",")[0].trim();
    }
    return request.getRemoteAddr();
  }
}
