package com.slotify.module.staff.repository;

import com.slotify.module.staff.entity.StaffShiftOverride;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/** Data access for {@link StaffShiftOverride} (per-date schedule exceptions). */
public interface StaffShiftOverrideRepository extends JpaRepository<StaffShiftOverride, Long> {

  Optional<StaffShiftOverride> findByStaffIdAndDate(Long staffId, LocalDate date);

  Optional<StaffShiftOverride> findByIdAndStaffId(Long id, Long staffId);

  /** Overrides on or after a date, for the "upcoming exceptions" views. */
  List<StaffShiftOverride> findAllByStaffIdAndDateGreaterThanEqualOrderByDateAsc(
      Long staffId, LocalDate from);

  /** Overrides of several staff members on one date, for the availability engine. */
  List<StaffShiftOverride> findAllByStaffIdInAndDate(Collection<Long> staffIds, LocalDate date);
}
