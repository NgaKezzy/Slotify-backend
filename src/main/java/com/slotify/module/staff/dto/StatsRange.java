package com.slotify.module.staff.dto;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.TemporalAdjusters;

/**
 * Preset periods of {@code GET /staff/stats?range=}. Every range is resolved against the current
 * date in the salon timezone so "today" matches what the staff member sees on the agenda.
 */
public enum StatsRange {
  /** The current salon-local day. */
  TODAY,
  /** Monday to Sunday of the current week. */
  WEEK,
  /** First to last day of the current month. */
  MONTH;

  /** First day (inclusive) of the range in {@code zone}. */
  public LocalDate from(Clock clock, ZoneId zone) {
    LocalDate today = LocalDate.now(clock.withZone(zone));
    return switch (this) {
      case TODAY -> today;
      case WEEK -> today.with(TemporalAdjusters.previousOrSame(java.time.DayOfWeek.MONDAY));
      case MONTH -> today.withDayOfMonth(1);
    };
  }

  /** Last day (inclusive) of the range in {@code zone}. */
  public LocalDate to(Clock clock, ZoneId zone) {
    LocalDate today = LocalDate.now(clock.withZone(zone));
    return switch (this) {
      case TODAY -> today;
      case WEEK -> today.with(TemporalAdjusters.nextOrSame(java.time.DayOfWeek.SUNDAY));
      case MONTH -> today.with(TemporalAdjusters.lastDayOfMonth());
    };
  }
}
