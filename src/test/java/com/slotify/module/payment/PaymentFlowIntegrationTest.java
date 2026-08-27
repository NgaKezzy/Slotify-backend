package com.slotify.module.payment;

import static org.assertj.core.api.Assertions.assertThat;

import com.slotify.TestcontainersConfiguration;
import com.slotify.module.booking.entity.Booking;
import com.slotify.module.booking.entity.PaymentStatus;
import com.slotify.module.booking.repository.BookingRepository;
import com.slotify.module.payment.entity.Payment;
import com.slotify.module.payment.entity.PaymentProvider;
import com.slotify.module.payment.entity.PaymentType;
import com.slotify.module.payment.repository.PaymentRepository;
import com.slotify.module.payment.service.PaymentService;
import com.slotify.module.salon.entity.Salon;
import com.slotify.module.salon.entity.SalonOpeningHour;
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
import java.util.Arrays;
import java.util.List;
import java.util.Map;
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
 * Coupons and payments end to end: validate + apply a coupon at booking time, cash payment marks
 * the booking PAID, a simulated Stripe webhook confirms an online payment, and Stripe/PayPal
 * endpoints fail cleanly (3004) when the keys are not configured.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(TestcontainersConfiguration.class)
class PaymentFlowIntegrationTest {

  private static final String PASSWORD = "Password123!";
  private static final ZoneId BERLIN = ZoneId.of("Europe/Berlin");

  @LocalServerPort private int port;
  @Autowired private UserRepository userRepository;
  @Autowired private SalonRepository salonRepository;
  @Autowired private SalonServiceRepository serviceRepository;
  @Autowired private StaffRepository staffRepository;
  @Autowired private StaffShiftRepository shiftRepository;
  @Autowired private BookingRepository bookingRepository;
  @Autowired private PaymentRepository paymentRepository;
  @Autowired private PaymentService paymentService;
  @Autowired private PasswordEncoder passwordEncoder;
  @MockitoBean private JavaMailSender mailSender;

  private Salon salon;
  private SalonService haircut;
  private String ownerToken;
  private String customerToken;
  private LocalDate day;

  @BeforeEach
  void setUp() {
    long suffix = System.nanoTime();
    User owner = user("pay-owner-" + suffix, Role.SALON_OWNER);
    salon = Salon.create(owner, "Pay Salon " + suffix, "pay-salon-" + suffix);
    salon.setAddress("Main St 1");
    salon.setCity("Berlin");
    salon.setCountry("DE");
    salon.setStatus(SalonStatus.ACTIVE);
    salon.replaceOpeningHours(
        Arrays.stream(DayOfWeek.values())
            .map(d -> SalonOpeningHour.of(d, LocalTime.of(9, 0), LocalTime.of(18, 0), false))
            .toList());
    salon = salonRepository.save(salon);
    haircut = serviceRepository.save(SalonService.create(salon, "Haircut", 30, 4000));
    Staff stylist = Staff.create(salon, "Sam");
    stylist.replaceServices(List.of(haircut));
    stylist = staffRepository.save(stylist);
    for (DayOfWeek d : DayOfWeek.values()) {
      shiftRepository.save(StaffShift.of(stylist, d, LocalTime.of(10, 0), LocalTime.of(16, 0)));
    }
    ownerToken = login(owner.getEmail());
    customerToken = login(user("pay-customer-" + suffix, Role.CUSTOMER).getEmail());
    day = LocalDate.now(BERLIN).plusDays(5);
  }

