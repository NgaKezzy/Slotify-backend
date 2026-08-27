package com.slotify.module.customer.dto;

import java.time.Instant;
import java.util.List;

/**
 * One row of the CRM customer list: contact data, visit statistics and tags.
 *
 * @param id user id of the customer
 * @param fullName customer name
 * @param email contact email
 * @param phone contact phone (may be null)
 * @param avatarUrl profile picture (may be null)
 * @param totalBookings bookings of any status at this salon
 * @param completedBookings bookings in status COMPLETED
 * @param noShows bookings in status NO_SHOW
 * @param cancelledBookings bookings in status CANCELLED
 * @param totalSpentMinor lifetime value: sum of completed booking totals in minor units
 * @param firstVisitAt start of the earliest completed booking (null when none)
 * @param lastVisitAt start of the latest completed booking (null when none)
 * @param tags tags of this salon assigned to the customer
 */
public record CustomerSummaryResponse(
    Long id,
    String fullName,
    String email,
    String phone,
    String avatarUrl,
    long totalBookings,
    long completedBookings,
    long noShows,
    long cancelledBookings,
    long totalSpentMinor,
    Instant firstVisitAt,
    Instant lastVisitAt,
    List<CustomerTagResponse> tags) {}
