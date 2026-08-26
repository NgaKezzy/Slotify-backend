package com.slotify.module.salon.dto;

import jakarta.validation.constraints.NotNull;
import java.time.DayOfWeek;
import java.time.LocalTime;

/**
 * Opening window for one weekday in the salon's local time.
 *
 * @param day weekday
 * @param openTime opening time (null when closed)
 * @param closeTime closing time (null when closed)
 * @param closed whether the salon is closed that day
 */
public record OpeningHourDto(
    @NotNull DayOfWeek day, LocalTime openTime, LocalTime closeTime, boolean closed) {}
