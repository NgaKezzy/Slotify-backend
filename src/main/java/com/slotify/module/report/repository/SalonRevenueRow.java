package com.slotify.module.report.repository;

/**
 * Projection of revenue grouped by salon.
 *
 * @param salonId salon id
 * @param salonName salon name
 * @param currency ISO-4217 currency of the salon
 * @param revenueMinor summed {@code total_minor}
 * @param bookings number of bookings summed
 */
public record SalonRevenueRow(
    Long salonId, String salonName, String currency, long revenueMinor, long bookings) {}
