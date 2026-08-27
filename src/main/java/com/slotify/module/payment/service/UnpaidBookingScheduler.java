package com.slotify.module.payment.service;

import com.slotify.config.AppProperties;
import com.slotify.module.booking.entity.Booking;
import com.slotify.module.booking.entity.BookingStatus;
import com.slotify.module.booking.entity.CancelledBy;
import com.slotify.module.booking.entity.PaymentStatus;
import com.slotify.module.booking.repository.BookingRepository;
import com.slotify.module.booking.service.BookingEvents;
import com.slotify.module.notification.entity.NotificationType;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Releases slots held by bookings whose online deposit was never paid: bookings with an online
 * payment method, still UNPAID after {@code app.booking.unpaid-timeout}, at salons that require a
 * deposit, are cancelled by the system.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class UnpaidBookingScheduler {

  private final BookingRepository bookingRepository;
  private final BookingEvents events;
  private final AppProperties properties;
  private final Clock clock;

  @Scheduled(fixedDelayString = "PT1M", initialDelayString = "PT1M")
  @Transactional
  public void cancelUnpaidDeposits() {
    Instant cutoff = Instant.now(clock).minus(properties.booking().unpaidTimeout());
    List<Booking> stale = bookingRepository.findUnpaidOnlineBookingsCreatedBefore(cutoff);
    for (Booking booking : stale) {
      if (!booking.getSalon().getSettings().isRequireDeposit()
          || booking.getPaymentStatus() != PaymentStatus.UNPAID
          || !booking.getStatus().canTransitionTo(BookingStatus.CANCELLED)) {
        continue;
      }
      booking.transitionTo(BookingStatus.CANCELLED);
      booking.setCancelledBy(CancelledBy.SYSTEM);
      booking.setCancelReason("Deposit not paid in time");
      events.statusChanged(booking, NotificationType.BOOKING_CANCELLED);
      log.info("Auto-cancelled unpaid booking {}", booking.getCode());
    }
  }
}
