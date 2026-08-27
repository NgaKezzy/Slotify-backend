package com.slotify.module.customer.repository;

import com.slotify.module.customer.entity.CustomerTagLink;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** Data access for {@link CustomerTagLink}. */
public interface CustomerTagLinkRepository
    extends JpaRepository<CustomerTagLink, CustomerTagLink.Key> {

  /** Removes every tag of the given salon from the customer (tags of other salons are kept). */
  @Modifying
  @Query(
      """
      DELETE FROM CustomerTagLink l
      WHERE l.id.customerId = :customerId
        AND l.id.tagId IN (SELECT t.id FROM CustomerTag t WHERE t.salon.id = :salonId)
      """)
  void deleteAllForSalon(@Param("salonId") Long salonId, @Param("customerId") Long customerId);
}
