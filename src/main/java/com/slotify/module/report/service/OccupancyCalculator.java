package com.slotify.module.report.service;

import com.slotify.module.report.dto.ReportPeriod;
import com.slotify.module.report.repository.BookedInterval;
import com.slotify.module.staff.entity.StaffShift;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Collection;

/**
 * Occupancy = booked minutes / scheduled minutes.
 *
 * <p>Scheduled minutes come from the weekly {@link StaffShift} windows multiplied over the days of
 * the period; per-day overrides and time off are deliberately ignored in v1 to keep the figure
 * cheap and predictable. Booked minutes are clipped to the period.
 */
public final class OccupancyCalculator {

  private static final int DAYS_PER_WEEK = 7;

  private OccupancyCalculator() {}

  /** Minutes covered by the shifts over every day of the period. */
  public static long availableMinutes(Collection<StaffShift> shifts, ReportPeriod period) {
    long[] minutesPerWeekday = new long[DAYS_PER_WEEK];
    for (StaffShift shift : shifts) {
      minutesPerWeekday[shift.getDayOfWeek()] +=
          Duration.between(shift.getStartTime(), shift.getEndTime()).toMinutes();
    }
    long total = 0;
    for (LocalDate date : period.dates()) {
      total += minutesPerWeekday[date.getDayOfWeek().getValue() - 1];
    }
    return total;
  }

  /** Minutes of the intervals that fall inside the period. */
  public static long bookedMinutes(Collection<BookedInterval> intervals, ReportPeriod period) {
    Instant start = period.startInstant();
    Instant end = period.endInstant();
    long total = 0;
    for (BookedInterval interval : intervals) {
      Instant from = interval.startAt().isBefore(start) ? start : interval.startAt();
      Instant to = interval.endAt().isAfter(end) ? end : interval.endAt();
      if (to.isAfter(from)) {
        total += Duration.between(from, to).toMinutes();
      }
    }
    return total;
  }

  /** Percentage rounded to one decimal; 0 when nothing is scheduled. */
  public static double percent(long booked, long available) {
    if (available <= 0) {
      return 0;
    }
    return Math.round(booked * 1000.0 / available) / 10.0;
  }
}
