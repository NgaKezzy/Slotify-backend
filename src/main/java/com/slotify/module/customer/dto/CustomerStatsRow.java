package com.slotify.module.customer.dto;

import java.time.Instant;

/**
 * JPQL projection of one customer's booking statistics at a salon (see {@code
 * CustomerStatsRepository}). Not returned to clients directly; mapped to {@link
 * CustomerSummaryResponse}.
 *
 * @param customerId user id
 * @param fullName customer name
 * @param email contact email
 * @param phone contact phone (may be null)
 * @param avatarUrl profile picture (may be null)
 * @param totalBookings bookings of any status
 * @param completedBookings bookings in status COMPLETED
 * @param noShows bookings in status NO_SHOW
 * @param cancelledBookings bookings in status CANCELLED
 * @param totalSpentMinor sum of {@code total_minor} of completed bookings
 * @param firstVisitAt start of the earliest completed booking (null when none)
 * @param lastVisitAt start of the latest completed booking (null when none)
 */
public record CustomerStatsRow(
    Long customerId,
    String fullName,
    String email,
    String phone,
    String avatarUrl,
    Long totalBookings,
    Long completedBookings,
    Long noShows,
    Long cancelledBookings,
    Long totalSpentMinor,
    Instant firstVisitAt,
    Instant lastVisitAt) {}
