package com.slotify;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.web.client.RestClient;

/**
 * Boots the full application against a throw-away MySQL container and verifies that Flyway
 * migrations apply and a public endpoint responds with the standard envelope.
 *
 * <p>Requires Docker (Testcontainers).
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(TestcontainersConfiguration.class)
class SlotifyApplicationTests {

  @LocalServerPort private int port;

  @Test
  void contextLoadsAndSystemInfoIsPublic() {
    String body =
        RestClient.create()
            .get()
            .uri("http://localhost:" + port + "/api/v1/system/info")
            .retrieve()
            .body(String.class);

    assertThat(body)
        .contains("\"success\":true")
        .contains("\"code\":1000")
        .contains("\"name\":\"Slotify\"");
  }
}
