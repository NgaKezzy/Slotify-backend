package com.slotify.module.staff.dto;

import java.math.BigDecimal;
import java.util.List;

/**
 * Customer-facing representation of a staff member ({@code GET /salons/{salonId}/staff}). Exposes
 * no account details.
 *
 * @param id staff id
 * @param displayName name shown to customers
 * @param title optional job title
 * @param bio optional biography
 * @param avatarUrl optional profile image
 * @param ratingAvg average review rating (0.00 – 5.00)
 * @param ratingCount number of reviews
 * @param serviceIds ids of the services the staff member can perform
 */
public record PublicStaffResponse(
    Long id,
    String displayName,
    String title,
    String bio,
    String avatarUrl,
    BigDecimal ratingAvg,
    int ratingCount,
    List<Long> serviceIds) {}