  @Test
  void couponIsValidatedAndAppliedToTheBooking() {
    JsonNode created =
        post(
            "/admin/salons/" + salon.getId() + "/coupons",
            ownerToken,
            Map.of(
                "code",
                "welcome-25",
                "type",
                "PERCENT",
                "value",
                25,
                "minOrderMinor",
                1000,
                "perUserLimit",
                1,
                "active",
                true));
    assertThat(created.get("data").get("code").asText()).isEqualTo("WELCOME-25");

    JsonNode preview =
        post(
            "/coupons/validate",
            customerToken,
            Map.of(
                "salonId",
                salon.getId(),
                "code",
                "welcome-25",
                "serviceIds",
                List.of(haircut.getId())));
    assertThat(preview.get("data").get("discountMinor").asLong()).isEqualTo(1000);
    assertThat(preview.get("data").get("totalMinor").asLong()).isEqualTo(3000);

    JsonNode booking = book(slot(0), "WELCOME-25", "CASH");
    assertThat(booking.get("code").asInt()).isEqualTo(1000);
    assertThat(booking.get("data").get("discountMinor").asLong()).isEqualTo(1000);
    assertThat(booking.get("data").get("totalMinor").asLong()).isEqualTo(3000);
    assertThat(booking.get("data").get("couponCode").asText()).isEqualTo("WELCOME-25");

    // per-user limit reached → 3005
    JsonNode second = book(slot(4), "WELCOME-25", "CASH");
    assertThat(second.get("code").asInt()).isEqualTo(3005);
  }

  @Test
  void cashPaymentMarksBookingPaidAndCannotBeRefundedOnline() {
    long bookingId = book(slot(0), null, "CASH").get("data").get("id").asLong();
    String base = "/admin/salons/" + salon.getId();

    JsonNode paid = post(base + "/bookings/" + bookingId + "/mark-paid", ownerToken, null);
    assertThat(paid.get("data").get("provider").asText()).isEqualTo("CASH");
    assertThat(paid.get("data").get("status").asText()).isEqualTo("SUCCEEDED");
    assertThat(bookingRepository.findById(bookingId).orElseThrow().getPaymentStatus())
        .isEqualTo(PaymentStatus.PAID);

    long paymentId = paid.get("data").get("id").asLong();
    JsonNode refund =
        post(base + "/payments/" + paymentId + "/refund", ownerToken, Map.of("reason", "x"));
    assertThat(refund.get("code").asInt()).isEqualTo(4004);

    JsonNode list = get(base + "/payments", ownerToken);
    assertThat(list.get("data").get("items")).hasSize(1);
  }

  @Test
  void onlineProvidersRejectWhenNotConfiguredAndWebhookConfirmsPayment() {
    JsonNode stripeBooking = book(slot(0), null, "STRIPE");
    assertThat(stripeBooking.get("success").asBoolean()).as(stripeBooking.toString()).isTrue();
    long bookingId = stripeBooking.get("data").get("id").asLong();
    JsonNode intent =
        post("/payments/stripe/intent", customerToken, Map.of("bookingId", bookingId));
    assertThat(intent.get("code").asInt()).isEqualTo(3004);

    // Simulate what the Stripe webhook does once the PaymentIntent succeeded.
    Booking booking = bookingRepository.findById(bookingId).orElseThrow();
    Payment payment =
        Payment.create(booking, PaymentProvider.STRIPE, PaymentType.FULL, booking.getTotalMinor());
    payment.setProviderRef("pi_test_" + bookingId);
    paymentRepository.save(payment);
    paymentService.handleSucceeded(
        "pi_test_" + bookingId, Map.of("eventType", "payment_intent.succeeded"));

    assertThat(bookingRepository.findById(bookingId).orElseThrow().getPaymentStatus())
        .isEqualTo(PaymentStatus.PAID);
    JsonNode config = get("/payments/config", null);
    assertThat(config.get("data").get("stripeEnabled").asBoolean()).isFalse();
  }

  // ---- helpers ---------------------------------------------------------------------------------

  private String slot(int index) {
    JsonNode availability =
        get(
            "/salons/"
                + salon.getId()
                + "/availability?date="
                + day
                + "&serviceIds="
                + haircut.getId(),
            null);
    return availability.get("data").get("slots").get(index).get("startAt").asText();
  }

  private JsonNode book(String startAt, String couponCode, String method) {
    Map<String, Object> body =
        new java.util.HashMap<>(
            Map.of(
                "salonId",
                salon.getId(),
                "serviceIds",
                List.of(haircut.getId()),
                "startAt",
                startAt,
                "paymentMethod",
                method));
    if (couponCode != null) {
      body.put("couponCode", couponCode);
    }
    return post("/bookings", customerToken, body);
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
