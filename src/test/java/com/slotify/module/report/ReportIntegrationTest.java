package com.slotify.module.report;

import static org.assertj.core.api.Assertions.assertThat;

import com.slotify.TestcontainersConfiguration;
import com.slotify.module.booking.entity.Booking;
import com.slotify.module.booking.entity.BookingItem;
import com.slotify.module.booking.entity.BookingStatus;
import com.slotify.module.booking.entity.PaymentMethod;
import com.slotify.module.booking.repository.BookingRepository;
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
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
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
 * Owner reports against a real MySQL: a salon with two staff members on a Mon-Fri schedule and a
 * handful of bookings in June 2026 (plus one in May for the "previous period" comparison).
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(TestcontainersConfiguration.class)
class ReportIntegrationTest {

  private static final String PASSWORD = "Password123!";
  private static final ZoneId BERLIN = ZoneId.of("Europe/Berlin");
  private static final String PERIOD = "from=2026-06-01&to=2026-06-30";
  private static final long HAIRCUT_PRICE = 2500;

  @LocalServerPort private int port;
  @Autowired private UserRepository userRepository;
  @Autowired private SalonRepository salonRepository;
  @Autowired private SalonServiceRepository salonServiceRepository;
  @Autowired private StaffRepository staffRepository;
  @Autowired private StaffShiftRepository staffShiftRepository;
  @Autowired private BookingRepository bookingRepository;
  @MockitoBean private JavaMailSender mailSender;

  private RestClient client;
  private long suffix;
  private String ownerToken;
  private Salon salon;
  private Staff anna;
  private Staff ben;
  private String reportsPath;

  @BeforeEach
  void seedSalonStaffAndBookings() {
    client =
        RestClient.builder()
            .baseUrl("http://localhost:" + port + "/api/v1")
            .defaultStatusHandler(status -> true, (req, res) -> {})
            .build();
    suffix = System.nanoTime();
    User owner = registerUser("owner-" + suffix + "@example.com", Role.SALON_OWNER);
    ownerToken = login(owner.getEmail());
    salon = createActiveSalon(owner, "Report Salon " + suffix, "report-salon-" + suffix);
    reportsPath = "/admin/salons/" + salon.getId() + "/reports";
    SalonService haircut =
        salonServiceRepository.save(SalonService.create(salon, "Haircut", 30, HAIRCUT_PRICE));
    anna = staffRepository.save(Staff.create(salon, "Anna"));
    ben = staffRepository.save(Staff.create(salon, "Ben"));
    for (Staff staff : new Staff[] {anna, ben}) {
      for (DayOfWeek day : DayOfWeek.values()) {
        if (day.getValue() <= DayOfWeek.FRIDAY.getValue()) {
          staffShiftRepository.save(
              StaffShift.of(staff, day, LocalTime.of(9, 0), LocalTime.of(17, 0)));
        }
      }
    }
    User carla = registerUser("carla-" + suffix + "@example.com", Role.CUSTOMER);
    User dan = registerUser("dan-" + suffix + "@example.com", Role.CUSTOMER);

    // previous period (May): Carla is therefore not a new customer in June
    booking(carla, anna, haircut, "2026-05-20T10:00", BookingStatus.COMPLETED, PaymentMethod.CASH);
    booking(carla, anna, haircut, "2026-06-02T10:00", BookingStatus.COMPLETED, PaymentMethod.CASH);
    booking(dan, anna, haircut, "2026-06-10T10:00", BookingStatus.COMPLETED, PaymentMethod.STRIPE);
    booking(dan, ben, haircut, "2026-06-15T10:00", BookingStatus.CANCELLED, null);
    booking(carla, ben, haircut, "2026-06-20T10:00", BookingStatus.NO_SHOW, PaymentMethod.CASH);
  }

  @Test
  void dashboardComparesPeriodWithPreviousOne() {
    JsonNode data = ownerGet("/dashboard?" + PERIOD).get("data");
    assertThat(data.get("currency").asText()).isEqualTo("EUR");
    assertThat(data.get("period").get("timezone").asText()).isEqualTo("Europe/Berlin");

    JsonNode current = data.get("current");
    assertThat(current.get("revenueMinor").asLong()).isEqualTo(2 * HAIRCUT_PRICE);
    assertThat(current.get("bookingsCount").asLong()).isEqualTo(4);
    assertThat(current.get("completedCount").asLong()).isEqualTo(2);
    assertThat(current.get("cancelledCount").asLong()).isEqualTo(1);
    assertThat(current.get("noShowCount").asLong()).isEqualTo(1);
    assertThat(current.get("newCustomers").asLong()).isEqualTo(1);
    assertThat(current.get("averageTicketMinor").asLong()).isEqualTo(HAIRCUT_PRICE);
    // 60 booked minutes over 2 staff x 22 weekdays x 480 minutes
    assertThat(current.get("occupancyPercent").asDouble()).isEqualTo(0.3);

    JsonNode previous = data.get("previous");
    assertThat(previous.get("revenueMinor").asLong()).isEqualTo(HAIRCUT_PRICE);
    assertThat(previous.get("bookingsCount").asLong()).isEqualTo(1);
    assertThat(data.get("todayBookings").isArray()).isTrue();
  }

