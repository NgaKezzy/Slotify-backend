package com.slotify.module.service.repository;

import com.slotify.module.service.entity.SalonService;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/** Data access for {@link SalonService}. Soft-deleted rows are excluded automatically. */
public interface SalonServiceRepository extends JpaRepository<SalonService, Long> {

  List<SalonService> findAllBySalonIdOrderBySortOrderAscNameAsc(Long salonId);

  List<SalonService> findAllBySalonIdAndActiveTrueOrderBySortOrderAscNameAsc(Long salonId);

  Optional<SalonService> findByIdAndSalonId(Long id, Long salonId);

  List<SalonService> findAllByIdInAndSalonIdAndActiveTrue(Collection<Long> ids, Long salonId);
}
