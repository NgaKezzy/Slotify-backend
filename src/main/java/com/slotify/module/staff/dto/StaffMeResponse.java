package com.slotify.module.staff.dto;

/**
 * Payload of {@code GET /staff/me}: the staff profile plus a summary of the salon the staff member
 * works at.
 *
 * @param staff the staff profile
 * @param salon the salon summary
 */
public record StaffMeResponse(StaffResponse staff, SalonSummary salon) {

  /**
   * Minimal salon information needed by the Staff app.
   *
   * @param id salon id
   * @param name salon name
   * @param slug URL slug
   * @param address street address
   * @param city city
   * @param timezone IANA timezone of shifts and bookings
   * @param currency ISO-4217 currency of prices
   * @param coverUrl optional cover image
   */
  public record SalonSummary(
      Long id,
      String name,
      String slug,
      String address,
      String city,
      String timezone,
      String currency,
      String coverUrl) {}
}
