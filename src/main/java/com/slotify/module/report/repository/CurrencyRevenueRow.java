package com.slotify.module.report.repository;

/**
 * Projection of revenue grouped by currency.
 *
 * @param currency ISO-4217 code
 * @param revenueMinor summed {@code total_minor}
 * @param bookings number of bookings summed
 */
public record CurrencyRevenueRow(String currency, long revenueMinor, long bookings) {}
