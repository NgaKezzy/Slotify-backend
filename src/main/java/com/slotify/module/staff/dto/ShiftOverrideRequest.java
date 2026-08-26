package com.slotify.module.staff.dto;

import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.time.LocalTime;

/**
 * Body of {@code POST /admin/salons/{salonId}/staff/{staffId}/shift-overrides}. Creates or replaces
 * the exception for the given date.
 *
 * @param date local date in the salon timezone
 * @param off {@code true} for a whole day off (times are ignored)
 * @param startTime replacement window start, required when {@code off} is {@code false}
 * @param endTime replacement window end, required when {@code off} is {@code false}
 */
public record ShiftOverrideRequest(
    @NotNull LocalDate date, boolean off, LocalTime startTime, LocalTime endTime) {}
