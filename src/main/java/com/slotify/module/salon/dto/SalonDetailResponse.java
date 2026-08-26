package com.slotify.module.salon.dto;

import com.slotify.module.salon.entity.SalonStatus;
import java.math.BigDecimal;
import java.util.List;

/**
 * Full salon representation (salon detail screen and owner settings).
 *
 * @param openingHours one entry per weekday
 * @param settings booking rules (customers see only what they need)
 */
public record SalonDetailResponse(
    Long id,
    Long ownerId,
    String name,
    String slug,
    String description,
    String phone,
    String email,
    String address,
    String city,
    String country,
    BigDecimal lat,
    BigDecimal lng,
    String timezone,
    String currency,
    String coverUrl,
    BigDecimal ratingAvg,
    int ratingCount,
    SalonStatus status,
    BigDecimal commissionPercent,
    List<String> images,
    List<OpeningHourDto> openingHours,
    SalonSettingsDto settings,
    List<CategoryResponse> categories,
    List<AmenityResponse> amenities) {}
