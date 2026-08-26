package com.slotify.module.booking.dto;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

/**
 * Bookable start times for a day.
 *
 * @param date requested date (salon local)
 * @param timezone salon timezone, so clients can render local times
 * @param durationMin total minutes the booking will take (durations + buffers)
 * @param slots start times with the staff members free at that time, sorted ascending
 * @param staff staff members that can perform all requested services
 */
public record AvailabilityResponse(
    LocalDate date, String timezone, int durationMin, List<Slot> slots, List<StaffOption> staff) {

  /** One bookable start time. */
  public record Slot(Instant startAt, List<Long> staffIds) {}

  /** Staff member option shown in the "choose stylist" step. */
  public record StaffOption(Long id, String displayName, String title, String avatarUrl) {}
}
