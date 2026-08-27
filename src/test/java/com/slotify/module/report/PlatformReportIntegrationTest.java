package com.slotify.module.report;

import static org.assertj.core.api.Assertions.assertThat;

import com.slotify.TestcontainersConfiguration;
import com.slotify.module.booking.entity.Booking;
import com.slotify.module.booking.entity.BookingItem;
import com.slotify.module.booking.entity.BookingStatus;
import com.slotify.module.booking.entity.PaymentMethod;
import com.slotify.module.booking.repository.BookingRepository;
import com.slotify.module.payment.entity.Payment;
import com.slotify.module.payment.entity.PaymentProvider;
import com.slotify.module.payment.entity.PaymentState;
import com.slotify.module.payment.entity.PaymentType;
import com.slotify.module.payment.entity.Refund;
import com.slotify.module.payment.repository.PaymentRepository;
import com.slotify.module.payment.repository.RefundRepository;
import com.slotify.module.salon.entity.Salon;
import com.slotify.module.salon.entity.SalonStatus;
import com.slotify.module.salon.repository.SalonRepository;
import com.slotify.module.service.entity.SalonService;
import com.slotify.module.service.repository.SalonServiceRepository;
import com.slotify.module.user.entity.Role;
import com.slotify.module.user.entity.User;
import com.slotify.module.user.repository.UserRepository;
import java.math.BigDecimal;
import java.time.Instant;
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
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.JsonNode;

