package com.slotify.module.service.entity;

import com.slotify.common.entity.BaseEntity;
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

/** Salon-defined grouping of services on the menu (e.g. "Haircuts", "Colour"). */
@Entity
@Table(name = "service_categories")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ServiceCategory extends BaseEntity {

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "salon_id", nullable = false)
  private Salon salon;

  @Column(nullable = false, length = 100)
  private String name;

  @Column(name = "sort_order", nullable = false)
  private int sortOrder;

  public static ServiceCategory of(Salon salon, String name, int sortOrder) {
    ServiceCategory category = new ServiceCategory();
    category.salon = salon;
    category.name = name;
    category.sortOrder = sortOrder;
    return category;
  }
}
