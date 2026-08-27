package com.slotify.module.customer.repository;

import com.slotify.module.customer.entity.CustomerNote;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/** Data access for {@link CustomerNote}. */
public interface CustomerNoteRepository extends JpaRepository<CustomerNote, Long> {

  List<CustomerNote> findAllBySalonIdAndCustomerIdOrderByCreatedAtDesc(
      Long salonId, Long customerId);

  Optional<CustomerNote> findByIdAndSalonIdAndCustomerId(Long id, Long salonId, Long customerId);
}
