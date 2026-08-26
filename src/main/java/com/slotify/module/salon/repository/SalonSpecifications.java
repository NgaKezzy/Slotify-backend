package com.slotify.module.salon.repository;

import com.slotify.module.salon.entity.Salon;
import com.slotify.module.salon.entity.SalonStatus;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import java.math.BigDecimal;
import org.springframework.data.jpa.domain.Specification;

/** JPA specifications used by the public salon search. Combine with {@code Specification.allOf}. */
public final class SalonSpecifications {

  private SalonSpecifications() {}

  public static Specification<Salon> hasStatus(SalonStatus status) {
    return (root, query, cb) -> cb.equal(root.get("status"), status);
  }

  /** Case-insensitive match on name or city. */
  public static Specification<Salon> matchesText(String q) {
    if (q == null || q.isBlank()) {
      return null;
    }
    String pattern = "%" + q.trim().toLowerCase() + "%";
    return (root, query, cb) ->
        cb.or(
            cb.like(cb.lower(root.get("name")), pattern),
            cb.like(cb.lower(root.get("city")), pattern));
  }

  public static Specification<Salon> inCity(String city) {
    if (city == null || city.isBlank()) {
      return null;
    }
    return (root, query, cb) -> cb.equal(cb.lower(root.get("city")), city.trim().toLowerCase());
  }

  public static Specification<Salon> hasCategory(Long categoryId) {
    if (categoryId == null) {
      return null;
    }
    return (root, query, cb) -> {
      query.distinct(true);
      Join<Object, Object> categories = root.join("categories", JoinType.INNER);
      return cb.equal(categories.get("id"), categoryId);
    };
  }

  /**
   * Cheap bounding-box pre-filter around a point; the exact Haversine distance is computed in
   * memory by the service for the (small) page of candidates.
   */
  public static Specification<Salon> withinBoundingBox(
      BigDecimal lat, BigDecimal lng, int radiusKm) {
    if (lat == null || lng == null) {
      return null;
    }
    double latDelta = radiusKm / 111.0; // ~111 km per degree of latitude
    double lngDelta = radiusKm / (111.0 * Math.cos(Math.toRadians(lat.doubleValue())));
    return (root, query, cb) ->
        cb.and(
            cb.between(
                root.get("lat"),
                lat.subtract(BigDecimal.valueOf(latDelta)),
                lat.add(BigDecimal.valueOf(latDelta))),
            cb.between(
                root.get("lng"),
                lng.subtract(BigDecimal.valueOf(lngDelta)),
                lng.add(BigDecimal.valueOf(lngDelta))));
  }
}
