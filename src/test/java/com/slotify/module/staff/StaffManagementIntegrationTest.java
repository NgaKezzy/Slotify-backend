package com.slotify.module.staff;

import static org.assertj.core.api.Assertions.assertThat;

import com.slotify.TestcontainersConfiguration;
import com.slotify.module.salon.entity.Salon;
import com.slotify.module.salon.entity.SalonStatus;
import com.slotify.module.salon.repository.SalonRepository;
import com.slotify.module.service.entity.SalonService;
import com.slotify.module.service.repository.SalonServiceRepository;
import com.slotify.module.user.entity.Role;
import com.slotify.module.user.entity.User;
import com.slotify.module.user.repository.UserRepository;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.JsonNode;

/**
 * End-to-end test of Phase 1.3 against a real MySQL: an owner creates a staff member with an
 * invitation, assigns a service and a weekly schedule; the public catalog lists the staff member;
 * the invited account can use the Staff app; a different owner is rejected with 1002.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(TestcontainersConfiguration.class)
class StaffManagementIntegrationTest {

  private static final String PASSWORD = "Password123!";

  @LocalServerPort private int port;
  @Autowired private UserRepository userRepository;
  @Autowired private SalonRepository salonRepository;
  @Autowired private SalonServiceRepository salonServiceRepository;
  @Autowired private PasswordEncoder passwordEncoder;
  @MockitoBean private JavaMailSender mailSender;

  private RestClient client;
  private long suffix;
  private String ownerToken;
  private Salon salon;
  private SalonService haircut;
  private String staffPath;

  @BeforeEach
  void setUpOwnerSalonAndService() {
    client =
        RestClient.builder()
            .baseUrl("http://localhost:" + port + "/api/v1")
            .defaultStatusHandler(status -> true, (req, res) -> {})
            .build();
    suffix = System.nanoTime();
    User owner = registerOwner("owner-" + suffix + "@example.com");
    ownerToken = login(owner.getEmail());
    salon = createActiveSalon(owner, "Staff Salon " + suffix, "staff-salon-" + suffix);
    haircut = salonServiceRepository.save(SalonService.create(salon, "Haircut", 30, 2500));
    staffPath = "/admin/salons/" + salon.getId() + "/staff";
  }

  @Test
  void createWithInviteLinksStaffAccountAndPublicCatalogListsServices() {
    String inviteEmail = "invited-" + suffix + "@example.com";
    JsonNode staff = createStaff(inviteEmail);
    long staffId = staff.get("id").asLong();
    assertThat(staff.get("userId").isNull()).isFalse();
    User invited = userRepository.findByEmailIgnoreCase(inviteEmail).orElseThrow();
    assertThat(invited.getRole()).isEqualTo(Role.STAFF);
    assertThat(invited.isEmailVerified()).isTrue();

    JsonNode withServices =
        ownerPut(staffId + "/services", Map.of("serviceIds", List.of(haircut.getId())));
    assertThat(withServices.get("data").get("serviceIds").get(0).asLong())
        .isEqualTo(haircut.getId());

    JsonNode foreignService =
        ownerPut(staffId + "/services", Map.of("serviceIds", List.of(haircut.getId() + 100_000)));
    assertThat(foreignService.get("code").asInt()).isEqualTo(3003);

    JsonNode publicStaff =
        client.get().uri("/salons/" + salon.getId() + "/staff").retrieve().body(JsonNode.class);
    assertThat(publicStaff.get("success").asBoolean()).isTrue();
    JsonNode listed = publicStaff.get("data").get(0);
    assertThat(listed.get("id").asLong()).isEqualTo(staffId);
    assertThat(listed.get("displayName").asText()).isEqualTo("Sam Stylist");
    assertThat(listed.get("serviceIds").get(0).asLong()).isEqualTo(haircut.getId());
    assertThat(listed.has("userId")).isFalse();
  }

  @Test
  void weeklyScheduleIsReplacedAndOverlapsRejected() {
    long staffId = createStaff(null).get("id").asLong();

    JsonNode shifts =
        ownerPut(
            staffId + "/shifts",
            Map.of(
                "shifts",
                List.of(
                    shift("MONDAY", "09:00", "12:00"),
                    shift("MONDAY", "13:00", "17:00"),
                    shift("TUESDAY", "10:00", "18:00"))));
    assertThat(shifts.get("code").asInt()).isEqualTo(1000);
    assertThat(shifts.get("data")).hasSize(3);

    JsonNode overlap =
        ownerPut(
            staffId + "/shifts",
            Map.of(
                "shifts",
                List.of(shift("MONDAY", "09:00", "12:00"), shift("MONDAY", "11:00", "17:00"))));
    assertThat(overlap.get("code").asInt()).isEqualTo(4006);

    JsonNode unchanged = ownerGet(staffId + "/shifts");
    assertThat(unchanged.get("data")).hasSize(3);
  }

  @Test
  void invitedAccountCanUseStaffApp() {
    String inviteEmail = "staffapp-" + suffix + "@example.com";
    long staffId = createStaff(inviteEmail).get("id").asLong();
    ownerPut(staffId + "/shifts", Map.of("shifts", List.of(shift("FRIDAY", "09:00", "17:00"))));

    // The temporary password only exists in the email; set a known one to sign in.
    User invited = userRepository.findByEmailIgnoreCase(inviteEmail).orElseThrow();
    invited.setPasswordHash(passwordEncoder.encode(PASSWORD));
    userRepository.save(invited);
    String staffToken = login(inviteEmail);

    JsonNode me = get("/staff/me", staffToken);
    assertThat(me.get("data").get("staff").get("id").asLong()).isEqualTo(staffId);
    assertThat(me.get("data").get("salon").get("id").asLong()).isEqualTo(salon.getId());

    JsonNode schedule = get("/staff/shifts", staffToken);
    assertThat(schedule.get("data").get("shifts")).hasSize(1);
    assertThat(schedule.get("data").get("shifts").get(0).get("dayOfWeek").asText())
        .isEqualTo("FRIDAY");
  }

  @Test
  void anotherOwnerIsForbidden() {
    createStaff(null);
    User intruder = registerOwner("intruder-" + suffix + "@example.com");
    String intruderToken = login(intruder.getEmail());

    ResponseEntity<JsonNode> forbidden =
        client
            .get()
            .uri(staffPath)
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + intruderToken)
            .retrieve()
            .toEntity(JsonNode.class);
    assertThat(forbidden.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    assertThat(forbidden.getBody().get("code").asInt()).isEqualTo(1002);
  }

  // ---------------------------------------------------------------------------------------------

  private JsonNode createStaff(String inviteEmail) {
    Map<String, String> body =
        inviteEmail == null
            ? Map.of("displayName", "Sam Stylist", "title", "Stylist")
            : Map.of("displayName", "Sam Stylist", "title", "Stylist", "inviteEmail", inviteEmail);
    ResponseEntity<JsonNode> created =
        client
            .post()
            .uri(staffPath)
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + ownerToken)
            .body(body)
            .retrieve()
            .toEntity(JsonNode.class);
    assertThat(created.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    return created.getBody().get("data");
  }

  private JsonNode ownerPut(String path, Object body) {
    return client
        .put()
        .uri(staffPath + "/" + path)
        .header(HttpHeaders.AUTHORIZATION, "Bearer " + ownerToken)
        .body(body)
        .retrieve()
        .body(JsonNode.class);
  }

  private JsonNode ownerGet(String path) {
    return get(staffPath + "/" + path, ownerToken);
  }

  private JsonNode get(String uri, String token) {
    return client
        .get()
        .uri(uri)
        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
        .retrieve()
        .body(JsonNode.class);
  }

  private static Map<String, String> shift(String day, String start, String end) {
    return Map.of("dayOfWeek", day, "startTime", start, "endTime", end);
  }

  /** Registers through the API, then promotes the account to SALON_OWNER directly. */
  private User registerOwner(String email) {
    client
        .post()
        .uri("/auth/register")
        .body(Map.of("fullName", "Olivia Owner", "email", email, "password", PASSWORD))
        .retrieve()
        .toBodilessEntity();
    User user = userRepository.findByEmailIgnoreCase(email).orElseThrow();
    user.setRole(Role.SALON_OWNER);
    return userRepository.save(user);
  }

  private String login(String email) {
    JsonNode session =
        client
            .post()
            .uri("/auth/login")
            .body(Map.of("email", email, "password", PASSWORD))
            .retrieve()
            .body(JsonNode.class);
    return session.get("data").get("accessToken").asText();
  }

  private Salon createActiveSalon(User owner, String name, String slug) {
    Salon salon = Salon.create(owner, name, slug);
    salon.setAddress("1 Main Street");
    salon.setCity("Berlin");
    salon.setCountry("DE");
    salon.setStatus(SalonStatus.ACTIVE);
    return salonRepository.save(salon);
  }
}
