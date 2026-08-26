package com.slotify.module.booking;

import static org.assertj.core.api.Assertions.assertThat;

import com.slotify.TestcontainersConfiguration;
import com.slotify.module.salon.entity.Salon;
import com.slotify.module.salon.entity.SalonStatus;
import com.slotify.module.salon.repository.SalonRepository;
import com.slotify.module.service.entity.SalonService;
import com.slotify.module.service.repository.SalonServiceRepository;
import com.slotify.module.staff.entity.Staff;
import com.slotify.module.staff.entity.StaffShift;
import com.slotify.module.staff.repository.StaffRepository;
import com.slotify.module.staff.repository.StaffShiftRepository;
import com.slotify.module.user.entity.Role;
import com.slotify.module.user.entity.User;
import com.slotify.module.user.repository.UserRepository;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.junit.jupiter.api.BeforeEach;
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
 * Availability → booking → double-booking protection under 20 concurrent requests → owner confirm /
 * staff complete → customer cancellation rules.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(TestcontainersConfiguration.class)
class BookingFlowIntegrationTest {

  private static final String PASSWORD = "Password123!";
  private static final ZoneId BERLIN = ZoneId.of("Europe/Berlin");

  @LocalServerPort private int port;
  @Autowired private UserRepository userRepository;
  @Autowired private SalonRepository salonRepository;
  @Autowired private SalonServiceRepository serviceRepository;
  @Autowired private StaffRepository staffRepository;
  @Autowired private StaffShiftRepository shiftRepository;
  @Autowired private PasswordEncoder passwordEncoder;
  @MockitoBean private JavaMailSender mailSender;

  private long suffix;
  private Salon salon;
  private SalonService haircut;
  private Staff stylist;
  private String ownerToken;
  private String customerToken;
  private LocalDate day;

  @BeforeEach
  void setUp() {
    suffix = System.nanoTime();
    User owner = user("owner-" + suffix, Role.SALON_OWNER);
    salon = Salon.create(owner, "Booking Salon " + suffix, "booking-salon-" + suffix);
    salon.setAddress("Main St 1");
    salon.setCity("Berlin");
    salon.setCountry("DE");
    salon.setStatus(SalonStatus.ACTIVE);
    salon.replaceOpeningHours(
        java.util.Arrays.stream(DayOfWeek.values())
            .map(
                d ->
                    com.slotify.module.salon.entity.SalonOpeningHour.of(
                        d, LocalTime.of(9, 0), LocalTime.of(18, 0), false))
            .toList());
    salon = salonRepository.save(salon);
    haircut = serviceRepository.save(SalonService.create(salon, "Haircut", 30, 2500));

    stylist = Staff.create(salon, "Sam");
    stylist.replaceServices(List.of(haircut));
    stylist = staffRepository.save(stylist);
    for (DayOfWeek d : DayOfWeek.values()) {
      shiftRepository.save(StaffShift.of(stylist, d, LocalTime.of(10, 0), LocalTime.of(16, 0)));
    }

    ownerToken = login(owner.getEmail());
    customerToken = login(user("customer-" + suffix, Role.CUSTOMER).getEmail());
    day = LocalDate.now(BERLIN).plusDays(7);
  }

  @Test
  void availabilityReflectsShiftsAndBookings() {
    JsonNode availability = availability();
    JsonNode slots = availability.get("data").get("slots");
    assertThat(availability.get("data").get("durationMin").asInt()).isEqualTo(30);
    // 10:00–16:00 shift, 30 min service, 15 min grid → 10:00 … 15:30 = 23 slots
    assertThat(slots).hasSize(23);
    assertThat(slots.get(0).get("startAt").asText())
        .isEqualTo(day.atTime(10, 0).atZone(BERLIN).toInstant().toString());

    JsonNode created = book(customerToken, slots.get(0).get("startAt").asText());
    assertThat(created.get("code").asInt()).isEqualTo(1000);
    assertThat(created.get("data").get("status").asText()).isEqualTo("CONFIRMED");
    assertThat(created.get("data").get("code").asText()).startsWith("SLT-");

    // 10:00 and 10:15 are gone (10:15 would overlap the 10:00–10:30 booking)
    List<String> remaining =
        availability().get("data").get("slots").findValues("startAt").stream()
            .map(JsonNode::asText)
            .toList();
    assertThat(remaining).hasSize(21);
    assertThat(remaining).doesNotContain(slots.get(0).get("startAt").asText());
  }

