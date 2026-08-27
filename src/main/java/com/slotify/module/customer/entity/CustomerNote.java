package com.slotify.module.customer.entity;

import com.slotify.common.entity.BaseEntity;
import com.slotify.module.salon.entity.Salon;
import com.slotify.module.user.entity.User;
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
 * An internal CRM note a salon keeps about one of its customers (table {@code customer_notes}).
 * Notes are private to the salon and never shown to the customer.
 */
@Entity
@Table(name = "customer_notes")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CustomerNote extends BaseEntity {

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "salon_id", nullable = false)
  private Salon salon;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "customer_id", nullable = false)
  private User customer;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "author_id", nullable = false)
  private User author;

  @Column(nullable = false, columnDefinition = "TEXT")
  private String note;

  /** Creates a note written by {@code author} about {@code customer} at {@code salon}. */
  public static CustomerNote create(Salon salon, User customer, User author, String note) {
    CustomerNote entity = new CustomerNote();
    entity.salon = salon;
    entity.customer = customer;
    entity.author = author;
    entity.note = note;
    return entity;
  }
}
