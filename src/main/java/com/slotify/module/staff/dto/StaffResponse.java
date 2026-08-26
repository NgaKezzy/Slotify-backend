package com.slotify.module.staff.dto;

import java.math.BigDecimal;
import java.util.List;

/**
 * Owner-facing representation of a staff member (admin endpoints and {@code /staff/me}).
 *
 * @param id staff id
 * @param salonId salon the staff member belongs to
 * @param userId linked login account, {@code null} when not invited yet
 * @param email email of the linked account, {@code null} when not invited yet
 * @param displayName name shown to customers
 * @param title optional job title
 * @param bio optional biography
 * @param avatarUrl optional profile image
 * @param ratingAvg average review rating (0.00 – 5.00)
 * @param ratingCount number of reviews
 * @param active whether the staff member is bookable
 * @param serviceIds ids of the services the staff member can perform
 */
public record StaffResponse(
    Long id,
    Long salonId,
    Long userId,
    String email,
    String displayName,
    String title,
    String bio,
    String avatarUrl,
    BigDecimal ratingAvg,
    int ratingCount,
    boolean active,
    List<Long> serviceIds) {}
