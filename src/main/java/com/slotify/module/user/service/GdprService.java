package com.slotify.module.user.service;

import com.slotify.common.exception.AppException;
import com.slotify.common.exception.ErrorCode;
import com.slotify.module.audit.entity.AuditAction;
import com.slotify.module.audit.service.AuditService;
import com.slotify.module.auth.repository.EmailVerificationTokenRepository;
import com.slotify.module.auth.repository.PasswordResetTokenRepository;
import com.slotify.module.auth.repository.RefreshTokenRepository;
import com.slotify.module.booking.entity.Booking;
import com.slotify.module.booking.repository.BookingRepository;
import com.slotify.module.notification.repository.NotificationRepository;
import com.slotify.module.salon.entity.SalonStatus;
import com.slotify.module.salon.repository.FavoriteRepository;
import com.slotify.module.salon.repository.SalonRepository;
import com.slotify.module.staff.repository.StaffRepository;
import com.slotify.module.user.dto.DeleteAccountRequest;
import com.slotify.module.user.dto.UserDataExport;
import com.slotify.module.user.entity.User;
import com.slotify.module.user.entity.UserStatus;
import com.slotify.module.user.mapper.UserMapper;
import com.slotify.module.user.repository.DeviceTokenRepository;
import com.slotify.security.UserPrincipal;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * GDPR rights of the signed-in user: data export (right of access) and account deletion (right to
 * erasure).
 *
 * <p>Deletion anonymises rather than removes the row: bookings and payments are financial records
 * that must be kept, so they keep pointing at a user whose personal data has been blanked out.
 * Everything that is purely personal (tokens, devices, favourites, notifications) is deleted.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class GdprService {

  /** Display name of anonymised accounts. */
  public static final String DELETED_USER_NAME = "Deleted user";

  /** Email domain of anonymised accounts (reserved, never routable). */
  public static final String ANONYMISED_EMAIL_DOMAIN = "anonymized.local";

  /** Statuses that make a salon "live" and therefore block the owner's deletion. */
  private static final List<SalonStatus> BLOCKING_SALON_STATUSES =
      List.of(SalonStatus.ACTIVE, SalonStatus.PENDING);

  private final UserService userService;
  private final UserMapper userMapper;
  private final PasswordEncoder passwordEncoder;
  private final DeviceTokenRepository deviceTokenRepository;
  private final RefreshTokenRepository refreshTokenRepository;
  private final EmailVerificationTokenRepository emailVerificationTokenRepository;
  private final PasswordResetTokenRepository passwordResetTokenRepository;
  private final FavoriteRepository favoriteRepository;
  private final NotificationRepository notificationRepository;
  private final BookingRepository bookingRepository;
  private final SalonRepository salonRepository;
  private final StaffRepository staffRepository;
  private final AuditService auditService;
  private final Clock clock;

  /** Collects every piece of personal data of the user into one document. */
  @Transactional(readOnly = true)
  public UserDataExport export(Long userId) {
    User user = userService.getById(userId);
    return new UserDataExport(
        Instant.now(clock),
        userMapper.toResponse(user),
        deviceTokenRepository.findAllByUserId(userId).stream()
            .map(t -> new UserDataExport.Device(t.getPlatform(), t.getAppType()))
            .toList(),
        favoriteRepository.findSalonsByUserId(userId).stream()
            .map(s -> new UserDataExport.FavoriteSalon(s.getId(), s.getName()))
            .toList(),
        notificationRepository.findAllByUserIdOrderByCreatedAtDesc(userId).stream()
            .map(
                n ->
                    new UserDataExport.NotificationEntry(
                        n.getType(), n.getTitle(), n.getBody(), n.getReadAt(), n.getCreatedAt()))
            .toList(),
        bookingRepository.findAllByCustomerIdOrderByStartAtDesc(userId).stream()
            .map(GdprService::toBookingEntry)
            .toList());
  }

  /**
   * Anonymises the account and removes its personal artefacts.
   *
   * @throws AppException {@link ErrorCode#INVALID_CREDENTIALS} when the password of a local account
   *     is missing or wrong; {@link ErrorCode#OWNER_HAS_SALONS} when the user still owns a live
   *     salon
   */
  public void deleteAccount(UserPrincipal principal, DeleteAccountRequest request) {
    Long userId = principal.id();
    User user = userService.getById(userId);
    assertPasswordConfirmed(user, request);
    if (salonRepository.existsByOwnerIdAndStatusIn(userId, BLOCKING_SALON_STATUSES)) {
      throw new AppException(ErrorCode.OWNER_HAS_SALONS);
    }

    deviceTokenRepository.deleteAllByUserId(userId);
    refreshTokenRepository.deleteAllByUserId(userId);
    emailVerificationTokenRepository.deleteAllByUserId(userId);
    passwordResetTokenRepository.deleteAllByUserId(userId);
    favoriteRepository.deleteAllByIdUserId(userId);
    notificationRepository.deleteAllByUserId(userId);
    staffRepository.findByUserId(userId).ifPresent(staff -> staff.setUser(null));

    anonymise(user);
    auditService.record(principal, null, AuditAction.USER_DELETED, "User", userId, null);
  }

  private void assertPasswordConfirmed(User user, DeleteAccountRequest request) {
    if (!user.hasPassword()) {
      return; // social login: the valid access token is the proof of identity
    }
    String password = request == null ? null : request.password();
    if (password == null || !passwordEncoder.matches(password, user.getPasswordHash())) {
      throw new AppException(ErrorCode.INVALID_CREDENTIALS);
    }
  }

  /**
   * Blanks every personal field; the id stays so that bookings keep a valid customer.
   *
   * <p>{@code deleted_at} is deliberately left {@code null}: {@code User} carries an entity-level
   * {@code @SQLRestriction("deleted_at IS NULL")}, so a soft-deleted row would vanish from every
   * association (bookings, payments) and break the salon's history views. {@link
   * UserStatus#DELETED} is the marker of an anonymised account and blocks sign-in.
   */
  private void anonymise(User user) {
    user.setFullName(DELETED_USER_NAME);
    user.setEmail("deleted-" + user.getId() + "@" + ANONYMISED_EMAIL_DOMAIN);
    user.setPhone(null);
    user.setAvatarUrl(null);
    user.setPasswordHash(null);
    user.setProviderId(null);
    user.setEmailVerified(false);
    user.setStatus(UserStatus.DELETED);
  }

  private static UserDataExport.BookingEntry toBookingEntry(Booking booking) {
    return new UserDataExport.BookingEntry(
        booking.getCode(),
        booking.getSalon().getName(),
        booking.getItems().stream()
            .map(
                i ->
                    new UserDataExport.BookingItemEntry(
                        i.getServiceName(), i.getDurationMin(), i.getPriceMinor()))
            .toList(),
        booking.getStartAt(),
        booking.getEndAt(),
        booking.getStatus(),
        booking.getSubtotalMinor(),
        booking.getDiscountMinor(),
        booking.getTotalMinor(),
        booking.getCurrency());
  }
}
