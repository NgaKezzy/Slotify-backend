package com.slotify.module.audit;

import static org.assertj.core.api.Assertions.assertThat;

import com.slotify.TestcontainersConfiguration;
import com.slotify.module.audit.entity.AuditAction;
import com.slotify.module.salon.entity.Salon;
import com.slotify.module.salon.entity.SalonStatus;
import com.slotify.module.salon.repository.SalonRepository;
import com.slotify.module.user.entity.Role;
import com.slotify.module.user.entity.User;
import com.slotify.module.user.repository.UserRepository;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.JsonNode;

/**
 * A settings update leaves an audit row that the owner (and the platform admin) can read, while
 * another owner is refused.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(TestcontainersConfiguration.class)
class AuditLogIntegrationTest {

  private static final String PASSWORD = "Password123!";

  @LocalServerPort private int port;
  @Autowired private UserRepository userRepository;
  @Autowired private SalonRepository salonRepository;
  @Autowired private PasswordEncoder passwordEncoder;
  @MockitoBean private JavaMailSender mailSender;

  @Test
  void settingsUpdateIsAuditedAndScopedToTheOwner() {
    long suffix = System.nanoTime();
    User owner = user("audit-owner-" + suffix, Role.SALON_OWNER);
    User other = user("audit-other-" + suffix, Role.SALON_OWNER);
    User admin = user("audit-admin-" + suffix, Role.SUPER_ADMIN);
    Salon salon = salon(owner, suffix);
    String ownerToken = login(owner.getEmail());

    JsonNode updated =
        exchange(
            HttpMethod.PUT,
            "/admin/salons/" + salon.getId() + "/settings",
            ownerToken,
            settings(30));
    assertThat(updated.get("code").asInt()).isEqualTo(1000);

    JsonNode logs =
        exchange(
            HttpMethod.GET,
            "/admin/salons/" + salon.getId() + "/audit-logs?action=SALON_SETTINGS_UPDATED",
            ownerToken,
            null);
    assertThat(logs.get("code").asInt()).isEqualTo(1000);
    JsonNode items = logs.get("data").get("items");
    assertThat(items).hasSize(1);
    JsonNode entry = items.get(0);
    assertThat(entry.get("action").asText()).isEqualTo(AuditAction.SALON_SETTINGS_UPDATED.name());
    assertThat(entry.get("entity").asText()).isEqualTo("SalonSettings");
    assertThat(entry.get("actorId").asLong()).isEqualTo(owner.getId());
    assertThat(entry.get("salonId").asLong()).isEqualTo(salon.getId());
    assertThat(entry.get("diff").get("slotIntervalMin").get("from").asInt()).isEqualTo(15);
    assertThat(entry.get("diff").get("slotIntervalMin").get("to").asInt()).isEqualTo(30);
    assertThat(entry.get("diff").has("autoConfirm")).as("unchanged fields are omitted").isFalse();

    JsonNode forbidden =
        exchange(
            HttpMethod.GET,
            "/admin/salons/" + salon.getId() + "/audit-logs",
            login(other.getEmail()),
            null);
    assertThat(forbidden.get("code").asInt()).isEqualTo(1002);

    JsonNode ownerOnPlatform =
        exchange(HttpMethod.GET, "/admin/platform/audit-logs", ownerToken, null);
    assertThat(ownerOnPlatform.get("code").asInt()).isEqualTo(1002);

    JsonNode platform =
        exchange(
            HttpMethod.GET,
            "/admin/platform/audit-logs?entity=SalonSettings&size=100",
            login(admin.getEmail()),
            null);
    assertThat(platform.get("data").get("items").findValues("salonId"))
        .extracting(JsonNode::asLong)
        .contains(salon.getId());
  }

  // ---- seeding ----------------------------------------------------------------------------------

  private User user(String name, Role role) {
    User user =
        User.local(name + "@example.com", passwordEncoder.encode(PASSWORD), "Test " + role, role);
    user.setEmailVerified(true);
    return userRepository.save(user);
  }

  private Salon salon(User owner, long suffix) {
    Salon salon = Salon.create(owner, "Audit Salon " + suffix, "audit-salon-" + suffix);
    salon.setAddress("Main St 1");
    salon.setCity("Berlin");
    salon.setCountry("DE");
    salon.setStatus(SalonStatus.ACTIVE);
    return salonRepository.save(salon);
  }

  private static Map<String, Object> settings(int slotIntervalMin) {
    return Map.of(
        "slotIntervalMin",
        slotIntervalMin,
        "minAdvanceBookingMin",
        60,
        "maxAdvanceDays",
        60,
        "cancelBeforeMin",
        1440,
        "autoConfirm",
        true,
        "requireDeposit",
        false,
        "depositPercent",
        0,
        "acceptStripe",
        true,
        "acceptPaypal",
        false,
        "acceptCash",
        true);
  }

  // ---- HTTP helpers -----------------------------------------------------------------------------

  private RestClient client() {
    return RestClient.builder()
        .baseUrl("http://localhost:" + port + "/api/v1")
        .defaultStatusHandler(status -> true, (req, res) -> {})
        .build();
  }

  private JsonNode exchange(HttpMethod method, String uri, String token, Object body) {
    RestClient.RequestBodySpec spec = client().method(method).uri(uri);
    if (token != null) {
      spec = spec.header(HttpHeaders.AUTHORIZATION, "Bearer " + token);
    }
    if (body != null) {
      spec.body(body);
    }
    return spec.retrieve().body(JsonNode.class);
  }

  private String login(String email) {
    JsonNode login =
        exchange(
            HttpMethod.POST, "/auth/login", null, Map.of("email", email, "password", PASSWORD));
    return login.get("data").get("accessToken").asText();
  }
}
