package com.slotify.module.booking.entity;

import com.slotify.common.entity.BaseEntity;
import com.slotify.module.service.entity.SalonService;
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

/** One service inside a booking with its name and price snapshotted at booking time. */
@Entity
@Table(name = "booking_items")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class BookingItem extends BaseEntity {

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "booking_id", nullable = false)
  private Booking booking;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "service_id")
  private SalonService service;

  @Column(name = "service_name", nullable = false, length = 150)
  private String serviceName;

  @Column(name = "duration_min", nullable = false)
  private int durationMin;

  @Column(name = "price_minor", nullable = false)
  private long priceMinor;

  public static BookingItem from(SalonService service) {
    BookingItem item = new BookingItem();
    item.service = service;
    item.serviceName = service.getName();
    item.durationMin = service.getDurationMin();
    item.priceMinor = service.getPriceMinor();
    return item;
  }
}
