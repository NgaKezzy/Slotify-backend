package com.slotify.module.report.service;

import com.slotify.module.report.dto.Granularity;
import com.slotify.module.report.dto.ReportPeriod;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.List;

/**
 * Assigns instants to time-series buckets (day, ISO week starting Monday, calendar month) in a
 * given timezone. A bucket is identified by its first day.
 */
public final class SeriesBucketer {

  private SeriesBucketer() {}

  /** First day of the bucket containing {@code date}. */
  public static LocalDate bucketStart(LocalDate date, Granularity granularity) {
    return switch (granularity) {
      case DAY -> date;
      case WEEK -> date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
      case MONTH -> date.withDayOfMonth(1);
    };
  }

  /** First day of the bucket containing the instant, evaluated in {@code zone}. */
  public static LocalDate bucketOf(Instant at, ZoneId zone, Granularity granularity) {
    return bucketStart(at.atZone(zone).toLocalDate(), granularity);
  }

  /** First day of the bucket following the one starting at {@code bucketStart}. */
  public static LocalDate nextBucket(LocalDate bucketStart, Granularity granularity) {
    return switch (granularity) {
      case DAY -> bucketStart.plusDays(1);
      case WEEK -> bucketStart.plusWeeks(1);
      case MONTH -> bucketStart.plusMonths(1);
    };
  }

  /**
   * Every bucket touching the period, in order. The first bucket may start before {@code
   * period.from()} (e.g. the Monday of a week that begins mid-period).
   */
  public static List<LocalDate> buckets(ReportPeriod period, Granularity granularity) {
    List<LocalDate> buckets = new ArrayList<>();
    for (LocalDate cursor = bucketStart(period.from(), granularity);
        !cursor.isAfter(period.to());
        cursor = nextBucket(cursor, granularity)) {
      buckets.add(cursor);
    }
    return buckets;
  }
}
