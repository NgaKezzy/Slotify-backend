package com.slotify.module.booking.service;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Pure slot computation (no I/O) — the heart of real-time availability.
 *
 * <p>Given the windows in which a staff member works, the periods that are already busy and the
 * salon rules, it returns every start time at which a service of the requested length fits. Kept
 * free of Spring and JPA so it can be unit-tested exhaustively; {@code AvailabilityService} feeds
 * it with data from the database.
 */
public final class AvailabilityEngine {

  private AvailabilityEngine() {}

  /**
   * Booking rules relevant for slot generation.
   *
   * @param slotInterval granularity of start times
   * @param minAdvance earliest start relative to {@code now}
   */
  public record Rules(Duration slotInterval, Duration minAdvance) {}

  /**
   * Computes the bookable start times.
   *
   * @param working windows in which the staff member works (any order, may overlap)
   * @param busy periods already taken (bookings, time off; any order)
   * @param serviceDuration total time the booking blocks (durations + buffers)
   * @param rules salon rules
   * @param now current time; slots must start at or after {@code now + minAdvance}
   * @return sorted list of slot start instants
   */
  public static List<Instant> slots(
      List<TimeWindow> working,
      List<TimeWindow> busy,
      Duration serviceDuration,
      Rules rules,
      Instant now) {
    Instant earliest = now.plus(rules.minAdvance());
    List<TimeWindow> busySorted = TimeWindow.merge(busy);
    List<Instant> result = new ArrayList<>();

    for (TimeWindow window : TimeWindow.merge(working)) {
      for (TimeWindow free : window.subtract(busySorted)) {
        Instant start = alignUp(free.start(), window.start(), rules.slotInterval());
        while (!start.plus(serviceDuration).isAfter(free.end())) {
          if (!start.isBefore(earliest)) {
            result.add(start);
          }
          start = start.plus(rules.slotInterval());
        }
      }
    }
    return result.stream().distinct().sorted().toList();
  }

  /**
   * Builds the working window of one day from a local time range in the salon timezone.
   *
   * @return empty when the range is closed / invalid
   */
  public static Optional<TimeWindow> localWindow(
      LocalDate date, LocalTime from, LocalTime to, ZoneId zone) {
    if (from == null || to == null || !from.isBefore(to)) {
      return Optional.empty();
    }
    return Optional.of(
        new TimeWindow(
            date.atTime(from).atZone(zone).toInstant(), date.atTime(to).atZone(zone).toInstant()));
  }

  /**
   * First instant {@code >= value} that lies on the slot grid anchored at {@code anchor}, so that
   * slots start at e.g. 09:00, 09:15, 09:30 even when a free gap begins at 09:07.
   */
  static Instant alignUp(Instant value, Instant anchor, Duration interval) {
    long step = interval.toSeconds();
    long offset = Math.floorMod(value.getEpochSecond() - anchor.getEpochSecond(), step);
    return offset == 0 ? value : value.plusSeconds(step - offset);
  }
}
