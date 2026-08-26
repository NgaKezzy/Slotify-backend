package com.slotify.module.salon.entity;

import com.slotify.common.entity.BaseEntity;
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

/** Gallery image of a salon. */
@Entity
@Table(name = "salon_images")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SalonImage extends BaseEntity {

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "salon_id", nullable = false)
  private Salon salon;

  @Column(nullable = false, length = 500)
  private String url;

  @Column(name = "sort_order", nullable = false)
  private int sortOrder;

  public static SalonImage of(Salon salon, String url, int sortOrder) {
    SalonImage image = new SalonImage();
    image.salon = salon;
    image.url = url;
    image.sortOrder = sortOrder;
    return image;
  }
}
