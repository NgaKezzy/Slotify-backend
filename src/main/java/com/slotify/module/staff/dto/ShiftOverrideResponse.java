package com.slotify.module.staff.dto;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * A stored per-date schedule exception.
 *
 * @param id override id
 * @param date local date in the salon timezone
 * @param off whether the whole day is off
 * @param startTime replacement window start ({@code null} when off)
 * @param endTime replacement window end ({@code null} when off)
 */
public record ShiftOverrideResponse(
    Long id, LocalDate date, boolean off, LocalTime startTime, LocalTime endTime) {}
