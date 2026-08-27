package com.slotify.module.report.repository;

import com.slotify.module.booking.entity.BookingStatus;

/**
 * Projection of a {@code GROUP BY status} count.
 *
 * @param status booking status
 * @param count number of bookings
 */
public record StatusCount(BookingStatus status, long count) {}
