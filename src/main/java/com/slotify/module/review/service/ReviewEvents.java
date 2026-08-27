package com.slotify.module.review.service;

import com.slotify.module.booking.entity.Booking;
import com.slotify.module.notification.entity.NotificationType;
import com.slotify.module.notification.service.NotificationService;
import com.slotify.module.review.entity.Review;
import com.slotify.module.user.entity.User;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.Locale;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/** Side effects of review changes: notifies the salon owner about a new review. */
@Component
@RequiredArgsConstructor
public class ReviewEvents {

  private final NotificationService notificationService;

  /** Called after a customer wrote a review. */
  public void created(Review review) {
    Booking booking = review.getBooking();
    User owner = booking.getSalon().getOwner();
    Map<String, Object> data =
        Map.of(
            "reviewId", review.getId(),
            "bookingId", booking.getId(),
            "salonId", booking.getSalon().getId());
    notificationService.notify(owner, NotificationType.REVIEW_RECEIVED, data, args(booking, owner));
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
