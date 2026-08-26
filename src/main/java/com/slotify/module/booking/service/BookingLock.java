package com.slotify.module.booking.service;

import com.slotify.common.exception.AppException;
import com.slotify.common.exception.ErrorCode;
import java.time.Duration;
import java.time.LocalDate;
import java.util.UUID;
import java.util.function.Supplier;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

/**
 * Short Redis lock per staff member and day that serialises concurrent booking attempts before the
 * transactional double-booking check runs ({@code SET NX} with a 5 s TTL).
 *
 * <p>The row lock in {@code BookingRepository.findOverlappingForUpdate} is the real guarantee; this
 * lock only avoids deadlocks/retries under load. If Redis is unavailable the lock is skipped.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class BookingLock {

  private static final Duration TTL = Duration.ofSeconds(5);
  private static final int ATTEMPTS = 20;
  private static final long RETRY_DELAY_MS = 100;

  private final StringRedisTemplate redis;

  /** Runs {@code action} while holding the lock for the staff/day. */
  public <T> T withLock(Long staffId, LocalDate date, Supplier<T> action) {
    String key = "lock:staff:" + staffId + ":" + date;
    String token = UUID.randomUUID().toString();
    boolean locked = acquire(key, token);
    try {
      return action.get();
    } finally {
      if (locked) {
        release(key, token);
      }
    }
  }

  private boolean acquire(String key, String token) {
    try {
      for (int i = 0; i < ATTEMPTS; i++) {
        if (Boolean.TRUE.equals(redis.opsForValue().setIfAbsent(key, token, TTL))) {
          return true;
        }
        Thread.sleep(RETRY_DELAY_MS);
      }
      throw new AppException(ErrorCode.SLOT_UNAVAILABLE);
    } catch (InterruptedException ex) {
      Thread.currentThread().interrupt();
      throw new AppException(ErrorCode.SLOT_UNAVAILABLE);
    } catch (AppException ex) {
      throw ex;
    } catch (RuntimeException ex) {
      log.warn("Redis unavailable, booking lock skipped: {}", ex.getMessage());
      return false;
    }
  }

  private void release(String key, String token) {
    try {
      if (token.equals(redis.opsForValue().get(key))) {
        redis.delete(key);
      }
    } catch (RuntimeException ex) {
      log.debug("Booking lock release failed: {}", ex.getMessage());
    }
  }
}
