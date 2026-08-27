package com.slotify.module.system;

import com.slotify.config.AppProperties;
import com.slotify.module.booking.entity.Booking;
import com.slotify.module.booking.entity.BookingItem;
import com.slotify.module.booking.entity.BookingStatus;
import com.slotify.module.booking.entity.PaymentMethod;
import com.slotify.module.booking.entity.PaymentStatus;
import com.slotify.module.booking.repository.BookingRepository;
import com.slotify.module.booking.service.BookingCodeGenerator;
import com.slotify.module.payment.entity.Payment;
import com.slotify.module.payment.entity.PaymentProvider;
import com.slotify.module.payment.entity.PaymentState;
import com.slotify.module.payment.entity.PaymentType;
import com.slotify.module.payment.repository.PaymentRepository;
import com.slotify.module.promotion.entity.Coupon;
import com.slotify.module.promotion.entity.CouponType;
import com.slotify.module.promotion.repository.CouponRepository;
import com.slotify.module.salon.entity.Amenity;
import com.slotify.module.salon.entity.Category;
import com.slotify.module.salon.entity.Salon;
import com.slotify.module.salon.entity.SalonOpeningHour;
import com.slotify.module.salon.entity.SalonStatus;
import com.slotify.module.salon.repository.AmenityRepository;
import com.slotify.module.salon.repository.CategoryRepository;
import com.slotify.module.salon.repository.SalonRepository;
import com.slotify.module.service.entity.SalonService;
import com.slotify.module.service.entity.ServiceCategory;
import com.slotify.module.service.repository.SalonServiceRepository;
import com.slotify.module.service.repository.ServiceCategoryRepository;
import com.slotify.module.staff.entity.Staff;
import com.slotify.module.staff.entity.StaffShift;
import com.slotify.module.staff.repository.StaffRepository;
import com.slotify.module.staff.repository.StaffShiftRepository;
import com.slotify.module.user.entity.Role;
import com.slotify.module.user.entity.User;
import com.slotify.module.user.repository.UserRepository;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Inserts demo data when {@code app.seed-demo-data=true} (profile {@code demo}). Idempotent: runs
 * only when no demo salon exists yet.
 *
 * <p>Demo credentials (password {@value #DEMO_PASSWORD} for all):
 *
 * <ul>
 *   <li>admin@slotify.demo – SUPER_ADMIN
 *   <li>owner@slotify.demo – SALON_OWNER of "Glow & Go" (Berlin) and "Serenity Spa" (Munich)
 *   <li>staff@slotify.demo – STAFF at Glow & Go
 *   <li>customer@slotify.demo – CUSTOMER
 * </ul>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DemoDataSeeder implements ApplicationRunner {

  /** Password shared by every demo account. */
  public static final String DEMO_PASSWORD = "Password123!";

  private static final String DEMO_SALON_SLUG = "glow-and-go-berlin";

  private final AppProperties properties;
  private final UserRepository userRepository;
  private final CategoryRepository categoryRepository;
  private final AmenityRepository amenityRepository;
  private final SalonRepository salonRepository;
  private final ServiceCategoryRepository serviceCategoryRepository;
  private final SalonServiceRepository serviceRepository;
  private final StaffRepository staffRepository;
  private final StaffShiftRepository shiftRepository;
  private final CouponRepository couponRepository;
  private final BookingRepository bookingRepository;
  private final PaymentRepository paymentRepository;
  private final BookingCodeGenerator codeGenerator;
  private final PasswordEncoder passwordEncoder;
  private final Clock clock;

  @Override
  @Transactional
  public void run(ApplicationArguments args) {
    if (!properties.seedDemoData()) {
      return;
    }
    User admin = seedUser("admin@slotify.demo", "Platform Admin", Role.SUPER_ADMIN);
    User owner = seedUser("owner@slotify.demo", "Olivia Owner", Role.SALON_OWNER);
    User staffUser = seedUser("staff@slotify.demo", "Sam Stylist", Role.STAFF);
    User customer = seedUser("customer@slotify.demo", "Chris Customer", Role.CUSTOMER);
    log.debug("Demo admin: {}", admin.getEmail());

    if (salonRepository.existsBySlug(DEMO_SALON_SLUG)) {
      return;
    }
    log.info("Seeding demo salons, services and staff");
    List<Category> categories = seedCategories();
    List<Amenity> amenities = seedAmenities();

    Salon glow =
        seedSalon(
            owner,
            new SalonSeed(
                "Glow & Go",
                DEMO_SALON_SLUG,
                "Kurfürstendamm 21",
                "Berlin",
                new BigDecimal("52.5034"),
                new BigDecimal("13.3300"),
                "Modern hair & nail studio in the heart of Berlin. Walk-ins welcome, bookings preferred.",
                categories.subList(0, 2),
                amenities));
    Salon serenity =
        seedSalon(
            owner,
            new SalonSeed(
                "Serenity Spa",
                "serenity-spa-munich",
                "Maximilianstraße 5",
                "München",
                new BigDecimal("48.1391"),
                new BigDecimal("11.5802"),
                "Massage, facials and wellness rituals to slow down.",
                categories.subList(2, 4),
                amenities.subList(0, 2)));

    seedGlowServicesAndStaff(glow, staffUser);
    seedSerenityServicesAndStaff(serenity);
    seedCoupons(glow, serenity);
    seedBookings(glow, customer);
  }

  private void seedCoupons(Salon glow, Salon serenity) {
    Coupon welcome = Coupon.create(glow, "WELCOME10", CouponType.PERCENT, 10);
    welcome.setMinOrderMinor(2000);
    welcome.setPerUserLimit(1);
    couponRepository.save(welcome);
    Coupon relax = Coupon.create(serenity, "RELAX15", CouponType.FIXED, 1500);
    relax.setMinOrderMinor(7000);
    relax.setUsageLimit(100);
    couponRepository.save(relax);
  }

  /**
   * A little history for the demo customer at Glow &amp; Go: three completed (paid in cash) visits
   * over the last weeks, one no-show, and one confirmed booking next week. Times are 11:00 local so
   * they fall inside every staff shift; existing rows are never overlapping because the seeder runs
   * once on an empty database.
   */
  private void seedBookings(Salon salon, User customer) {
    List<Staff> staff =
        staffRepository.findAllBySalonIdAndActiveTrueOrderByDisplayNameAsc(salon.getId());
    List<SalonService> services =
        serviceRepository.findAllBySalonIdOrderBySortOrderAscNameAsc(salon.getId());
    if (staff.isEmpty() || services.isEmpty()) {
      return;
    }
    Staff sam =
        staff.stream()
            .filter(s -> s.getDisplayName().startsWith("Sam"))
            .findFirst()
            .orElse(staff.getFirst());
    SalonService cut = services.getFirst();
    LocalDate today = LocalDate.now(clock.withZone(salon.zoneId()));

    seedBooking(
        salon, customer, sam, cut, weekday(today.minusDays(28)), BookingStatus.COMPLETED, true);
    seedBooking(
        salon, customer, sam, cut, weekday(today.minusDays(14)), BookingStatus.COMPLETED, true);
    seedBooking(
        salon, customer, sam, cut, weekday(today.minusDays(7)), BookingStatus.COMPLETED, true);
    seedBooking(
        salon, customer, sam, cut, weekday(today.minusDays(3)), BookingStatus.NO_SHOW, false);
    seedBooking(
        salon, customer, sam, cut, weekday(today.plusDays(7)), BookingStatus.CONFIRMED, false);
  }

  private void seedBooking(
      Salon salon,
      User customer,
      Staff staff,
      SalonService service,
      LocalDate date,
      BookingStatus status,
      boolean paid) {
    Instant start = date.atTime(11, 0).atZone(salon.zoneId()).toInstant();
    Instant end = start.plusSeconds(service.totalMinutes() * 60L);
    Booking booking = Booking.create(codeGenerator.next(), salon, customer, staff, start, end);
    booking.addItem(BookingItem.from(service));
    booking.setStatus(status);
    booking.setPaymentMethod(PaymentMethod.CASH);
    booking = bookingRepository.save(booking);
    if (paid) {
      Payment payment =
          Payment.create(booking, PaymentProvider.CASH, PaymentType.FULL, booking.getTotalMinor());
      payment.setProviderRef("cash-" + booking.getCode());
      payment.setStatus(PaymentState.SUCCEEDED);
      payment.setPaidAt(end);
      paymentRepository.save(payment);
      booking.setPaymentStatus(PaymentStatus.PAID);
    }
  }

  /** Moves weekend dates to the following Monday so the slot is inside Sam's Mon–Fri shift. */
  private static LocalDate weekday(LocalDate date) {
    return switch (date.getDayOfWeek()) {
      case SATURDAY -> date.plusDays(2);
      case SUNDAY -> date.plusDays(1);
      default -> date;
    };
  }

  private User seedUser(String email, String fullName, Role role) {
    return userRepository
        .findByEmailIgnoreCase(email)
        .orElseGet(
            () -> {
              User user = User.local(email, passwordEncoder.encode(DEMO_PASSWORD), fullName, role);
              user.setEmailVerified(true);
              log.info("Created demo user {} ({})", email, role);
              return userRepository.save(user);
            });
  }

  private List<Category> seedCategories() {
    if (categoryRepository.count() > 0) {
      return categoryRepository.findAllByOrderBySortOrderAscNameAsc();
    }
    return categoryRepository.saveAll(
        List.of(
            Category.of("Hair", null, 1),
            Category.of("Nails", null, 2),
            Category.of("Massage", null, 3),
            Category.of("Facial & Skin", null, 4),
            Category.of("Barber", null, 5),
            Category.of("Makeup", null, 6)));
  }

  private List<Amenity> seedAmenities() {
    if (amenityRepository.count() > 0) {
      return amenityRepository.findAllByOrderByNameAsc();
    }
    return amenityRepository.saveAll(
        List.of(
            Amenity.of("Free Wi-Fi", "wifi"),
            Amenity.of("Parking", "local_parking"),
            Amenity.of("Wheelchair accessible", "accessible"),
            Amenity.of("Card payment", "credit_card")));
  }

  /** Static description of a demo salon. */
  private record SalonSeed(
      String name,
      String slug,
      String address,
      String city,
      BigDecimal lat,
      BigDecimal lng,
      String description,
      List<Category> categories,
      List<Amenity> amenities) {}

  private Salon seedSalon(User owner, SalonSeed seed) {
    Salon salon = Salon.create(owner, seed.name(), seed.slug());
    salon.setAddress(seed.address());
    salon.setCity(seed.city());
    salon.setCountry("DE");
    salon.setLat(seed.lat());
    salon.setLng(seed.lng());
    salon.setDescription(seed.description());
    salon.setPhone("+49 30 1234567");
    salon.setEmail(seed.slug() + "@slotify.demo");
    salon.setStatus(SalonStatus.ACTIVE);
    salon.setCategories(new LinkedHashSet<>(seed.categories()));
    salon.setAmenities(new LinkedHashSet<>(seed.amenities()));
    salon.replaceOpeningHours(
        Arrays.stream(DayOfWeek.values())
            .map(
                day ->
                    day == DayOfWeek.SUNDAY
                        ? SalonOpeningHour.of(day, null, null, true)
                        : SalonOpeningHour.of(day, LocalTime.of(9, 0), LocalTime.of(19, 0), false))
            .toList());
    return salonRepository.save(salon);
  }

  private void seedGlowServicesAndStaff(Salon salon, User staffUser) {
    ServiceCategory hair = serviceCategoryRepository.save(ServiceCategory.of(salon, "Hair", 1));
    ServiceCategory nails = serviceCategoryRepository.save(ServiceCategory.of(salon, "Nails", 2));
    SalonService cut = service(salon, hair, "Women's haircut", 45, 10, 4500, 1);
    SalonService mensCut = service(salon, hair, "Men's haircut", 30, 5, 2800, 2);
    SalonService color = service(salon, hair, "Colour & blow-dry", 90, 15, 9500, 3);
    SalonService manicure = service(salon, nails, "Classic manicure", 40, 5, 3200, 4);
    SalonService gel = service(salon, nails, "Gel nails", 60, 10, 5500, 5);

    Staff sam = Staff.create(salon, "Sam Stylist");
    sam.setUser(staffUser);
    sam.setTitle("Senior Stylist");
    sam.setBio("10 years of experience with cuts and colour.");
    sam.replaceServices(List.of(cut, mensCut, color));
    staffRepository.save(sam);
    weeklyShift(sam, LocalTime.of(9, 0), LocalTime.of(17, 0), DayOfWeek.MONDAY, DayOfWeek.FRIDAY);

    Staff mia = Staff.create(salon, "Mia Nails");
    mia.setTitle("Nail Artist");
    mia.replaceServices(List.of(manicure, gel));
    staffRepository.save(mia);
    weeklyShift(
        mia, LocalTime.of(11, 0), LocalTime.of(19, 0), DayOfWeek.TUESDAY, DayOfWeek.SATURDAY);

    Staff leo = Staff.create(salon, "Leo Barber");
    leo.setTitle("Barber");
    leo.replaceServices(List.of(mensCut, cut));
    staffRepository.save(leo);
    weeklyShift(
        leo, LocalTime.of(10, 0), LocalTime.of(18, 0), DayOfWeek.MONDAY, DayOfWeek.SATURDAY);
  }

  private void seedSerenityServicesAndStaff(Salon salon) {
    ServiceCategory massage =
        serviceCategoryRepository.save(ServiceCategory.of(salon, "Massage", 1));
    ServiceCategory skin = serviceCategoryRepository.save(ServiceCategory.of(salon, "Facials", 2));
    SalonService swedish = service(salon, massage, "Swedish massage", 60, 15, 7500, 1);
    SalonService deep = service(salon, massage, "Deep tissue massage", 90, 15, 10500, 2);
    SalonService facial = service(salon, skin, "Hydrating facial", 50, 10, 6900, 3);

    Staff anna = Staff.create(salon, "Anna Therapist");
    anna.setTitle("Massage Therapist");
    anna.replaceServices(List.of(swedish, deep));
    staffRepository.save(anna);
    weeklyShift(
        anna, LocalTime.of(9, 0), LocalTime.of(18, 0), DayOfWeek.MONDAY, DayOfWeek.SATURDAY);

    Staff nora = Staff.create(salon, "Nora Skin");
    nora.setTitle("Aesthetician");
    nora.replaceServices(List.of(facial, swedish));
    staffRepository.save(nora);
    weeklyShift(
        nora, LocalTime.of(12, 0), LocalTime.of(19, 0), DayOfWeek.WEDNESDAY, DayOfWeek.SATURDAY);
  }

  private SalonService service(
      Salon salon,
      ServiceCategory category,
      String name,
      int duration,
      int buffer,
      long price,
      int order) {
    SalonService service = SalonService.create(salon, name, duration, price);
    service.setCategory(category);
    service.setBufferAfterMin(buffer);
    service.setSortOrder(order);
    return serviceRepository.save(service);
  }

  private void weeklyShift(
      Staff staff, LocalTime from, LocalTime to, DayOfWeek first, DayOfWeek last) {
    for (DayOfWeek day : DayOfWeek.values()) {
      if (day.getValue() >= first.getValue() && day.getValue() <= last.getValue()) {
        shiftRepository.save(StaffShift.of(staff, day, from, to));
      }
    }
  }
}
