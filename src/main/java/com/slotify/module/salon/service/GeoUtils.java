package com.slotify.module.salon.service;

import java.math.BigDecimal;

/** Great-circle distance helper (Haversine formula). */
public final class GeoUtils {

  private static final double EARTH_RADIUS_KM = 6371.0;

  private GeoUtils() {}

  /**
   * Distance in kilometres between two coordinates; {@code null} when any coordinate is missing.
   */
  public static Double distanceKm(
      BigDecimal lat1, BigDecimal lng1, BigDecimal lat2, BigDecimal lng2) {
    if (lat1 == null || lng1 == null || lat2 == null || lng2 == null) {
      return null;
    }
    double dLat = Math.toRadians(lat2.doubleValue() - lat1.doubleValue());
    double dLng = Math.toRadians(lng2.doubleValue() - lng1.doubleValue());
    double a =
        Math.sin(dLat / 2) * Math.sin(dLat / 2)
            + Math.cos(Math.toRadians(lat1.doubleValue()))
                * Math.cos(Math.toRadians(lat2.doubleValue()))
                * Math.sin(dLng / 2)
                * Math.sin(dLng / 2);
    double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    return Math.round(EARTH_RADIUS_KM * c * 10.0) / 10.0;
  }
}
