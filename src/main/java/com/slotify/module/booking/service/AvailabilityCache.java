package com.slotify.module.booking.service;

import com.slotify.module.booking.dto.AvailabilityResponse;
import java.time.Duration;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

/**
 * Short-lived Redis cache for availability results.
 *
 * <p>Key: {@code avail:<salonId>:<date>:<serviceIds>:<staffId|any>}, TTL 60 s. Any booking, shift
 * or time-off change for that salon/date calls {@link #evict(Long, LocalDate)}. Redis failures
 * degrade to "no cache" so availability keeps working.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AvailabilityCache {

  static final Duration TTL = Duration.ofSeconds(60);
  private static final String PREFIX = "avail:";

  private final StringRedisTemplate redis;
  private final ObjectMapper objectMapper;

  public Optional<AvailabilityResponse> get(String key) {
    try {
      String json = redis.opsForValue().get(key);
      return json == null
          ? Optional.empty()
          : Optional.of(objectMapper.readValue(json, AvailabilityResponse.class));
    } catch (RuntimeException ex) {
      log.debug("Availability cache read failed: {}", ex.getMessage());
      return Optional.empty();
    }
  }

  public void put(String key, AvailabilityResponse response) {
    try {
      redis.opsForValue().set(key, objectMapper.writeValueAsString(response), TTL);
    } catch (RuntimeException ex) {
      log.debug("Availability cache write failed: {}", ex.getMessage());
    }
  }

  /** Removes every cached result of the salon for the given day. */
  public void evict(Long salonId, LocalDate date) {
    try {
      Set<String> keys = redis.keys(PREFIX + salonId + ":" + date + ":*");
      if (keys != null && !keys.isEmpty()) {
        redis.delete(keys);
      }
    } catch (RuntimeException ex) {
      log.debug("Availability cache evict failed: {}", ex.getMessage());
    }
  }

  static String key(Long salonId, LocalDate date, List<Long> serviceIds, Long staffId) {
    return PREFIX
        + salonId
        + ":"
        + date
        + ":"
        + serviceIds.stream().sorted().map(String::valueOf).reduce((a, b) -> a + "," + b).orElse("")
        + ":"
        + (staffId == null ? "any" : staffId);
  }
}
