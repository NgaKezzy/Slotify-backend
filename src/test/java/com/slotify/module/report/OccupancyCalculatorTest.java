package com.slotify.module.report;

import static org.assertj.core.api.Assertions.assertThat;

import com.slotify.module.report.dto.ReportPeriod;
import com.slotify.module.report.repository.BookedInterval;
import com.slotify.module.report.service.OccupancyCalculator;
import com.slotify.module.salon.entity.Salon;
import com.slotify.module.staff.entity.Staff;
import com.slotify.module.staff.entity.StaffShift;
import com.slotify.module.user.entity.Role;
import com.slotify.module.user.entity.User;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;
import org.junit.jupiter.api.Test;

/** Scheduled versus booked minutes over a period. */
class OccupancyCalculatorTest {

  private static final ZoneId BERLIN = ZoneId.of("Europe/Berlin");

  // Monday 2026-03-02 .. Sunday 2026-03-08: one full week.
  private final ReportPeriod week =
      new ReportPeriod(LocalDate.of(2026, 3, 2), LocalDate.of(2026, 3, 8), BERLIN);

  @Test
  void availableMinutesMultiplyWeeklyShiftsOverTheDays() {
    Staff staff =
        Staff.create(Salon.create(User.local("o@x", null, "O", Role.SALON_OWNER), "S", "s"), "A");
    List<StaffShift> shifts =
        List.of(
            StaffShift.of(staff, DayOfWeek.MONDAY, LocalTime.of(9, 0), LocalTime.of(17, 0)),
            StaffShift.of(staff, DayOfWeek.MONDAY, LocalTime.of(18, 0), LocalTime.of(20, 0)),
            StaffShift.of(staff, DayOfWeek.WEDNESDAY, LocalTime.of(9, 0), LocalTime.of(12, 0)));
    // Monday 8h + 2h, Wednesday 3h = 13h
    assertThat(OccupancyCalculator.availableMinutes(shifts, week)).isEqualTo(13 * 60);
    ReportPeriod twoWeeks = new ReportPeriod(week.from(), week.to().plusWeeks(1), BERLIN);
    assertThat(OccupancyCalculator.availableMinutes(shifts, twoWeeks)).isEqualTo(26 * 60);
  }

  @Test
  void bookedMinutesAreClippedToThePeriod() {
    List<BookedInterval> intervals =
        List.of(
            new BookedInterval(
                1L, Instant.parse("2026-03-03T09:00:00Z"), Instant.parse("2026-03-03T10:30:00Z")),
            // straddles the period end (Sunday 8 March 23:00 UTC = Monday 00:00 Berlin)
            new BookedInterval(
                1L, Instant.parse("2026-03-08T22:30:00Z"), Instant.parse("2026-03-08T23:30:00Z")),
            // entirely outside
            new BookedInterval(
                1L, Instant.parse("2026-03-10T09:00:00Z"), Instant.parse("2026-03-10T10:00:00Z")));
    assertThat(OccupancyCalculator.bookedMinutes(intervals, week)).isEqualTo(90 + 30);
  }

  @Test
  void percentIsRoundedToOneDecimalAndZeroWhenNothingScheduled() {
    assertThat(OccupancyCalculator.percent(1, 3)).isEqualTo(33.3);
    assertThat(OccupancyCalculator.percent(120, 480)).isEqualTo(25.0);
    assertThat(OccupancyCalculator.percent(10, 0)).isZero();
  }
}
