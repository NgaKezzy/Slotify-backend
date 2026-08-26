package com.slotify.module.booking.entity;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

/**
 * Lifecycle of a booking and the allowed transitions (see plan §3.5).
 *
 * <pre>
 * PENDING ──confirm──► CONFIRMED ──start──► IN_PROGRESS ──complete──► COMPLETED
 *    │                    │
 *    ├─reject──► REJECTED ├─cancel──► CANCELLED
 *    └─cancel──► CANCELLED└─no_show─► NO_SHOW
 * </pre>
 */
public enum BookingStatus {
  PENDING,
  CONFIRMED,
  IN_PROGRESS,
  COMPLETED,
  CANCELLED,
  NO_SHOW,
  REJECTED;

  private static final Map<BookingStatus, Set<BookingStatus>> TRANSITIONS =
      new EnumMap<>(BookingStatus.class);

  static {
    TRANSITIONS.put(PENDING, EnumSet.of(CONFIRMED, REJECTED, CANCELLED));
    TRANSITIONS.put(CONFIRMED, EnumSet.of(IN_PROGRESS, COMPLETED, CANCELLED, NO_SHOW));
    TRANSITIONS.put(IN_PROGRESS, EnumSet.of(COMPLETED, NO_SHOW));
    TRANSITIONS.put(COMPLETED, EnumSet.noneOf(BookingStatus.class));
    TRANSITIONS.put(CANCELLED, EnumSet.noneOf(BookingStatus.class));
    TRANSITIONS.put(NO_SHOW, EnumSet.noneOf(BookingStatus.class));
    TRANSITIONS.put(REJECTED, EnumSet.noneOf(BookingStatus.class));
  }

  /** Statuses that occupy the staff member's calendar. */
  public static final Set<BookingStatus> BLOCKING = EnumSet.of(PENDING, CONFIRMED, IN_PROGRESS);

  /** Statuses shown under "upcoming" in the customer app. */
  public static final Set<BookingStatus> UPCOMING = EnumSet.of(PENDING, CONFIRMED, IN_PROGRESS);

  public boolean canTransitionTo(BookingStatus target) {
    return TRANSITIONS.get(this).contains(target);
  }

  public boolean isFinal() {
    return TRANSITIONS.get(this).isEmpty();
  }

  public boolean blocksCalendar() {
    return BLOCKING.contains(this);
  }
}
