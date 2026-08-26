package com.slotify.module.booking.service;

import com.slotify.module.booking.dto.BookingResponse;
import com.slotify.module.booking.entity.Booking;
import com.slotify.module.booking.entity.BookingStatus;
import com.slotify.module.booking.mapper.BookingMapper;
import com.slotify.module.notification.entity.NotificationType;
import com.slotify.module.notification.service.NotificationService;
import com.slotify.module.realtime.RealtimePublisher;
import com.slotify.module.user.entity.User;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.Locale;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Side effects of booking changes: in-app/push notifications, real-time STOMP events and cache
 * eviction. Kept out of {@code BookingService} so the business rules stay readable.
 */
@Component
@RequiredArgsConstructor
public class BookingEvents {

  static final String QUEUE_BOOKINGS = "bookings";

  private final NotificationService notificationService;
  private final RealtimePublisher realtimePublisher;
  private final AvailabilityCache availabilityCache;
  private final BookingMapper mapper;

  /** Called after a booking was created (customer or walk-in). */
  public void created(Booking booking) {
    evict(booking);
    NotificationType customerType =
        booking.getStatus() == BookingStatus.CONFIRMED
            ? NotificationType.BOOKING_CONFIRMED
            : NotificationType.BOOKING_CREATED;
    notifyCustomer(booking, customerType);
    notifyOwner(booking, NotificationType.BOOKING_CREATED);
    publish(booking, "BOOKING_CREATED");
  }

  /** Called after a status transition. */
  public void statusChanged(Booking booking, NotificationType customerType) {
    evict(booking);
    if (customerType != null) {
      notifyCustomer(booking, customerType);
    }
    publish(booking, "BOOKING_" + booking.getStatus().name());
  }

  /** Called after the start time or staff changed. */
  public void rescheduled(Booking booking) {
    evict(booking);
    notifyCustomer(booking, NotificationType.BOOKING_RESCHEDULED);
    publish(booking, "BOOKING_RESCHEDULED");
  }

  private void notifyCustomer(Booking booking, NotificationType type) {
    notificationService.notify(
        booking.getCustomer(), type, deepLink(booking), args(booking, booking.getCustomer()));
  }

  private void notifyOwner(Booking booking, NotificationType type) {
    User owner = booking.getSalon().getOwner();
    notificationService.notify(owner, type, deepLink(booking), args(booking, owner));
  }

  private void publish(Booking booking, String type) {
    BookingResponse salonView = mapper.forSalon(booking);
    realtimePublisher.publishToSalon(booking.getSalon().getId(), type, salonView);
    realtimePublisher.publishToUser(
        booking.getCustomer().getId(), QUEUE_BOOKINGS, type, mapper.forCustomer(booking));
  }

  private void evict(Booking booking) {
    availabilityCache.evict(
        booking.getSalon().getId(),
        booking.getStartAt().atZone(booking.getSalon().zoneId()).toLocalDate());
  }

  private static Map<String, Object> deepLink(Booking booking) {
    return Map.of("bookingId", booking.getId(), "salonId", booking.getSalon().getId());
  }

  /** Message arguments: {0} code, {1} salon, {2} local date/time, {3} customer name. */
  private static Object[] args(Booking booking, User recipient) {
    Locale locale = Locale.forLanguageTag(recipient.getLocale());
    String when =
        booking
            .getStartAt()
            .atZone(booking.getSalon().zoneId())
            .format(
                DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM, FormatStyle.SHORT)
                    .withLocale(locale));
    return new Object[] {
      booking.getCode(), booking.getSalon().getName(), when, booking.getCustomer().getFullName()
    };
  }
}
