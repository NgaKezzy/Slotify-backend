package com.slotify.module.booking.dto;

import com.slotify.module.booking.entity.BookingStatus;
import java.time.Instant;
import org.springframework.format.annotation.DateTimeFormat;

/**
 * Query parameters of the owner's booking list.
 *
 * @param status status filter
 * @param staffId staff filter
 * @param from bookings starting at or after this instant
 * @param to bookings starting before this instant
 * @param q search in code, customer name or email
 * @param page zero-based page (default 0)
 * @param size page size (default 20, max 100)
 */
public record BookingListFilter(
    BookingStatus status,
    Long staffId,
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to,
    String q,
    Integer page,
    Integer size) {

  public int pageOrDefault() {
    return page == null ? 0 : page;
  }

  public int sizeOrDefault() {
    return size == null ? 20 : Math.min(size, 100);
  }
}
