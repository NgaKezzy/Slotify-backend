package com.slotify.module.customer.entity;

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

/**
 * A salon-defined label for customers such as "VIP" or "Allergic to ammonia" (table {@code
 * customer_tags}). Names are unique per salon; customers are linked through {@link
 * CustomerTagLink}.
 */
@Entity
@Table(name = "customer_tags")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CustomerTag extends BaseEntity {

  /** Colour used when the request does not specify one. */
  public static final String DEFAULT_COLOR = "#6B7280";

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "salon_id", nullable = false)
  private Salon salon;

  @Column(nullable = false, length = 50)
  private String name;

  /** Hex colour like {@code #FF8800}. */
  @Column(nullable = false, length = 7)
  private String color = DEFAULT_COLOR;

  /** Creates a tag of the salon; a null colour falls back to {@link #DEFAULT_COLOR}. */
  public static CustomerTag create(Salon salon, String name, String color) {
    CustomerTag tag = new CustomerTag();
    tag.salon = salon;
    tag.name = name;
    tag.color = color == null ? DEFAULT_COLOR : color;
    return tag;
  }
}
