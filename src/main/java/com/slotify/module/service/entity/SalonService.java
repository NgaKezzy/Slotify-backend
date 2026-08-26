package com.slotify.module.service.entity;

import com.slotify.common.entity.SoftDeletableEntity;
import com.slotify.module.salon.entity.Salon;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.SQLRestriction;

/**
 * A bookable service on a salon's menu (table {@code services}).
 *
 * <p>Named {@code SalonService} to avoid confusion with Spring's {@code @Service} stereotype.
 * Prices are minor units (cents) in the salon currency.
 */
@Entity
@Table(name = "services")
@SQLRestriction("deleted_at IS NULL")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SalonService extends SoftDeletableEntity {

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "salon_id", nullable = false)
  private Salon salon;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "category_id")
  private ServiceCategory category;

  @Column(nullable = false, length = 150)
  private String name;

  @Column(columnDefinition = "TEXT")
  private String description;

  @Column(name = "duration_min", nullable = false)
  private int durationMin;

  /** Clean-up time reserved after the service, in minutes. */
  @Column(name = "buffer_after_min", nullable = false)
  private int bufferAfterMin;

  @Column(name = "price_minor", nullable = false)
  private long priceMinor;

  @Column(nullable = false, length = 3)
  private String currency;

  @Column(name = "image_url", length = 500)
  private String imageUrl;

  @Column(name = "is_active", nullable = false)
  private boolean active = true;

  @Column(name = "sort_order", nullable = false)
  private int sortOrder;

  public static SalonService create(Salon salon, String name, int durationMin, long priceMinor) {
    SalonService service = new SalonService();
    service.salon = salon;
    service.name = name;
    service.durationMin = durationMin;
    service.priceMinor = priceMinor;
    service.currency = salon.getCurrency();
    return service;
  }

  /** Total time the service blocks on the calendar (duration + buffer). */
  public int totalMinutes() {
    return durationMin + bufferAfterMin;
  }
}