  @Test
  void revenueSeriesAndBreakdowns() {
    JsonNode monthly = ownerGet("/revenue?" + PERIOD + "&granularity=MONTH").get("data");
    assertThat(monthly.get("totalRevenueMinor").asLong()).isEqualTo(2 * HAIRCUT_PRICE);
    assertThat(monthly.get("series")).hasSize(1);
    assertThat(monthly.get("series").get(0).get("periodStart").asText()).isEqualTo("2026-06-01");
    assertThat(monthly.get("series").get(0).get("bookings").asLong()).isEqualTo(2);
    assertThat(monthly.get("byPaymentMethod")).hasSize(2);
    assertThat(monthly.get("byService").get(0).get("serviceName").asText()).isEqualTo("Haircut");
    assertThat(monthly.get("byService").get(0).get("revenueMinor").asLong())
        .isEqualTo(2 * HAIRCUT_PRICE);

    JsonNode daily = ownerGet("/revenue?" + PERIOD + "&granularity=DAY").get("data");
    assertThat(daily.get("series")).hasSize(30);
    long seriesTotal = 0;
    for (JsonNode point : daily.get("series")) {
      seriesTotal += point.get("revenueMinor").asLong();
    }
    assertThat(seriesTotal).isEqualTo(2 * HAIRCUT_PRICE);

    JsonNode bookings = ownerGet("/bookings?" + PERIOD + "&granularity=WEEK").get("data");
    assertThat(bookings.get("totals").get("COMPLETED").asLong()).isEqualTo(2);
    assertThat(bookings.get("totals").get("CANCELLED").asLong()).isEqualTo(1);
    assertThat(bookings.get("series").get(0).get("periodStart").asText()).isEqualTo("2026-06-01");
  }

  @Test
  void staffPerformanceRanksByRevenue() {
    JsonNode staff = ownerGet("/staff-performance?" + PERIOD).get("data").get("staff");
    assertThat(staff).hasSize(2);
    JsonNode first = staff.get(0);
    assertThat(first.get("staffId").asLong()).isEqualTo(anna.getId());
    assertThat(first.get("bookings").asLong()).isEqualTo(2);
    assertThat(first.get("completed").asLong()).isEqualTo(2);
    assertThat(first.get("revenueMinor").asLong()).isEqualTo(2 * HAIRCUT_PRICE);
    assertThat(first.get("utilisationPercent").asDouble()).isGreaterThan(0);
    JsonNode second = staff.get(1);
    assertThat(second.get("staffId").asLong()).isEqualTo(ben.getId());
    assertThat(second.get("noShows").asLong()).isEqualTo(1);
    assertThat(second.get("revenueMinor").asLong()).isZero();
  }

  @Test
  void exportsAreDownloadedAsAttachments() {
    ResponseEntity<byte[]> excel = download("type=excel&report=revenue");
    assertThat(excel.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(excel.getHeaders().getContentDisposition().getFilename())
        .isEqualTo("revenue-report.xlsx");
    assertThat(excel.getBody()).isNotEmpty();

    ResponseEntity<byte[]> pdf = download("type=pdf&report=staff");
    assertThat(pdf.getHeaders().getContentType().toString()).isEqualTo("application/pdf");
    assertThat(new String(pdf.getBody(), 0, 5)).isEqualTo("%PDF-");
  }

  @Test
  void invalidRangeAndForeignOwnerAreRejected() {
    JsonNode reversed = ownerGet("/dashboard?from=2026-06-30&to=2026-06-01");
    assertThat(reversed.get("code").asInt()).isEqualTo(3007);

    User intruder = registerUser("intruder-" + suffix + "@example.com", Role.SALON_OWNER);
    ResponseEntity<JsonNode> forbidden =
        client
            .get()
            .uri(reportsPath + "/dashboard")
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + login(intruder.getEmail()))
            .retrieve()
            .toEntity(JsonNode.class);
    assertThat(forbidden.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    assertThat(forbidden.getBody().get("code").asInt()).isEqualTo(1002);
  }

  // ---------------------------------------------------------------------------------------------

  private void booking(
      User customer,
      Staff staff,
      SalonService service,
      String localStart,
      BookingStatus status,
      PaymentMethod method) {
    LocalDateTime start = LocalDateTime.parse(localStart);
    Booking booking =
        Booking.create(
            "R" + (suffix % 100_000_000) + bookingRepository.count(),
            salon,
            customer,
            staff,
            start.atZone(BERLIN).toInstant(),
            start.plusMinutes(service.getDurationMin()).atZone(BERLIN).toInstant());
    booking.addItem(BookingItem.from(service));
    booking.setStatus(status);
    booking.setPaymentMethod(method);
    bookingRepository.save(booking);
  }

  private JsonNode ownerGet(String pathAndQuery) {
    return client
        .get()
        .uri(reportsPath + pathAndQuery)
        .header(HttpHeaders.AUTHORIZATION, "Bearer " + ownerToken)
        .retrieve()
        .body(JsonNode.class);
  }

  private ResponseEntity<byte[]> download(String query) {
    return client
        .get()
        .uri(reportsPath + "/export?" + PERIOD + "&" + query)
        .header(HttpHeaders.AUTHORIZATION, "Bearer " + ownerToken)
        .retrieve()
        .toEntity(byte[].class);
  }

  /** Registers through the API, then sets the role directly. */
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

  private Salon createActiveSalon(User owner, String name, String slug) {
    Salon salon = Salon.create(owner, name, slug);
    salon.setAddress("1 Main Street");
    salon.setCity("Berlin");
    salon.setCountry("DE");
    salon.setStatus(SalonStatus.ACTIVE);
    return salonRepository.save(salon);
  }
}
