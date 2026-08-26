package com.slotify.module.booking;

import static org.assertj.core.api.Assertions.assertThat;

import com.slotify.module.booking.entity.BookingStatus;
import org.junit.jupiter.api.Test;

/** The state machine must match plan §3.5 exactly. */
class BookingStatusTest {

  @Test
  void pendingCanBeConfirmedRejectedOrCancelled() {
    assertThat(BookingStatus.PENDING.canTransitionTo(BookingStatus.CONFIRMED)).isTrue();
    assertThat(BookingStatus.PENDING.canTransitionTo(BookingStatus.REJECTED)).isTrue();
    assertThat(BookingStatus.PENDING.canTransitionTo(BookingStatus.CANCELLED)).isTrue();
    assertThat(BookingStatus.PENDING.canTransitionTo(BookingStatus.COMPLETED)).isFalse();
    assertThat(BookingStatus.PENDING.canTransitionTo(BookingStatus.NO_SHOW)).isFalse();
  }

  @Test
  void confirmedFlowsToInProgressCompletedCancelledOrNoShow() {
    assertThat(BookingStatus.CONFIRMED.canTransitionTo(BookingStatus.IN_PROGRESS)).isTrue();
    assertThat(BookingStatus.CONFIRMED.canTransitionTo(BookingStatus.COMPLETED)).isTrue();
    assertThat(BookingStatus.CONFIRMED.canTransitionTo(BookingStatus.CANCELLED)).isTrue();
    assertThat(BookingStatus.CONFIRMED.canTransitionTo(BookingStatus.NO_SHOW)).isTrue();
    assertThat(BookingStatus.CONFIRMED.canTransitionTo(BookingStatus.REJECTED)).isFalse();
    assertThat(BookingStatus.CONFIRMED.canTransitionTo(BookingStatus.PENDING)).isFalse();
  }

  @Test
  void terminalStatesAllowNothing() {
    for (BookingStatus terminal :
        new BookingStatus[] {
          BookingStatus.COMPLETED,
          BookingStatus.CANCELLED,
          BookingStatus.NO_SHOW,
          BookingStatus.REJECTED
        }) {
      assertThat(terminal.isFinal()).isTrue();
      for (BookingStatus target : BookingStatus.values()) {
        assertThat(terminal.canTransitionTo(target)).isFalse();
      }
    }
  }

  @Test
  void onlyLiveBookingsBlockTheCalendar() {
    assertThat(BookingStatus.BLOCKING)
        .containsExactlyInAnyOrder(
            BookingStatus.PENDING, BookingStatus.CONFIRMED, BookingStatus.IN_PROGRESS);
    assertThat(BookingStatus.CANCELLED.blocksCalendar()).isFalse();
  }
}
