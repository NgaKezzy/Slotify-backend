package com.slotify.module.staff.dto;

import java.time.DayOfWeek;
import java.time.LocalTime;

/**
 * A stored weekly working window.
 *
 * @param id shift id
 * @param dayOfWeek weekday
 * @param startTime start of the window (salon local time)
 * @param endTime end of the window (salon local time)
 */
public record ShiftResponse(Long id, DayOfWeek dayOfWeek, LocalTime startTime, LocalTime endTime) {}
