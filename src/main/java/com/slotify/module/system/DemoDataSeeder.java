package com.slotify.module.system;

import com.slotify.config.AppProperties;
import com.slotify.module.user.entity.Role;
import com.slotify.module.user.entity.User;
import com.slotify.module.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Inserts demo accounts (and, in later phases, salons, services, staff and bookings) when {@code
 * app.seed-demo-data=true} (profile {@code demo}). Idempotent: existing rows are left untouched.
 *
 * <p>Demo credentials (password {@value #DEMO_PASSWORD} for all):
 *
 * <ul>
 *   <li>admin@slotify.demo – SUPER_ADMIN
 *   <li>owner@slotify.demo – SALON_OWNER
 *   <li>staff@slotify.demo – STAFF
 *   <li>customer@slotify.demo – CUSTOMER
 * </ul>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DemoDataSeeder implements ApplicationRunner {

  /** Password shared by every demo account. */
  public static final String DEMO_PASSWORD = "Password123!";

  private final AppProperties properties;
  private final UserRepository userRepository;
  private final PasswordEncoder passwordEncoder;

  @Override
  @Transactional
  public void run(ApplicationArguments args) {
    if (!properties.seedDemoData()) {
      return;
    }
    log.info("Seeding demo data");
    seedUser("admin@slotify.demo", "Platform Admin", Role.SUPER_ADMIN);
    seedUser("owner@slotify.demo", "Olivia Owner", Role.SALON_OWNER);
    seedUser("staff@slotify.demo", "Sam Stylist", Role.STAFF);
    seedUser("customer@slotify.demo", "Chris Customer", Role.CUSTOMER);
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
}
