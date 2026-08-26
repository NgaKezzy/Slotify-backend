package com.slotify.module.booking.dto;

import jakarta.validation.constraints.NotNull;
import java.time.Instant;

/**
 * Body of {@code POST /bookings/{id}/reschedule}.
 *
 * @param startAt new start (UTC); must be an available slot
 * @param staffId optionally switch staff member (null keeps the current one)
 */
public record RescheduleBookingRequest(@NotNull Instant startAt, Long staffId) {}
