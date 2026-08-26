package com.slotify.module.staff.dto;

import jakarta.validation.constraints.NotNull;
import java.time.DayOfWeek;
import java.time.LocalTime;

/**
 * One weekly working window, in the salon's local time.
 *
 * @param dayOfWeek weekday, e.g. {@code MONDAY}
 * @param startTime start of the window (inclusive), e.g. {@code 09:00}
 * @param endTime end of the window (exclusive), must be after {@code startTime}
 */
public record ShiftRequest(
    @NotNull DayOfWeek dayOfWeek, @NotNull LocalTime startTime, @NotNull LocalTime endTime) {}
