package com.slotify.module.staff.repository;

import com.slotify.module.staff.entity.StaffTimeOff;
import com.slotify.module.staff.entity.TimeOffStatus;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** Data access for {@link StaffTimeOff}. */
public interface StaffTimeOffRepository extends JpaRepository<StaffTimeOff, Long> {

  List<StaffTimeOff> findAllByStaffIdOrderByStartAtDesc(Long staffId);

  Optional<StaffTimeOff> findByIdAndStaffId(Long id, Long staffId);

  /**
   * Time-off entries of several staff members with the given status that overlap {@code [from,
   * to)}. The availability engine calls this with {@link TimeOffStatus#APPROVED}.
   */
  @Query(
      """
      SELECT t FROM StaffTimeOff t
      WHERE t.staff.id IN :staffIds AND t.status = :status
        AND t.startAt < :to AND t.endAt > :from
      """)
  List<StaffTimeOff> findAllOverlapping(
      @Param("staffIds") Collection<Long> staffIds,
      @Param("status") TimeOffStatus status,
      @Param("from") Instant from,
      @Param("to") Instant to);
}
