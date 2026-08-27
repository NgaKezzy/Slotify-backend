package com.slotify.module.payment.repository;

/**
 * Projection of an amount grouped by salon (payments or refunds).
 *
 * @param salonId salon id
 * @param amountMinor summed amount in minor units
 */
public record SalonAmount(Long salonId, long amountMinor) {}
