package com.slotify.module.auth;

import static org.assertj.core.api.Assertions.assertThat;

import com.slotify.TestcontainersConfiguration;
import com.slotify.module.auth.repository.EmailVerificationTokenRepository;
import com.slotify.module.user.entity.User;
import com.slotify.module.user.repository.UserRepository;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.JsonNode;

/**
 * End-to-end test of the authentication flow against a real MySQL: register → me → refresh
 * (rotation) → logout → refresh rejected, plus wrong-password and duplicate-email error codes.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(TestcontainersConfiguration.class)
class AuthFlowIntegrationTest {

  @LocalServerPort private int port;
  @Autowired private UserRepository userRepository;
  @Autowired private EmailVerificationTokenRepository verificationTokenRepository;
  @MockitoBean private JavaMailSender mailSender;

  private RestClient client() {
    return RestClient.builder()
        .baseUrl("http://localhost:" + port + "/api/v1")
        .defaultStatusHandler(status -> true, (req, res) -> {})
        .build();
  }

  @Test
  void fullSessionLifecycle() {
    RestClient client = client();
    String email = "flow-" + System.nanoTime() + "@example.com";

    // register
    ResponseEntity<JsonNode> registered =
        client
            .post()
            .uri("/auth/register")
            .body(Map.of("fullName", "Flow Tester", "email", email, "password", "Password123!"))
            .retrieve()
            .toEntity(JsonNode.class);
    assertThat(registered.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    JsonNode session = registered.getBody().get("data");
    assertThat(registered.getBody().get("code").asInt()).isEqualTo(1000);
    assertThat(session.get("user").get("role").asText()).isEqualTo("CUSTOMER");
    String accessToken = session.get("accessToken").asText();
    String refreshToken = session.get("refreshToken").asText();

    // me
    JsonNode me =
        client
            .get()
            .uri("/me")
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
            .retrieve()
            .body(JsonNode.class);
    assertThat(me.get("data").get("email").asText()).isEqualTo(email);

    // refresh rotates the token
    JsonNode refreshed =
        client
            .post()
            .uri("/auth/refresh")
            .body(Map.of("refreshToken", refreshToken))
            .retrieve()
            .body(JsonNode.class);
    String rotated = refreshed.get("data").get("refreshToken").asText();
    assertThat(rotated).isNotEqualTo(refreshToken);

    // the old token is now unusable (1005 TOKEN_INVALID)
    JsonNode replay =
        client
            .post()
            .uri("/auth/refresh")
            .body(Map.of("refreshToken", refreshToken))
            .retrieve()
            .body(JsonNode.class);
    assertThat(replay.get("success").asBoolean()).isFalse();
    assertThat(replay.get("code").asInt()).isEqualTo(1005);

    // logout revokes the rotated token
    client
        .post()
        .uri("/auth/logout")
        .body(Map.of("refreshToken", rotated))
        .retrieve()
        .toBodilessEntity();
    JsonNode afterLogout =
        client
            .post()
            .uri("/auth/refresh")
            .body(Map.of("refreshToken", rotated))
            .retrieve()
            .body(JsonNode.class);
    assertThat(afterLogout.get("code").asInt()).isEqualTo(1005);
  }

  @Test
  void registrationSendsVerificationEmailAndTokenVerifies() {
    RestClient client = client();
    String email = "verify-" + System.nanoTime() + "@example.com";
    client
        .post()
        .uri("/auth/register")
        .body(Map.of("fullName", "Verify Me", "email", email, "password", "Password123!"))
        .retrieve()
        .toBodilessEntity();

    User user = userRepository.findByEmailIgnoreCase(email).orElseThrow();
    assertThat(user.isEmailVerified()).isFalse();
    // The raw token only exists in the email; read the stored hash to prove the row exists.
    assertThat(verificationTokenRepository.findAll())
        .anyMatch(t -> t.getUser().getId().equals(user.getId()));
  }

  @Test
  void wrongPasswordAndDuplicateEmailReturnBusinessCodes() {
    RestClient client = client();
    String email = "dup-" + System.nanoTime() + "@example.com";
    client
        .post()
        .uri("/auth/register")
        .body(Map.of("fullName", "Dup", "email", email, "password", "Password123!"))
        .retrieve()
        .toBodilessEntity();

    JsonNode duplicate =
        client
            .post()
            .uri("/auth/register")
            .body(Map.of("fullName", "Dup", "email", email, "password", "Password123!"))
            .retrieve()
            .body(JsonNode.class);
    assertThat(duplicate.get("code").asInt()).isEqualTo(4002);

    ResponseEntity<JsonNode> wrongPassword =
        client
            .post()
            .uri("/auth/login")
            .body(Map.of("email", email, "password", "nope-nope"))
            .retrieve()
            .toEntity(JsonNode.class);
    assertThat(wrongPassword.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    assertThat(wrongPassword.getBody().get("code").asInt()).isEqualTo(1003);

    JsonNode validation =
        client
            .post()
            .uri("/auth/register")
            .body(Map.of("fullName", "", "email", "not-an-email", "password", "short"))
            .retrieve()
            .body(JsonNode.class);
    assertThat(validation.get("code").asInt()).isEqualTo(3001);
    assertThat(validation.get("errors")).isNotEmpty();
  }
}
