package com.slotify.module.salon.repository;

import com.slotify.module.salon.entity.Salon;
import com.slotify.module.salon.entity.SalonStatus;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

/** Data access for {@link Salon}. Public search uses {@link SalonSpecifications}. */
public interface SalonRepository
    extends JpaRepository<Salon, Long>, JpaSpecificationExecutor<Salon> {

  Optional<Salon> findBySlug(String slug);

  boolean existsBySlug(String slug);

  List<Salon> findAllByOwnerIdOrderByNameAsc(Long ownerId);

  Optional<Salon> findByIdAndStatus(Long id, SalonStatus status);
}
