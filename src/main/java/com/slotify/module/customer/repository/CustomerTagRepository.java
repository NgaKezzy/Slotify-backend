package com.slotify.module.customer.repository;

import com.slotify.module.customer.entity.CustomerTag;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** Data access for {@link CustomerTag}. */
public interface CustomerTagRepository extends JpaRepository<CustomerTag, Long> {

  List<CustomerTag> findAllBySalonIdOrderByNameAsc(Long salonId);

  Optional<CustomerTag> findByIdAndSalonId(Long id, Long salonId);

  boolean existsBySalonIdAndNameIgnoreCase(Long salonId, String name);

  boolean existsBySalonIdAndNameIgnoreCaseAndIdNot(Long salonId, String name, Long id);

  long countBySalonIdAndIdIn(Long salonId, Collection<Long> ids);

  /** Tags of the salon assigned to one customer. */
  @Query(
      """
      SELECT t FROM CustomerTag t, CustomerTagLink l
      WHERE l.id.tagId = t.id AND l.id.customerId = :customerId AND t.salon.id = :salonId
      ORDER BY t.name
      """)
  List<CustomerTag> findAssigned(
      @Param("salonId") Long salonId, @Param("customerId") Long customerId);

  /** Tags of the salon assigned to any of the customers: rows of {@code [Long customerId, tag]}. */
  @Query(
      """
      SELECT l.id.customerId, t FROM CustomerTag t, CustomerTagLink l
      WHERE l.id.tagId = t.id AND l.id.customerId IN :customerIds AND t.salon.id = :salonId
      ORDER BY t.name
      """)
  List<Object[]> findAssignedForCustomers(
      @Param("salonId") Long salonId, @Param("customerIds") Collection<Long> customerIds);
}
