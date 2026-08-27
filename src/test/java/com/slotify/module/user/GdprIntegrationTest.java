package com.slotify.module.user;

import static org.assertj.core.api.Assertions.assertThat;

import com.slotify.TestcontainersConfiguration;
import com.slotify.module.booking.entity.Booking;
import com.slotify.module.booking.entity.BookingItem;
import com.slotify.module.booking.repository.BookingRepository;
import com.slotify.module.salon.entity.Favorite;
import com.slotify.module.salon.entity.Salon;
import com.slotify.module.salon.entity.SalonStatus;
import com.slotify.module.salon.repository.FavoriteRepository;
import com.slotify.module.salon.repository.SalonRepository;
import com.slotify.module.service.entity.SalonService;
import com.slotify.module.service.repository.SalonServiceRepository;
import com.slotify.module.user.entity.Role;
import com.slotify.module.user.entity.User;
import com.slotify.module.user.entity.UserStatus;
import com.slotify.module.user.repository.UserRepository;
import com.slotify.module.user.service.GdprService;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.JsonNode;

/**
 * GDPR flows: a customer exports their data (bookings included), then deletes the account and can
 * no longer sign in; an owner with a live salon is refused.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(TestcontainersConfiguration.class)
class GdprIntegrationTest {

  private static final String PASSWORD = "Password123!";

  @LocalServerPort private int port;
  @Autowired private UserRepository userRepository;
  @Autowired private SalonRepository salonRepository;
  @Autowired private SalonServiceRepository serviceRepository;
  @Autowired private BookingRepository bookingRepository;
  @Autowired private FavoriteRepository favoriteRepository;
  @Autowired private PasswordEncoder passwordEncoder;
  @Autowired private JdbcTemplate jdbcTemplate;
  @MockitoBean private JavaMailSender mailSender;

  @Test
  void exportContainsBookingsAndDeleteAnonymisesTheAccount() {
    long suffix = System.nanoTime();
    User owner = user("owner-" + suffix, Role.SALON_OWNER);
    User customer = user("customer-" + suffix, Role.CUSTOMER);
    Salon salon = salon(owner, suffix);
    Booking booking = seedBooking(salon, customer);
    String bookingCode = booking.getCode();
    favoriteRepository.save(Favorite.of(customer.getId(), salon.getId()));
    String token = login(customer.getEmail());

    ResponseEntity<String> export = exportRaw(token);
    assertThat(export.getStatusCode().is2xxSuccessful()).isTrue();
    assertThat(export.getHeaders().getFirst(HttpHeaders.CONTENT_DISPOSITION))
        .contains("attachment")
        .contains("slotify-data-" + customer.getId() + ".json");
    assertThat(export.getBody())
        .contains(bookingCode)
        .contains(salon.getName())
        .contains("Haircut")
        .contains(customer.getEmail());

    // Wrong password is refused, the right one anonymises the account.
    JsonNode wrong = delete("/me", token, Map.of("password", "nope"));
    assertThat(wrong.get("code").asInt()).isEqualTo(1003);
    JsonNode deleted = delete("/me", token, Map.of("password", PASSWORD));
    assertThat(deleted.get("code").asInt()).isEqualTo(1000);

    Map<String, Object> row =
        jdbcTemplate.queryForMap(
            "SELECT email, full_name, phone, password_hash, status, deleted_at FROM users"
                + " WHERE id = ?",
            customer.getId());
    assertThat(row.get("email")).isEqualTo("deleted-" + customer.getId() + "@anonymized.local");
    assertThat(row.get("full_name")).isEqualTo(GdprService.DELETED_USER_NAME);
    assertThat(row.get("phone")).isNull();
    assertThat(row.get("password_hash")).isNull();
    assertThat(row.get("status")).isEqualTo(UserStatus.DELETED.name());
    assertThat(row.get("deleted_at")).as("row stays visible for booking history").isNull();
    assertThat(favoriteRepository.findSalonsByUserId(customer.getId())).isEmpty();
    assertThat(bookingRepository.findAllByCustomerIdOrderByStartAtDesc(customer.getId()))
        .as("bookings are kept as financial history")
        .hasSize(1);

    // The old email no longer belongs to any visible account.
    JsonNode login =
        post("/auth/login", null, Map.of("email", customer.getEmail(), "password", PASSWORD));
    assertThat(login.get("code").asInt()).isEqualTo(1003);

    // The salon still sees its booking history, now attributed to the anonymised customer.
    JsonNode ownerView =
        get(
            "/admin/salons/" + salon.getId() + "/bookings/" + booking.getId(),
            login(owner.getEmail()));
    assertThat(ownerView.get("code").asInt()).as(ownerView.toString()).isEqualTo(1000);
    assertThat(ownerView.get("data").get("customer").get("fullName").asText())
        .isEqualTo(GdprService.DELETED_USER_NAME);
  }

  @Test
  void deletedStatusCannotSignIn() {
    User user = user("deleted-" + System.nanoTime(), Role.CUSTOMER);
    user.setStatus(UserStatus.DELETED);
    userRepository.save(user);

    JsonNode login =
        post("/auth/login", null, Map.of("email", user.getEmail(), "password", PASSWORD));
    assertThat(login.get("code").asInt()).isEqualTo(1007);
  }

  @Test
  void suspendedUserCannotSignIn() {
    User user = user("suspended-" + System.nanoTime(), Role.CUSTOMER);
    user.setStatus(UserStatus.SUSPENDED);
    userRepository.save(user);

    JsonNode login =
        post("/auth/login", null, Map.of("email", user.getEmail(), "password", PASSWORD));
    assertThat(login.get("code").asInt()).isEqualTo(1007);
  }

  @Test
  void ownerWithActiveSalonCannotDeleteAccount() {
    long suffix = System.nanoTime();
    User owner = user("busy-owner-" + suffix, Role.SALON_OWNER);
    salon(owner, suffix);
    String token = login(owner.getEmail());

    JsonNode refused = delete("/me", token, Map.of("password", PASSWORD));
    assertThat(refused.get("code").asInt()).isEqualTo(4009);
    assertThat(userRepository.findById(owner.getId()).orElseThrow().getStatus())
        .isEqualTo(UserStatus.ACTIVE);
  }

  // ---- seeding ----------------------------------------------------------------------------------

  private User user(String name, Role role) {
    User user =
        User.local(name + "@example.com", passwordEncoder.encode(PASSWORD), "Test " + role, role);
    user.setEmailVerified(true);
    return userRepository.save(user);
  }

  private Salon salon(User owner, long suffix) {
    Salon salon = Salon.create(owner, "Gdpr Salon " + suffix, "gdpr-salon-" + suffix);
    salon.setAddress("Main St 1");
    salon.setCity("Berlin");
    salon.setCountry("DE");
    salon.setStatus(SalonStatus.ACTIVE);
    return salonRepository.save(salon);
  }

  private Booking seedBooking(Salon salon, User customer) {
    SalonService haircut = serviceRepository.save(SalonService.create(salon, "Haircut", 30, 2500));
    Instant start = Instant.now().plus(3, ChronoUnit.DAYS).truncatedTo(ChronoUnit.HOURS);
    Booking booking =
        Booking.create(
            "SLT-GDPR" + (customer.getId() % 1000),
            salon,
            customer,
            null,
            start,
            start.plus(30, ChronoUnit.MINUTES));
    booking.addItem(BookingItem.from(haircut));
    return bookingRepository.save(booking);
  }

  // ---- HTTP helpers -----------------------------------------------------------------------------

  private RestClient client() {
    return RestClient.builder()
        .baseUrl("http://localhost:" + port + "/api/v1")
        .defaultStatusHandler(status -> true, (req, res) -> {})
        .build();
  }

  private ResponseEntity<String> exportRaw(String token) {
    return client()
        .get()
        .uri("/me/export")
        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
        .retrieve()
        .toEntity(String.class);
  }

  private JsonNode get(String uri, String token) {
    return client()
        .get()
        .uri(uri)
        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
        .retrieve()
        .body(JsonNode.class);
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

  private JsonNode delete(String uri, String token, Object body) {
    return client()
        .method(org.springframework.http.HttpMethod.DELETE)
        .uri(uri)
        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
        .body(body)
        .retrieve()
        .body(JsonNode.class);
  }

  private String login(String email) {
    JsonNode login = post("/auth/login", null, Map.of("email", email, "password", PASSWORD));
    return login.get("data").get("accessToken").asText();
  }
}
