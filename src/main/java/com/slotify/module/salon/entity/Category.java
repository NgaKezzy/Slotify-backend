package com.slotify.module.salon.entity;

import com.slotify.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Global salon category (Hair, Nails, Massage, ...) managed by SUPER_ADMIN. */
@Entity
@Table(name = "categories")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Category extends BaseEntity {

  @Column(nullable = false, length = 100)
  private String name;

  @Column(name = "icon_url", length = 500)
  private String iconUrl;

  @Column(name = "sort_order", nullable = false)
  private int sortOrder;

  public static Category of(String name, String iconUrl, int sortOrder) {
    Category category = new Category();
    category.name = name;
    category.iconUrl = iconUrl;
    category.sortOrder = sortOrder;
    return category;
  }
}
