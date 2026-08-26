package com.slotify.module.salon.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.math.BigDecimal;

/**
 * Query parameters of {@code GET /salons} (public search).
 *
 * @param q free-text search on name / city
 * @param city exact city filter
 * @param categoryId global category filter
 * @param lat search centre latitude (with {@code lng} enables distance sorting/filtering)
 * @param lng search centre longitude
 * @param radiusKm maximum distance from the centre
 * @param sort {@code rating} (default), {@code distance} or {@code name}
 * @param page zero-based page
 * @param size page size (max 50)
 */
public record SalonSearchRequest(
    String q,
    String city,
    Long categoryId,
    @DecimalMin("-90.0") @DecimalMax("90.0") BigDecimal lat,
    @DecimalMin("-180.0") @DecimalMax("180.0") BigDecimal lng,
    @Min(1) @Max(500) Integer radiusKm,
    String sort,
    @Min(0) Integer page,
    @Min(1) @Max(50) Integer size) {

  /** Sort keys accepted by the search endpoint. */
  public enum Sort {
    RATING,
    DISTANCE,
    NAME;

    /** Parses a query value case-insensitively; unknown values fall back to RATING. */
    public static Sort parse(String value) {
      if (value == null) {
        return RATING;
      }
      try {
        return valueOf(value.trim().toUpperCase());
      } catch (IllegalArgumentException ex) {
        return RATING;
      }
    }
  }

  public boolean hasLocation() {
    return lat != null && lng != null;
  }

  public int pageOrDefault() {
    return page == null ? 0 : page;
  }

  public int sizeOrDefault() {
    return size == null ? 20 : size;
  }
}
