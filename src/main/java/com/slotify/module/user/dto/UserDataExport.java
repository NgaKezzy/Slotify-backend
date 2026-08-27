package com.slotify.module.user.dto;

import com.slotify.module.booking.entity.BookingStatus;
import com.slotify.module.notification.entity.NotificationType;
import com.slotify.module.user.entity.DeviceToken;
import java.time.Instant;
import java.util.List;

/**
 * Everything the platform stores about one user, returned by {@code GET /me/export} (GDPR right of
 * access). Sensitive technical values (password hash, raw push tokens) are intentionally left out.
 *
 * <p>Reviews are not included yet: the review entity arrives with the review module in Phase 2 and
 * must be added here when it lands.
 *
 * @param generatedAt when the document was produced (UTC)
 * @param profile the account itself
 * @param devices registered push-notification devices (platform and app only)
 * @param favorites bookmarked salons
 * @param notifications in-app notifications received
 * @param bookings appointment history
 */
public record UserDataExport(
    Instant generatedAt,
    UserResponse profile,
    List<Device> devices,
    List<FavoriteSalon> favorites,
    List<NotificationEntry> notifications,
    List<BookingEntry> bookings) {

  /** One registered device (the FCM token itself is not exported). */
  public record Device(DeviceToken.Platform platform, DeviceToken.AppType appType) {}

  /** A bookmarked salon. */
  public record FavoriteSalon(Long salonId, String name) {}

  /** An in-app notification. */
  public record NotificationEntry(
      NotificationType type, String title, String body, Instant readAt, Instant createdAt) {}

  /** One appointment with its price snapshot. */
  public record BookingEntry(
      String code,
      String salonName,
      List<BookingItemEntry> items,
      Instant startAt,
      Instant endAt,
      BookingStatus status,
      long subtotalMinor,
      long discountMinor,
      long totalMinor,
      String currency) {}

  /** One service line of a booking. */
  public record BookingItemEntry(String serviceName, int durationMin, long priceMinor) {}
}
