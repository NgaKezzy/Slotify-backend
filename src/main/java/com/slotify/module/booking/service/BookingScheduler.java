package com.slotify.module.booking.service;

import com.slotify.module.booking.entity.Booking;
import com.slotify.module.booking.entity.BookingStatus;
import com.slotify.module.booking.repository.BookingRepository;
import com.slotify.module.notification.entity.NotificationType;
import com.slotify.module.notification.service.NotificationService;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Background jobs around bookings.
 *
 * <ul>
 *   <li>Reminder push/in-app notification {@value #REMINDER_HOURS} hours before a confirmed booking
 *       (once per booking).
 *   <li>Auto-complete bookings the salon forgot to close, {@value #AUTO_COMPLETE_HOURS} hours after
 *       their end.
 * </ul>
 *
 * Unpaid-deposit auto-cancellation is added with the payment module (Phase 2).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class BookingScheduler {

  static final int REMINDER_HOURS = 24;
  static final int AUTO_COMPLETE_HOURS = 3;

  private final BookingRepository bookingRepository;
  private final NotificationService notificationService;
  private final Clock clock;

  /** Every 5 minutes: remind customers about bookings starting in ~24 h. */
  @Scheduled(fixedDelayString = "PT5M", initialDelayString = "PT1M")
  @Transactional
  public void sendReminders() {
    Instant now = Instant.now(clock);
    Instant windowEnd = now.plus(Duration.ofHours(REMINDER_HOURS));
    Instant windowStart = windowEnd.minus(Duration.ofMinutes(10));
    List<Booking> due =
        bookingRepository.findAllByStatusAndStartAtBetweenAndReminderSentAtIsNull(
            BookingStatus.CONFIRMED, windowStart, windowEnd);
    for (Booking booking : due) {
      Locale locale = Locale.forLanguageTag(booking.getCustomer().getLocale());
      String when =
          booking
              .getStartAt()
              .atZone(booking.getSalon().zoneId())
              .format(
                  DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM, FormatStyle.SHORT)
                      .withLocale(locale));
      notificationService.notify(
          booking.getCustomer(),
          NotificationType.BOOKING_REMINDER,
          Map.of("bookingId", booking.getId()),
          booking.getCode(),
          booking.getSalon().getName(),
          when,
          booking.getCustomer().getFullName());
      booking.setReminderSentAt(now);
    }
    if (!due.isEmpty()) {
      log.info("Sent {} booking reminders", due.size());
    }
  }

  /** Hourly: close bookings that ended hours ago without being completed. */
  @Scheduled(fixedDelayString = "PT1H", initialDelayString = "PT2M")
  @Transactional
  public void autoComplete() {
    Instant cutoff = Instant.now(clock).minus(Duration.ofHours(AUTO_COMPLETE_HOURS));
    List<Booking> stale =
        bookingRepository.findAllByStatusInAndEndAtBefore(
            List.of(BookingStatus.CONFIRMED, BookingStatus.IN_PROGRESS), cutoff);
    stale.forEach(b -> b.transitionTo(BookingStatus.COMPLETED));
    if (!stale.isEmpty()) {
      log.info("Auto-completed {} bookings", stale.size());
    }
  }
}
