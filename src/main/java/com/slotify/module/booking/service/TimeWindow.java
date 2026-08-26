package com.slotify.module.booking.service;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Half-open interval {@code [start, end)} on the UTC timeline, used by the availability engine for
 * working windows and busy periods.
 *
 * @param start inclusive start
 * @param end exclusive end
 */
public record TimeWindow(Instant start, Instant end) {

  public TimeWindow {
    if (!start.isBefore(end)) {
      throw new IllegalArgumentException("start must be before end: " + start + " / " + end);
    }
  }

  public Duration duration() {
    return Duration.between(start, end);
  }

  public boolean overlaps(TimeWindow other) {
    return start.isBefore(other.end) && other.start.isBefore(end);
  }

  public boolean contains(TimeWindow other) {
    return !other.start.isBefore(start) && !other.end.isAfter(end);
  }

  /** Returns the parts of this window that are not covered by {@code busy} (already sorted). */
  public List<TimeWindow> subtract(List<TimeWindow> busy) {
    List<TimeWindow> free = new ArrayList<>();
    Instant cursor = start;
    for (TimeWindow b : busy) {
      if (!b.overlaps(this)) {
        continue;
      }
      if (b.start.isAfter(cursor)) {
        free.add(new TimeWindow(cursor, b.start));
      }
      if (b.end.isAfter(cursor)) {
        cursor = b.end;
      }
    }
    if (cursor.isBefore(end)) {
      free.add(new TimeWindow(cursor, end));
    }
    return free;
  }

  /** Merges overlapping / touching windows into a sorted, disjoint list. */
  public static List<TimeWindow> merge(List<TimeWindow> windows) {
    List<TimeWindow> sorted = new ArrayList<>(windows);
    sorted.sort(Comparator.comparing(TimeWindow::start));
    List<TimeWindow> merged = new ArrayList<>();
    for (TimeWindow w : sorted) {
      if (merged.isEmpty()) {
        merged.add(w);
        continue;
      }
      TimeWindow last = merged.getLast();
      if (!w.start.isAfter(last.end)) {
        merged.set(merged.size() - 1, new TimeWindow(last.start, max(last.end, w.end)));
      } else {
        merged.add(w);
      }
    }
    return merged;
  }

  /** Intersection of two window lists (each assumed sorted & disjoint). */
  public static List<TimeWindow> intersect(List<TimeWindow> a, List<TimeWindow> b) {
    List<TimeWindow> result = new ArrayList<>();
    for (TimeWindow x : a) {
      for (TimeWindow y : b) {
        if (x.overlaps(y)) {
          result.add(new TimeWindow(max(x.start, y.start), min(x.end, y.end)));
        }
      }
    }
    return merge(result);
  }

  private static Instant max(Instant a, Instant b) {
    return a.isAfter(b) ? a : b;
  }

  private static Instant min(Instant a, Instant b) {
    return a.isBefore(b) ? a : b;
  }
}
