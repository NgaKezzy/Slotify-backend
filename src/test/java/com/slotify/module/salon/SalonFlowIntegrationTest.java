package com.slotify.module.salon;

import static org.assertj.core.api.Assertions.assertThat;

import com.slotify.TestcontainersConfiguration;
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
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.JsonNode;

/**
 * Owner creates a salon → it is hidden until a SUPER_ADMIN approves it → it appears in the public
 * search with its services → another owner cannot edit it → favourites work.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(TestcontainersConfiguration.class)
class SalonFlowIntegrationTest {

  private static final String PASSWORD = "Password123!";

  @LocalServerPort private int port;
  @Autowired private UserRepository userRepository;
  @Autowired private PasswordEncoder passwordEncoder;
  @MockitoBean private JavaMailSender mailSender;

  @Test
  void salonLifecycle() {
    String ownerEmail = "owner-" + System.nanoTime() + "@example.com";
    String owner = tokenFor(ownerEmail, Role.CUSTOMER);
    String admin = tokenFor("admin-" + System.nanoTime() + "@example.com", Role.SUPER_ADMIN);
    String other = tokenFor("other-" + System.nanoTime() + "@example.com", Role.SALON_OWNER);
    String customer = tokenFor("cust-" + System.nanoTime() + "@example.com", Role.CUSTOMER);

    long salonId = createSalon(owner);
    // The access token still carries the CUSTOMER role: clients must refresh (or re-login)
    // after creating their first salon.
    assertThat(userRepository.findByEmailIgnoreCase(ownerEmail).orElseThrow().getRole())
        .isEqualTo(Role.SALON_OWNER);
    owner = tokenFor(ownerEmail, Role.SALON_OWNER);

    addService(owner, salonId);
    assertHiddenWhilePending(salonId, other);
    approve(admin, salonId);
    assertPubliclyVisible(salonId);
    assertFavourites(customer, salonId);
  }

  private long createSalon(String ownerToken) {
    JsonNode created = post("/admin/salons", ownerToken, salonBody("Glow & Go Berlin"));
    assertThat(created.get("code").asInt()).isEqualTo(1000);
    JsonNode data = created.get("data");
    assertThat(data.get("slug").asText()).isEqualTo("glow-go-berlin");
    assertThat(data.get("status").asText()).isEqualTo("PENDING");
    assertThat(data.get("openingHours")).hasSize(7);
    assertThat(data.get("settings").get("slotIntervalMin").asInt()).isEqualTo(15);
    return data.get("id").asLong();
  }

  private void addService(String ownerToken, long salonId) {
    JsonNode service =
        post(
            "/admin/salons/" + salonId + "/services",
            ownerToken,
            Map.of(
                "name", "Haircut",
                "durationMin", 45,
                "bufferAfterMin", 15,
                "priceMinor", 3500,
                "active", true,
                "sortOrder", 1));
    assertThat(service.get("code").asInt()).isEqualTo(1000);
    assertThat(service.get("data").get("currency").asText()).isEqualTo("EUR");
  }

  private void assertHiddenWhilePending(long salonId, String otherOwnerToken) {
    JsonNode hidden = get("/salons/" + salonId, null);
    assertThat(hidden.get("code").asInt()).isEqualTo(2003);

    JsonNode forbidden = get("/admin/salons/" + salonId, otherOwnerToken);
    assertThat(forbidden.get("code").asInt()).isEqualTo(1002);
  }

  private void approve(String adminToken, long salonId) {
    JsonNode approved = post("/admin/platform/salons/" + salonId + "/approve", adminToken, null);
    assertThat(approved.get("data").get("status").asText()).isEqualTo("ACTIVE");
  }

  private void assertPubliclyVisible(long salonId) {
    JsonNode search = get("/salons?q=glow&lat=52.52&lng=13.40&radiusKm=10&sort=distance", null);
    assertThat(search.get("success").asBoolean()).as(search.toString()).isTrue();
    JsonNode items = search.get("data").get("items");
    assertThat(items.findValues("id")).extracting(JsonNode::asLong).contains(salonId);
    assertThat(items.get(0).get("distanceKm").asDouble()).isLessThan(5);

    JsonNode bySlug = get("/salons/glow-go-berlin", null);
    assertThat(bySlug.get("data").get("id").asLong()).isEqualTo(salonId);

    JsonNode publicServices = get("/salons/" + salonId + "/services", null);
    assertThat(publicServices.get("data")).hasSize(1);
  }

  private void assertFavourites(String customerToken, long salonId) {
    post("/me/favorites/" + salonId, customerToken, null);
    JsonNode favorites = get("/me/favorites", customerToken);
    assertThat(favorites.get("data").findValues("id"))
        .extracting(JsonNode::asLong)
        .contains(salonId);
  }

  // ---- HTTP helpers -----------------------------------------------------------------------------

  private RestClient client() {
    return RestClient.builder()
        .baseUrl("http://localhost:" + port + "/api/v1")
        .defaultStatusHandler(status -> true, (req, res) -> {})
        .build();
  }

  private JsonNode get(String uri, String token) {
    RestClient.RequestHeadersSpec<?> spec = client().get().uri(uri);
    if (token != null) {
      spec = spec.header(HttpHeaders.AUTHORIZATION, "Bearer " + token);
    }
    return spec.retrieve().body(JsonNode.class);
  }

  private JsonNode post(String uri, String token, Object body) {
    RestClient.RequestBodySpec spec = client().post().uri(uri);
    if (token != null) {
      spec = spec.header(HttpHeaders.AUTHORIZATION, "Bearer " + token);
    }
    if (body != null) {
      spec.body(body);
    }
    return spec.retrieve().body(JsonNode.class);
  }

  private String tokenFor(String email, Role role) {
    userRepository
        .findByEmailIgnoreCase(email)
        .orElseGet(
            () -> {
              User user = User.local(email, passwordEncoder.encode(PASSWORD), "Test " + role, role);
              user.setEmailVerified(true);
              return userRepository.save(user);
            });
    JsonNode login = post("/auth/login", null, Map.of("email", email, "password", PASSWORD));
    return login.get("data").get("accessToken").asText();
  }

  private static Map<String, Object> salonBody(String name) {
    return Map.of(
        "name", name,
        "address", "Unter den Linden 1",
        "city", "Berlin",
        "country", "DE",
        "timezone", "Europe/Berlin",
        "currency", "EUR",
        "lat", 52.5170,
        "lng", 13.3889);
  }
}
