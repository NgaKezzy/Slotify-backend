package com.slotify.module.staff.repository;

import com.slotify.module.staff.entity.Staff;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** Data access for {@link Staff}. Soft-deleted rows are excluded automatically. */
public interface StaffRepository extends JpaRepository<Staff, Long> {

  List<Staff> findAllBySalonIdOrderByDisplayNameAsc(Long salonId);

  List<Staff> findAllBySalonIdAndActiveTrueOrderByDisplayNameAsc(Long salonId);

  Optional<Staff> findByIdAndSalonId(Long id, Long salonId);

  /** The staff row linked to a login account (a user is staff of at most one salon in v1). */
  Optional<Staff> findByUserId(Long userId);

  /**
   * Active staff of a salon who can perform <em>every</em> service in {@code serviceIds}. Used by
   * the availability engine for "any staff" searches.
   *
   * @param serviceIds distinct service ids
   * @param size {@code serviceIds.size()}
   */
  @Query(
      """
      SELECT s FROM Staff s JOIN s.services sv
      WHERE s.salon.id = :salonId AND s.active = true AND sv.id IN :serviceIds
      GROUP BY s
      HAVING COUNT(DISTINCT sv.id) = :size
      """)
  List<Staff> findActiveBySalonIdPerformingAllServices(
      @Param("salonId") Long salonId,
      @Param("serviceIds") Collection<Long> serviceIds,
      @Param("size") long size);
}
