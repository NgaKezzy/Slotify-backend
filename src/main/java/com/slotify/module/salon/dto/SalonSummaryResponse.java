package com.slotify.module.salon.dto;

import com.slotify.module.salon.entity.SalonStatus;
import java.math.BigDecimal;
import java.util.List;

/**
 * Compact salon representation for lists (search results, favourites, owner's salons).
 *
 * @param id salon id
 * @param name display name
 * @param slug URL-friendly identifier
 * @param city city
 * @param country ISO country code
 * @param address street address
 * @param coverUrl cover image
 * @param ratingAvg average rating 0-5
 * @param ratingCount number of reviews
 * @param currency ISO currency of prices
 * @param status approval status
 * @param categoryIds global category ids
 * @param distanceKm distance from the search point, when a location was given
 */
public record SalonSummaryResponse(
    Long id,
    String name,
    String slug,
    String city,
    String country,
    String address,
    String coverUrl,
    BigDecimal ratingAvg,
    int ratingCount,
    String currency,
    SalonStatus status,
    List<Long> categoryIds,
    Double distanceKm) {

  /** Copy with a distance attached. */
  public SalonSummaryResponse withDistance(Double km) {
    return new SalonSummaryResponse(
        id,
        name,
        slug,
        city,
        country,
        address,
        coverUrl,
        ratingAvg,
        ratingCount,
        currency,
        status,
        categoryIds,
        km);
  }
}
