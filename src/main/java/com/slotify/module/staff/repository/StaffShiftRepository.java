package com.slotify.module.staff.repository;

import com.slotify.module.staff.entity.StaffShift;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** Data access for {@link StaffShift} (weekly schedule). */
public interface StaffShiftRepository extends JpaRepository<StaffShift, Long> {

  List<StaffShift> findAllByStaffIdOrderByDayOfWeekAscStartTimeAsc(Long staffId);

  /** Shifts of several staff members on one weekday (0 = Monday), for the availability engine. */
  List<StaffShift> findAllByStaffIdInAndDayOfWeekOrderByStartTimeAsc(
      Collection<Long> staffIds, int dayOfWeek);

  @Modifying
  @Query("DELETE FROM StaffShift s WHERE s.staff.id = :staffId")
  void deleteAllByStaffId(@Param("staffId") Long staffId);
}
