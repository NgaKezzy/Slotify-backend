package com.slotify.module.report.dto;

import com.slotify.common.exception.AppException;
import com.slotify.common.exception.ErrorCode;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.stream.Stream;

/**
 * An inclusive range of local dates in the salon timezone, as used by every report endpoint.
 *
 * <p>Instants are compared against {@code [startInstant(), endInstant())}: the start of the first
 * day and the start of the day after the last day, both in the salon zone.
 *
 * @param from first day (inclusive)
 * @param to last day (inclusive)
 * @param zone timezone the dates are interpreted in
 */
public record ReportPeriod(LocalDate from, LocalDate to, ZoneId zone) {

  /** Length of the default period ("last 30 days") when the client sends no dates. */
  public static final int DEFAULT_DAYS = 30;

  /** Upper bound to keep on-demand aggregation cheap. */
  public static final int MAX_DAYS = 366 * 2;

  /**
   * Resolves the query parameters into a period, defaulting to the last {@link #DEFAULT_DAYS} days
   * ending today.
   *
   * @throws AppException {@link ErrorCode#INVALID_TIME_RANGE} when {@code to} precedes {@code from}
   *     or the range is longer than {@link #MAX_DAYS}
   */
  public static ReportPeriod resolve(LocalDate from, LocalDate to, ZoneId zone, Clock clock) {
    LocalDate today = LocalDate.now(clock.withZone(zone));
    LocalDate end = to != null ? to : today;
    LocalDate start = from != null ? from : end.minusDays(DEFAULT_DAYS - 1L);
    if (end.isBefore(start) || ChronoUnit.DAYS.between(start, end) >= MAX_DAYS) {
      throw new AppException(ErrorCode.INVALID_TIME_RANGE);
    }
    return new ReportPeriod(start, end, zone);
  }

  /** Number of calendar days in the period (at least 1). */
  public long days() {
    return ChronoUnit.DAYS.between(from, to) + 1;
  }

  /** Start of the first day in the zone. */
  public Instant startInstant() {
    return from.atStartOfDay(zone).toInstant();
  }

  /** Start of the day after the last day in the zone (exclusive upper bound). */
  public Instant endInstant() {
    return to.plusDays(1).atStartOfDay(zone).toInstant();
  }

  /** The period of equal length that ends the day before this one starts. */
  public ReportPeriod previous() {
    long length = days();
    return new ReportPeriod(from.minusDays(length), from.minusDays(1), zone);
  }

  /** Every day of the period in order. */
  public List<LocalDate> dates() {
    return Stream.iterate(from, d -> !d.isAfter(to), d -> d.plusDays(1)).toList();
  }

  /** Client-facing description. */
  public PeriodDto toDto() {
    return new PeriodDto(from, to, zone.getId());
  }

  /**
   * Serialised period.
   *
   * @param from first day (inclusive)
   * @param to last day (inclusive)
   * @param timezone IANA zone id
   */
  public record PeriodDto(LocalDate from, LocalDate to, String timezone) {}
}
