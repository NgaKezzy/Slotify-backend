package com.slotify.module.service.repository;

import com.slotify.module.service.entity.ServiceCategory;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/** Data access for {@link ServiceCategory}. */
public interface ServiceCategoryRepository extends JpaRepository<ServiceCategory, Long> {

  List<ServiceCategory> findAllBySalonIdOrderBySortOrderAscNameAsc(Long salonId);

  Optional<ServiceCategory> findByIdAndSalonId(Long id, Long salonId);
}
