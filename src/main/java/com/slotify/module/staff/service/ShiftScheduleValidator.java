package com.slotify.module.staff.service;

import com.slotify.common.exception.AppException;
import com.slotify.common.exception.ErrorCode;
import com.slotify.module.staff.dto.ShiftRequest;
import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Validates a weekly shift schedule before it is stored: every window must end after it starts and
 * windows on the same weekday must not overlap (touching windows such as 09:00–12:00 and
 * 12:00–17:00 are allowed). Used by {@link StaffService#replaceShifts}.
 */
public final class ShiftScheduleValidator {

  private ShiftScheduleValidator() {}

  /**
   * Checks ordering and overlap of the given windows.
   *
   * @throws AppException {@link ErrorCode#INVALID_TIME_RANGE} when a window is empty or reversed,
   *     {@link ErrorCode#SHIFT_OVERLAP} when two windows on the same weekday intersect
   */
  public static void validate(List<ShiftRequest> shifts) {
    shifts.forEach(shift -> requireOrdered(shift.startTime(), shift.endTime()));

    Map<DayOfWeek, List<ShiftRequest>> byDay =
        shifts.stream().collect(Collectors.groupingBy(ShiftRequest::dayOfWeek));
    byDay.values().forEach(ShiftScheduleValidator::requireNoOverlap);
  }

  /**
   * Ensures {@code end} is strictly after {@code start}.
   *
   * @throws AppException {@link ErrorCode#INVALID_TIME_RANGE}
   */
  public static void requireOrdered(LocalTime start, LocalTime end) {
    if (start == null || end == null || !end.isAfter(start)) {
      throw new AppException(ErrorCode.INVALID_TIME_RANGE);
    }
  }

  private static void requireNoOverlap(List<ShiftRequest> sameDay) {
    List<ShiftRequest> sorted =
        sameDay.stream().sorted(Comparator.comparing(ShiftRequest::startTime)).toList();
    for (int i = 1; i < sorted.size(); i++) {
      // Sorted by start, so only the previous window can still be running when this one starts.
      if (sorted.get(i).startTime().isBefore(sorted.get(i - 1).endTime())) {
        throw new AppException(ErrorCode.SHIFT_OVERLAP);
      }
    }
  }
}
