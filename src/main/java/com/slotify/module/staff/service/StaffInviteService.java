package com.slotify.module.staff.service;

import com.slotify.common.exception.AppException;
import com.slotify.common.exception.ErrorCode;
import com.slotify.module.notification.service.MailService;
import com.slotify.module.salon.entity.Salon;
import com.slotify.module.staff.repository.StaffRepository;
import com.slotify.module.user.entity.Role;
import com.slotify.module.user.entity.User;
import com.slotify.module.user.repository.UserRepository;
import java.security.SecureRandom;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Creates or links the login account of an invited staff member and sends the invitation email.
 * Used by {@link StaffService#create} when {@code inviteEmail} is given.
 *
 * <p>Rules: a brand-new email gets a {@code STAFF} account with a random temporary password; an
 * existing {@code CUSTOMER} or {@code STAFF} account is linked and promoted to {@code STAFF}; owner
 * / admin accounts and accounts already linked to another staff row are rejected.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class StaffInviteService {

  private static final String PASSWORD_ALPHABET =
      "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz23456789";
  private static final int PASSWORD_LENGTH = 12;
  private static final Set<Role> LINKABLE_ROLES = Set.of(Role.CUSTOMER, Role.STAFF);

  private final UserRepository userRepository;
  private final StaffRepository staffRepository;
  private final PasswordEncoder passwordEncoder;
  private final MailService mailService;
  private final SecureRandom random = new SecureRandom();

  /**
   * Resolves the account for {@code email}, creating it when necessary, and emails the invitation.
   *
   * @param salon salon the person joins (named in the email)
   * @param email address to invite
   * @param fullName name used when a new account has to be created
   * @return the account to link to the staff row
   * @throws AppException {@link ErrorCode#EMAIL_ALREADY_USED} when the address belongs to an owner
   *     / admin or to an account that is already staff somewhere
   */
  public User invite(Salon salon, String email, String fullName) {
    return userRepository
        .findByEmailIgnoreCase(email)
        .map(existing -> linkExisting(salon, existing))
        .orElseGet(() -> createAccount(salon, email, fullName));
  }

  private User linkExisting(Salon salon, User existing) {
    if (!LINKABLE_ROLES.contains(existing.getRole())
        || staffRepository.findByUserId(existing.getId()).isPresent()) {
      throw new AppException(ErrorCode.EMAIL_ALREADY_USED);
    }
    existing.setRole(Role.STAFF);
    mailService.sendStaffInvite(existing, salon, null);
    return existing;
  }

  private User createAccount(Salon salon, String email, String fullName) {
    String rawPassword = generatePassword();
    User user = User.local(email, passwordEncoder.encode(rawPassword), fullName, Role.STAFF);
    // The owner vouches for the address, so the staff member can sign in right away.
    user.setEmailVerified(true);
    userRepository.save(user);
    mailService.sendStaffInvite(user, salon, rawPassword);
    return user;
  }

  private String generatePassword() {
    StringBuilder password = new StringBuilder(PASSWORD_LENGTH);
    for (int i = 0; i < PASSWORD_LENGTH; i++) {
      password.append(PASSWORD_ALPHABET.charAt(random.nextInt(PASSWORD_ALPHABET.length())));
    }
    return password.toString();
  }
}
