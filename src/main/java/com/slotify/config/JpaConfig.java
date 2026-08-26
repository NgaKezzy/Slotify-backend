package com.slotify.config;

import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Persistence-related beans.
 *
 * <p>A single {@link Clock} bean is used for every "now" in the codebase so that tests can freeze
 * time. Always inject {@code Clock} instead of calling {@code Instant.now()} directly.
 */
@Configuration
public class JpaConfig {

  @Bean
  Clock clock() {
    return Clock.systemUTC();
  }
}
