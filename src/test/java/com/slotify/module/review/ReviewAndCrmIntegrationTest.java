package com.slotify.module.review;

import static org.assertj.core.api.Assertions.assertThat;

import com.slotify.TestcontainersConfiguration;
import com.slotify.module.booking.entity.Booking;
import com.slotify.module.booking.entity.BookingItem;
import com.slotify.module.booking.entity.BookingStatus;
import com.slotify.module.booking.repository.BookingRepository;
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
import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
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
import org.springframework.http.HttpMethod;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.JsonNode;

/**
 * Review lifecycle (create → rating aggregate → public list → reply → hide) and CRM (customer list
 * with statistics, tags, notes, tenant isolation) on top of a completed booking seeded directly
 * through the repositories.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(TestcontainersConfiguration.class)
class ReviewAndCrmIntegrationTest {

  private static final String PASSWORD = "Password123!";
  private static final long HAIRCUT_PRICE_MINOR = 2500;

  @LocalServerPort private int port;
  @Autowired private UserRepository userRepository;
  @Autowired private SalonRepository salonRepository;
  @Autowired private SalonServiceRepository serviceRepository;
  @Autowired private StaffRepository staffRepository;
  @Autowired private StaffShiftRepository shiftRepository;
  @Autowired private BookingRepository bookingRepository;
  @Autowired private PasswordEncoder passwordEncoder;
  @MockitoBean private JavaMailSender mailSender;

  private long suffix;
  private Salon salon;
  private SalonService haircut;
  private Staff stylist;
  private User customer;
  private Booking completed;
  private String ownerToken;
  private String otherOwnerToken;
  private String customerToken;

  @BeforeEach
  void setUp() {
    suffix = System.nanoTime();
    User owner = user("owner-" + suffix, "Olivia Owner", Role.SALON_OWNER);
    User otherOwner = user("other-" + suffix, "Oscar Other", Role.SALON_OWNER);
    customer = user("customer-" + suffix, "Chris Customer", Role.CUSTOMER);
    salon = activeSalon(owner);
    haircut =
        serviceRepository.save(SalonService.create(salon, "Haircut", 30, HAIRCUT_PRICE_MINOR));
    stylist = Staff.create(salon, "Sam");
    stylist.replaceServices(List.of(haircut));
    stylist = staffRepository.save(stylist);
    for (DayOfWeek d : DayOfWeek.values()) {
      shiftRepository.save(StaffShift.of(stylist, d, LocalTime.of(10, 0), LocalTime.of(16, 0)));
    }
    completed = booking(BookingStatus.COMPLETED, Instant.now().minus(2, ChronoUnit.DAYS));

    ownerToken = login(owner.getEmail());
    otherOwnerToken = login(otherOwner.getEmail());
    customerToken = login(customer.getEmail());
  }

  @Test
  void customerReviewsCompletedBookingAndSalonRatingIsAggregated() {
    Booking pending = booking(BookingStatus.CONFIRMED, Instant.now().plus(3, ChronoUnit.DAYS));
    assertThat(review(pending.getId(), 5, "too early").get("code").asInt()).isEqualTo(3008);

    JsonNode created = review(completed.getId(), 4, "Great cut");
    assertThat(created.get("code").asInt()).isEqualTo(1000);
    assertThat(created.get("data").get("rating").asInt()).isEqualTo(4);
    assertThat(created.get("data").get("customerName").asText()).isEqualTo("Chris Customer");
    assertThat(created.get("data").get("staff").get("displayName").asText()).isEqualTo("Sam");

    // one review per booking
    assertThat(review(completed.getId(), 5, "again").get("code").asInt()).isEqualTo(4007);

    // customer can read it back
    JsonNode mine = get("/bookings/" + completed.getId() + "/review", customerToken);
    assertThat(mine.get("data").get("comment").asText()).isEqualTo("Great cut");

    // denormalised rating columns of salon and staff
    Salon reloaded = salonRepository.findById(salon.getId()).orElseThrow();
    assertThat(reloaded.getRatingAvg()).isEqualByComparingTo(new BigDecimal("4.00"));
    assertThat(reloaded.getRatingCount()).isEqualTo(1);
    Staff staff = staffRepository.findById(stylist.getId()).orElseThrow();
    assertThat(staff.getRatingAvg()).isEqualByComparingTo(new BigDecimal("4.00"));
    assertThat(staff.getRatingCount()).isEqualTo(1);

    // public list shows the abbreviated name and the summary counts it
    JsonNode publicList = get("/salons/" + salon.getSlug() + "/reviews", null);
    assertThat(publicList.get("data").get("items")).hasSize(1);
    assertThat(publicList.get("data").get("items").get(0).get("customerName").asText())
        .isEqualTo("Chris C.");
    JsonNode summary = get("/salons/" + salon.getId() + "/reviews/summary", null).get("data");
    assertThat(summary.get("count").asLong()).isEqualTo(1);
    assertThat(summary.get("average").decimalValue()).isEqualByComparingTo("4.00");
    assertThat(summary.get("distribution").get("4").asLong()).isEqualTo(1);
    assertThat(summary.get("distribution").get("5").asLong()).isZero();
  }

  @Test
  void ownerRepliesHidesAndOtherOwnersAreRejected() {
    long reviewId = review(completed.getId(), 5, "Loved it").get("data").get("id").asLong();
    String base = "/admin/salons/" + salon.getId() + "/reviews/" + reviewId;

    JsonNode replied = post(base + "/reply", ownerToken, Map.of("reply", "Thank you!"));
    assertThat(replied.get("code").asInt()).isEqualTo(1000);
    assertThat(replied.get("data").get("reply").asText()).isEqualTo("Thank you!");
    assertThat(replied.get("data").get("repliedAt").isNull()).isFalse();

    // the reply is visible publicly
    JsonNode item = get("/salons/" + salon.getId() + "/reviews", null).get("data").get("items");
    assertThat(item.get(0).get("reply").asText()).isEqualTo("Thank you!");

    // another owner cannot touch it
    assertThat(post(base + "/reply", otherOwnerToken, Map.of("reply", "x")).get("code").asInt())
        .isEqualTo(1002);
    assertThat(
            get("/admin/salons/" + salon.getId() + "/reviews", otherOwnerToken).get("code").asInt())
        .isEqualTo(1002);

    // hiding removes it from the public list and the aggregates
    JsonNode hidden = put(base + "/visibility", ownerToken, Map.of("visible", false));
    assertThat(hidden.get("data").get("visible").asBoolean()).isFalse();
    assertThat(get("/salons/" + salon.getId() + "/reviews", null).get("data").get("items"))
        .isEmpty();
    JsonNode summary = get("/salons/" + salon.getId() + "/reviews/summary", null).get("data");
    assertThat(summary.get("count").asLong()).isZero();
    assertThat(salonRepository.findById(salon.getId()).orElseThrow().getRatingCount()).isZero();

    // the owner still sees it with the visibility filter
    JsonNode ownerList =
        get("/admin/salons/" + salon.getId() + "/reviews?visible=false", ownerToken);
    assertThat(ownerList.get("data").get("items")).hasSize(1);
    assertThat(
            get("/admin/salons/" + salon.getId() + "/reviews?visible=true", ownerToken)
                .get("data")
                .get("items"))
        .isEmpty();
  }

  @Test
  void crmListsCustomersWithStatisticsAndTags() {
    booking(BookingStatus.NO_SHOW, Instant.now().minus(10, ChronoUnit.DAYS));
    String base = "/admin/salons/" + salon.getId();

    JsonNode list = get(base + "/customers?q=chris", ownerToken);
    assertThat(list.get("code").asInt()).isEqualTo(1000);
    assertThat(list.get("data").get("items")).hasSize(1);
    JsonNode row = list.get("data").get("items").get(0);
    assertThat(row.get("id").asLong()).isEqualTo(customer.getId());
    assertThat(row.get("totalBookings").asLong()).isEqualTo(2);
    assertThat(row.get("completedBookings").asLong()).isEqualTo(1);
    assertThat(row.get("noShows").asLong()).isEqualTo(1);
    assertThat(row.get("totalSpentMinor").asLong()).isEqualTo(HAIRCUT_PRICE_MINOR);
    assertThat(row.get("lastVisitAt").asText()).isEqualTo(completed.getStartAt().toString());
    assertThat(row.get("tags")).isEmpty();
    assertThat(get(base + "/customers?q=nobody", ownerToken).get("data").get("items")).isEmpty();

    // tag catalogue + assignment
    JsonNode vip =
        post(base + "/customer-tags", ownerToken, Map.of("name", "VIP", "color", "#FFAA00"));
    assertThat(vip.get("code").asInt()).isEqualTo(1000);
    long vipId = vip.get("data").get("id").asLong();
    assertThat(post(base + "/customer-tags", ownerToken, Map.of("name", "vip")).get("code").asInt())
        .isEqualTo(4008);
    JsonNode assigned =
        put(
            base + "/customers/" + customer.getId() + "/tags",
            ownerToken,
            Map.of("tagIds", List.of(vipId)));
    assertThat(assigned.get("data").get(0).get("name").asText()).isEqualTo("VIP");

    JsonNode detail = get(base + "/customers/" + customer.getId(), ownerToken).get("data");
    assertThat(detail.get("customer").get("tags").get(0).get("color").asText())
        .isEqualTo("#FFAA00");
    assertThat(detail.get("customer").get("email").asText()).isEqualTo(customer.getEmail());
    assertThat(detail.get("recentBookings")).hasSize(2);
    assertThat(detail.get("recentBookings").get(0).get("status").asText()).isEqualTo("COMPLETED");

    // tenant isolation: other owner gets 1002, unknown customer 2002
    assertThat(get(base + "/customers", otherOwnerToken).get("code").asInt()).isEqualTo(1002);
    long strangerId = user("stranger-" + suffix, "Stan Stranger", Role.CUSTOMER).getId();
    assertThat(get(base + "/customers/" + strangerId, ownerToken).get("code").asInt())
        .isEqualTo(2002);
  }

  @Test
  void crmNotesCanBeAddedListedAndDeleted() {
    String notes = "/admin/salons/" + salon.getId() + "/customers/" + customer.getId() + "/notes";

    JsonNode added = post(notes, ownerToken, Map.of("note", "Prefers morning slots"));
    assertThat(added.get("code").asInt()).isEqualTo(1000);
    assertThat(added.get("data").get("authorName").asText()).isEqualTo("Olivia Owner");
    long noteId = added.get("data").get("id").asLong();

    assertThat(get(notes, ownerToken).get("data")).hasSize(1);
    assertThat(post(notes, otherOwnerToken, Map.of("note", "x")).get("code").asInt())
        .isEqualTo(1002);

    assertThat(delete(notes + "/" + noteId, ownerToken).get("code").asInt()).isEqualTo(1000);
    assertThat(get(notes, ownerToken).get("data")).isEmpty();
    assertThat(delete(notes + "/" + noteId, ownerToken).get("code").asInt()).isEqualTo(2001);
  }

  // ---- seeding helpers ------------------------------------------------------------------------

  private Salon activeSalon(User owner) {
    Salon s = Salon.create(owner, "Review Salon " + suffix, "review-salon-" + suffix);
    s.setAddress("Main St 1");
    s.setCity("Berlin");
    s.setCountry("DE");
    s.setStatus(SalonStatus.ACTIVE);
    s.replaceOpeningHours(
        Arrays.stream(DayOfWeek.values())
            .map(d -> SalonOpeningHour.of(d, LocalTime.of(9, 0), LocalTime.of(18, 0), false))
            .toList());
    return salonRepository.save(s);
  }

  private Booking booking(BookingStatus status, Instant startAt) {
    Booking b =
        Booking.create(
            "SLT-" + Long.toString(System.nanoTime(), 36).toUpperCase(),
            salon,
            customer,
            stylist,
            startAt,
            startAt.plus(30, ChronoUnit.MINUTES));
    b.addItem(BookingItem.from(haircut));
    b.setStatus(status);
    return bookingRepository.save(b);
  }

  private User user(String prefix, String fullName, Role role) {
    User u = User.local(prefix + "@example.com", passwordEncoder.encode(PASSWORD), fullName, role);
    u.setEmailVerified(true);
    return userRepository.save(u);
  }

  // ---- HTTP helpers ---------------------------------------------------------------------------

  private JsonNode review(long bookingId, int rating, String comment) {
    return post(
        "/bookings/" + bookingId + "/review",
        customerToken,
        Map.of("rating", rating, "comment", comment));
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
    return exchange(HttpMethod.GET, uri, token, null);
  }

  private JsonNode post(String uri, String token, Object body) {
    return exchange(HttpMethod.POST, uri, token, body);
  }

  private JsonNode put(String uri, String token, Object body) {
    return exchange(HttpMethod.PUT, uri, token, body);
  }

  private JsonNode delete(String uri, String token) {
    return exchange(HttpMethod.DELETE, uri, token, null);
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
}
