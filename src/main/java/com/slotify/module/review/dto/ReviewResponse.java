package com.slotify.module.review.dto;

import java.time.Instant;

/**
 * A review as returned to customers, the public salon page and the owner.
 *
 * @param id review id
 * @param bookingId reviewed booking
 * @param bookingCode human-readable booking code
 * @param salonId salon the review belongs to
 * @param staff staff member who performed the service (may be null)
 * @param customerName full name for the author / owner, abbreviated ("Chris C.") on the public page
 * @param rating 1-5
 * @param comment customer's text
 * @param reply owner's answer, if any
 * @param repliedAt when the owner answered
 * @param visible whether the review is shown publicly
 * @param createdAt when the review was written
 */
public record ReviewResponse(
    Long id,
    Long bookingId,
    String bookingCode,
    Long salonId,
    StaffRef staff,
    String customerName,
    int rating,
    String comment,
    String reply,
    Instant repliedAt,
    boolean visible,
    Instant createdAt) {

  /** Minimal staff reference. */
  public record StaffRef(Long id, String displayName) {}
}
