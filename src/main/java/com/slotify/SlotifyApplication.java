package com.slotify;

import com.slotify.config.AppProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Entry point of the Slotify backend.
 *
 * <p>The application is a modular monolith: every business area lives under {@code
 * com.slotify.module.<name>} with its own controller/service/repository/entity/dto packages, while
 * {@code common}, {@code config} and {@code security} hold cross-cutting concerns.
 */
@SpringBootApplication
@EnableJpaAuditing
@EnableScheduling
@EnableConfigurationProperties(AppProperties.class)
public class SlotifyApplication {

  public static void main(String[] args) {
    SpringApplication.run(SlotifyApplication.class, args);
  }
}
