package com.slotify.module.salon.repository;

import com.slotify.module.salon.entity.Amenity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/** Data access for global {@link Amenity}. */
public interface AmenityRepository extends JpaRepository<Amenity, Long> {

  List<Amenity> findAllByOrderByNameAsc();
}