  @Test
  void onlyOneOfTwentyConcurrentRequestsWinsTheSlot() throws Exception {
    String slot = availability().get("data").get("slots").get(5).get("startAt").asText();
    List<String> tokens = new ArrayList<>();
    for (int i = 0; i < 20; i++) {
      tokens.add(login(user("racer-" + suffix + "-" + i, Role.CUSTOMER).getEmail()));
    }

    ExecutorService pool = Executors.newFixedThreadPool(20);
    List<CompletableFuture<Integer>> futures =
        tokens.stream()
            .map(t -> CompletableFuture.supplyAsync(() -> book(t, slot).get("code").asInt(), pool))
            .toList();
    List<Integer> codes = futures.stream().map(CompletableFuture::join).toList();
    pool.shutdown();

    assertThat(codes).filteredOn(c -> c == 1000).hasSize(1);
    assertThat(codes).filteredOn(c -> c == 4003).hasSize(19);
  }

  @Test
  void ownerAndCustomerLifecycle() {
    String slot = availability().get("data").get("slots").get(8).get("startAt").asText();
    long bookingId = book(customerToken, slot).get("data").get("id").asLong();
    String base = "/admin/salons/" + salon.getId() + "/bookings/" + bookingId;

    // owner can see the customer's contact details and start/complete the visit
    JsonNode detail = get(base, ownerToken);
    assertThat(detail.get("data").get("customer").get("email").asText()).contains("customer-");
    assertThat(post(base + "/start", ownerToken, null).get("data").get("status").asText())
        .isEqualTo("IN_PROGRESS");
    assertThat(post(base + "/complete", ownerToken, null).get("data").get("status").asText())
        .isEqualTo("COMPLETED");
    // completed is final
    assertThat(post(base + "/cancel", ownerToken, null).get("code").asInt()).isEqualTo(4004);

    // customer: past list contains it, upcoming does not
    assertThat(get("/bookings?status=past", customerToken).get("data").get("items")).hasSize(1);
    assertThat(get("/bookings?status=upcoming", customerToken).get("data").get("items")).isEmpty();

    // a fresh booking can be cancelled by the customer (deadline is 24 h, booking is in 7 days)
    String slot2 = availability().get("data").get("slots").get(12).get("startAt").asText();
    long secondId = book(customerToken, slot2).get("data").get("id").asLong();
    JsonNode cancelled =
        post("/bookings/" + secondId + "/cancel", customerToken, Map.of("reason", "changed plans"));
    assertThat(cancelled.get("data").get("status").asText()).isEqualTo("CANCELLED");
    assertThat(cancelled.get("data").get("cancelledBy").asText()).isEqualTo("CUSTOMER");
  }

  // ---- helpers --------------------------------------------------------------------------------

  private JsonNode availability() {
    return get(
        "/salons/" + salon.getId() + "/availability?date=" + day + "&serviceIds=" + haircut.getId(),
        null);
  }

  private JsonNode book(String token, String startAt) {
    return post(
        "/bookings",
        token,
        Map.of(
            "salonId",
            salon.getId(),
            "serviceIds",
            List.of(haircut.getId()),
            "startAt",
            startAt,
            "paymentMethod",
            "CASH"));
  }

  private User user(String prefix, Role role) {
    User user = User.local(prefix + "@example.com", passwordEncoder.encode(PASSWORD), prefix, role);
    user.setEmailVerified(true);
    return userRepository.save(user);
  }

  private String login(String email) {
    return post("/auth/login", null, Map.of("email", email, "password", PASSWORD))
        .get("data")
        .get("accessToken")
        .asText();
  }

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
}
