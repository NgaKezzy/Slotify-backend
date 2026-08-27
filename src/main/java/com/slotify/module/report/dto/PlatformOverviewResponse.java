package com.slotify.module.report.dto;

import com.slotify.module.salon.entity.SalonStatus;
import com.slotify.module.user.entity.Role;
import java.util.List;
import java.util.Map;

/**
 * Platform-wide overview for the super admin.
 *
 * @param period the last 30 days the booking figures refer to (UTC days)
 * @param salonsByStatus number of salons per status
 * @param usersByRole number of user accounts per role
 * @param bookingsLast30Days bookings starting in the period, any status
 * @param revenueLast30Days revenue of COMPLETED bookings per currency
 * @param topSalons the five salons with the highest revenue in the period
 */
public record PlatformOverviewResponse(
    ReportPeriod.PeriodDto period,
    Map<SalonStatus, Long> salonsByStatus,
    Map<Role, Long> usersByRole,
    long bookingsLast30Days,
    List<CurrencyRevenue> revenueLast30Days,
    List<SalonRevenue> topSalons) {

  /**
   * Revenue in one currency (amounts of different currencies are never added together).
   *
   * @param currency ISO-4217 code
   * @param revenueMinor revenue in minor units
   * @param completedBookings number of completed bookings
   */
  public record CurrencyRevenue(String currency, long revenueMinor, long completedBookings) {}

  /**
   * Revenue of one salon.
   *
   * @param salonId salon id
   * @param name salon name
   * @param currency ISO-4217 code
   * @param revenueMinor revenue in minor units
   * @param completedBookings number of completed bookings
   */
  public record SalonRevenue(
      Long salonId, String name, String currency, long revenueMinor, long completedBookings) {}
}
