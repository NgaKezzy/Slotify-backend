package com.slotify.module.booking;

import static org.assertj.core.api.Assertions.assertThat;

import com.slotify.module.booking.service.AvailabilityEngine;
import com.slotify.module.booking.service.AvailabilityEngine.Rules;
import com.slotify.module.booking.service.TimeWindow;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;
import org.junit.jupiter.api.Test;

/** Unit tests for the pure slot algorithm. Times are 2026-03-02 (Monday), salon in Berlin. */
class AvailabilityEngineTest {

  private static final ZoneId BERLIN = ZoneId.of("Europe/Berlin");
  private static final LocalDate DAY = LocalDate.of(2026, 3, 2);
  private static final Rules RULES = new Rules(Duration.ofMinutes(15), Duration.ofMinutes(60));
  private static final Instant EARLY_MORNING = at(6, 0);

  private static Instant at(int hour, int minute) {
    return DAY.atTime(LocalTime.of(hour, minute)).atZone(BERLIN).toInstant();
  }

  private static TimeWindow window(int fromHour, int toHour) {
    return AvailabilityEngine.localWindow(
            DAY, LocalTime.of(fromHour, 0), LocalTime.of(toHour, 0), BERLIN)
        .orElseThrow();
  }

  @Test
  void generatesSlotsOnTheGridWithinWorkingHours() {
    List<Instant> slots =
        AvailabilityEngine.slots(
            List.of(window(9, 11)), List.of(), Duration.ofMinutes(30), RULES, EARLY_MORNING);

    // 09:00 .. 10:30 (last start that still ends by 11:00)
    assertThat(slots)
        .containsExactly(
            at(9, 0), at(9, 15), at(9, 30), at(9, 45), at(10, 0), at(10, 15), at(10, 30));
  }

  @Test
  void excludesBusyPeriodsAndReAlignsToTheGrid() {
    List<TimeWindow> busy = List.of(new TimeWindow(at(9, 20), at(9, 50)));

    List<Instant> slots =
        AvailabilityEngine.slots(
            List.of(window(9, 11)), busy, Duration.ofMinutes(30), RULES, EARLY_MORNING);

    // Before the booking only 09:00 fits (ends 09:30 > 09:20 → no; 08:50 not in window) …
    assertThat(slots).doesNotContain(at(9, 0), at(9, 15), at(9, 30));
    // … after it the free gap starts 09:50 but the grid re-aligns to 10:00.
    assertThat(slots).startsWith(at(10, 0));
    assertThat(slots).containsExactly(at(10, 0), at(10, 15), at(10, 30));
  }

  @Test
  void respectsMinimumAdvanceNotice() {
    Instant now = at(9, 10); // + 60 min advance → earliest 10:10 → first grid slot 10:15

    List<Instant> slots =
        AvailabilityEngine.slots(
            List.of(window(9, 12)), List.of(), Duration.ofMinutes(30), RULES, now);

    assertThat(slots).startsWith(at(10, 15));
  }

  @Test
  void mergesOverlappingWorkingWindowsAndBusyPeriods() {
    List<TimeWindow> working = List.of(window(9, 12), window(11, 14));
    List<TimeWindow> busy =
        List.of(new TimeWindow(at(10, 0), at(11, 0)), new TimeWindow(at(10, 30), at(11, 30)));

    List<Instant> slots =
        AvailabilityEngine.slots(working, busy, Duration.ofMinutes(60), RULES, EARLY_MORNING);

    assertThat(slots).contains(at(9, 0), at(11, 30), at(13, 0));
    assertThat(slots).doesNotContain(at(10, 0), at(10, 45), at(13, 15));
  }

  @Test
  void serviceLongerThanAnyGapYieldsNoSlots() {
    List<Instant> slots =
        AvailabilityEngine.slots(
            List.of(window(9, 10)), List.of(), Duration.ofMinutes(90), RULES, EARLY_MORNING);

    assertThat(slots).isEmpty();
  }

  @Test
  void closedDayProducesNoWindow() {
    assertThat(AvailabilityEngine.localWindow(DAY, null, null, BERLIN)).isEmpty();
    assertThat(AvailabilityEngine.localWindow(DAY, LocalTime.NOON, LocalTime.NOON, BERLIN))
        .isEmpty();
  }

  @Test
  void intersectionOfSalonHoursAndStaffShift() {
    List<TimeWindow> shift = List.of(window(8, 13));
    List<TimeWindow> salon = List.of(window(9, 18));

    assertThat(TimeWindow.intersect(shift, salon)).containsExactly(window(9, 13));
  }

  @Test
  void handlesDaylightSavingSwitch() {
    // 2026-03-29 clocks jump 02:00 → 03:00 in Berlin; a 09-12 window is still 3 hours.
    LocalDate dst = LocalDate.of(2026, 3, 29);
    TimeWindow w =
        AvailabilityEngine.localWindow(dst, LocalTime.of(9, 0), LocalTime.of(12, 0), BERLIN)
            .orElseThrow();
    assertThat(w.duration()).isEqualTo(Duration.ofHours(3));
  }
}
