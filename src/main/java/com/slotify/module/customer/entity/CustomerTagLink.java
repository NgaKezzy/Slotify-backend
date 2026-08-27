package com.slotify.module.customer.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.io.Serializable;
import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Assignment of a {@link CustomerTag} to a customer (table {@code customer_tag_links}; composite
 * key, no surrogate id). The salon scope comes from the tag.
 */
@Entity
@Table(name = "customer_tag_links")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CustomerTagLink {

  /** Composite primary key of {@link CustomerTagLink}. */
  @Embeddable
  @Getter
  @EqualsAndHashCode
  @NoArgsConstructor(access = AccessLevel.PROTECTED)
  public static class Key implements Serializable {

    @Column(name = "customer_id", nullable = false)
    private Long customerId;

    @Column(name = "tag_id", nullable = false)
    private Long tagId;

    public Key(Long customerId, Long tagId) {
      this.customerId = customerId;
      this.tagId = tagId;
    }
  }

  @EmbeddedId private Key id;

  public static CustomerTagLink of(Long customerId, Long tagId) {
    CustomerTagLink link = new CustomerTagLink();
    link.id = new Key(customerId, tagId);
    return link;
  }
}