/**
 * Super admin endpoints: payouts computed from a seeded Stripe payment and refund, the overview,
 * and user administration (search, suspend, re-role).
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(TestcontainersConfiguration.class)
class PlatformReportIntegrationTest {

  private static final String PASSWORD = "Password123!";
  private static final String PERIOD = "from=2026-07-01&to=2026-07-31";
  private static final long PAID = 10_000;
  private static final long REFUNDED = 1_000;
  private static final long CASH_PRICE = 2_500;

  @LocalServerPort private int port;
  @Autowired private UserRepository userRepository;
  @Autowired private SalonRepository salonRepository;
  @Autowired private SalonServiceRepository salonServiceRepository;
  @Autowired private BookingRepository bookingRepository;
  @Autowired private PaymentRepository paymentRepository;
  @Autowired private RefundRepository refundRepository;
  @MockitoBean private JavaMailSender mailSender;

  private RestClient client;
  private long suffix;
  private String adminToken;
  private User owner;
  private Salon salon;

  @BeforeEach
  void seedSalonWithOnlinePaymentAndCashBooking() {
    client =
        RestClient.builder()
            .baseUrl("http://localhost:" + port + "/api/v1")
            .defaultStatusHandler(status -> true, (req, res) -> {})
            .build();
    suffix = System.nanoTime();
    User admin = registerUser("admin-" + suffix + "@example.com", Role.SUPER_ADMIN);
    adminToken = login(admin.getEmail());
    owner = registerUser("owner-" + suffix + "@example.com", Role.SALON_OWNER);
    User customer = registerUser("cust-" + suffix + "@example.com", Role.CUSTOMER);

    salon = Salon.create(owner, "Payout Salon " + suffix, "payout-salon-" + suffix);
    salon.setAddress("1 Main Street");
    salon.setCity("Berlin");
    salon.setCountry("DE");
    salon.setStatus(SalonStatus.ACTIVE);
    salon.setCommissionPercent(new BigDecimal("12.50"));
    salon = salonRepository.save(salon);
    SalonService massage =
        salonServiceRepository.save(SalonService.create(salon, "Massage", 60, PAID));

    Booking online =
        booking(
            customer,
            massage,
            "2026-07-05T10:00:00Z",
            BookingStatus.COMPLETED,
            PaymentMethod.STRIPE);
    Payment payment = Payment.create(online, PaymentProvider.STRIPE, PaymentType.FULL, PAID);
    payment.setStatus(PaymentState.PARTIAL_REFUND);
    payment.setPaidAt(Instant.parse("2026-07-05T09:00:00Z"));
    payment = paymentRepository.save(payment);
    Refund refund = Refund.create(payment, REFUNDED, "Late cancellation");
    refund.setStatus(Refund.Status.SUCCEEDED);
    refundRepository.save(refund);
    Refund failed = Refund.create(payment, REFUNDED, "Provider error");
    failed.setStatus(Refund.Status.FAILED);
    refundRepository.save(failed);

    SalonService trim =
        salonServiceRepository.save(SalonService.create(salon, "Trim", 20, CASH_PRICE));
    booking(customer, trim, "2026-07-06T10:00:00Z", BookingStatus.COMPLETED, PaymentMethod.CASH);
  }

  @Test
  void payoutsSubtractRefundsAndCommission() {
    JsonNode rows = adminGet("/reports/payouts?" + PERIOD).get("data").get("rows");
    JsonNode row = null;
    for (JsonNode candidate : rows) {
      if (candidate.get("salonId").asLong() == salon.getId()) {
        row = candidate;
      }
    }
    assertThat(row).isNotNull();
    assertThat(row.get("currency").asText()).isEqualTo("EUR");
    assertThat(row.get("onlineRevenueMinor").asLong()).isEqualTo(PAID - REFUNDED);
    // 9000 * 12.5 % = 1125
    assertThat(row.get("commissionMinor").asLong()).isEqualTo(1_125);
    assertThat(row.get("payoutMinor").asLong()).isEqualTo(7_875);
    assertThat(row.get("cashRevenueMinor").asLong()).isEqualTo(CASH_PRICE);

    ResponseEntity<byte[]> excel =
        client
            .get()
            .uri("/admin/platform/reports/payouts/export?" + PERIOD + "&type=excel")
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
            .retrieve()
            .toEntity(byte[].class);
    assertThat(excel.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(excel.getHeaders().getContentDisposition().getFilename())
        .isEqualTo("payouts-report.xlsx");
  }

  @Test
  void overviewCountsSalonsAndUsers() {
    JsonNode data = adminGet("/reports/overview").get("data");
    assertThat(data.get("salonsByStatus").get("ACTIVE").asLong()).isGreaterThanOrEqualTo(1);
    assertThat(data.get("usersByRole").get("SUPER_ADMIN").asLong()).isGreaterThanOrEqualTo(1);
    assertThat(data.get("revenueLast30Days").isArray()).isTrue();
    assertThat(data.get("topSalons").isArray()).isTrue();
  }

  @Test
  void userAdministration() {
    JsonNode search = adminGet("/users?q=owner-" + suffix + "&role=SALON_OWNER").get("data");
    assertThat(search.get("totalItems").asLong()).isEqualTo(1);
    assertThat(search.get("items").get(0).get("id").asLong()).isEqualTo(owner.getId());

    JsonNode suspended =
        adminPut("/users/" + owner.getId() + "/status", Map.of("status", "SUSPENDED"));
    assertThat(suspended.get("data").get("status").asText()).isEqualTo("SUSPENDED");
    JsonNode refused =
        client
            .post()
            .uri("/auth/login")
            .body(Map.of("email", owner.getEmail(), "password", PASSWORD))
            .retrieve()
            .body(JsonNode.class);
    assertThat(refused.get("success").asBoolean()).isFalse();

    JsonNode reRoled = adminPut("/users/" + owner.getId() + "/role", Map.of("role", "CUSTOMER"));
    assertThat(reRoled.get("data").get("role").asText()).isEqualTo("CUSTOMER");
    assertThat(adminGet("/users/" + owner.getId()).get("data").get("role").asText())
        .isEqualTo("CUSTOMER");
  }

  // ---------------------------------------------------------------------------------------------

  private Booking booking(
      User customer,
      SalonService service,
      String startUtc,
      BookingStatus status,
      PaymentMethod method) {
    Instant start = Instant.parse(startUtc);
    Booking booking =
        Booking.create(
            "P" + (suffix % 100_000_000) + bookingRepository.count(),
            salon,
            customer,
            null,
            start,
            start.plusSeconds(service.getDurationMin() * 60L));
    booking.addItem(BookingItem.from(service));
    booking.setStatus(status);
    booking.setPaymentMethod(method);
    return bookingRepository.save(booking);
  }

  private JsonNode adminGet(String pathAndQuery) {
    return client
        .get()
        .uri("/admin/platform" + pathAndQuery)
        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
        .retrieve()
        .body(JsonNode.class);
  }

  private JsonNode adminPut(String path, Object body) {
    return client
        .put()
        .uri("/admin/platform" + path)
        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
        .body(body)
        .retrieve()
        .body(JsonNode.class);
  }

  private User registerUser(String email, Role role) {
    client
        .post()
        .uri("/auth/register")
        .body(Map.of("fullName", "Test User", "email", email, "password", PASSWORD))
        .retrieve()
        .toBodilessEntity();
    User user = userRepository.findByEmailIgnoreCase(email).orElseThrow();
    user.setRole(role);
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
}
