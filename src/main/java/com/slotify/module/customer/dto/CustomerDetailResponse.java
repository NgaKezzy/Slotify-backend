package com.slotify.module.customer.dto;

import com.slotify.module.booking.dto.BookingResponse;
import java.util.List;

/**
 * CRM detail page of one customer.
 *
 * @param customer statistics, contact data and tags
 * @param notes internal notes, newest first
 * @param recentBookings the last bookings at this salon, newest first
 */
public record CustomerDetailResponse(
    CustomerSummaryResponse customer,
    List<CustomerNoteResponse> notes,
    List<BookingResponse> recentBookings) {}
