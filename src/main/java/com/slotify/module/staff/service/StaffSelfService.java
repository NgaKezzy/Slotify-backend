package com.slotify.module.staff.service;

import com.slotify.common.exception.AppException;
import com.slotify.common.exception.ErrorCode;
import com.slotify.module.staff.dto.StaffMeResponse;
import com.slotify.module.staff.dto.StaffScheduleResponse;
import com.slotify.module.staff.dto.TimeOffRequest;
import com.slotify.module.staff.dto.TimeOffResponse;
import com.slotify.module.staff.entity.Staff;
import com.slotify.module.staff.entity.TimeOffStatus;
import com.slotify.module.staff.mapper.StaffMapper;
import com.slotify.module.staff.repository.StaffRepository;
import com.slotify.module.staff.repository.StaffShiftOverrideRepository;
import com.slotify.module.staff.repository.StaffShiftRepository;
import com.slotify.module.staff.repository.StaffTimeOffRepository;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Self-service operations of the Staff app ({@code /api/v1/staff/**}): profile, schedule and time
 * off of the staff member linked to the signed-in {@code STAFF} account.
 *
 * <p>Time off created here is {@link TimeOffStatus#PENDING} until the owner approves it.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class StaffSelfService {

  private final StaffRepository staffRepository;
  private final StaffShiftRepository shiftRepository;
  private final StaffShiftOverrideRepository overrideRepository;
  private final StaffTimeOffRepository timeOffRepository;
  private final StaffService staffService;
  private final StaffMapper mapper;
  private final Clock clock;

  /** Profile and salon summary of the signed-in staff member. */
  @Transactional(readOnly = true)
  public StaffMeResponse me(Long userId) {
    Staff staff = requireStaff(userId);
    return new StaffMeResponse(mapper.toResponse(staff), mapper.toSalonSummary(staff.getSalon()));
  }

  /** Weekly shifts plus per-date exceptions from today (salon local date) onwards. */
  @Transactional(readOnly = true)
  public StaffScheduleResponse schedule(Long userId) {
    Staff staff = requireStaff(userId);
    LocalDate today = LocalDate.now(clock.withZone(staff.getSalon().zoneId()));
    return new StaffScheduleResponse(
        mapper.toShiftResponses(
            shiftRepository.findAllByStaffIdOrderByDayOfWeekAscStartTimeAsc(staff.getId())),
        mapper.toOverrideResponses(
            overrideRepository.findAllByStaffIdAndDateGreaterThanEqualOrderByDateAsc(
                staff.getId(), today)));
  }

  @Transactional(readOnly = true)
  public List<TimeOffResponse> listTimeOff(Long userId) {
    Staff staff = requireStaff(userId);
    return mapper.toTimeOffResponses(
        timeOffRepository.findAllByStaffIdOrderByStartAtDesc(staff.getId()));
  }

  /** Requests an absence; it stays {@code PENDING} until the owner approves it. */
  public TimeOffResponse requestTimeOff(Long userId, TimeOffRequest request) {
    Staff staff = requireStaff(userId);
    return mapper.toResponse(staffService.createTimeOff(staff, request, TimeOffStatus.PENDING));
  }

  /** Withdraws one of the staff member's own absences. */
  public void deleteTimeOff(Long userId, Long timeOffId) {
    staffService.deleteTimeOff(requireStaff(userId), timeOffId);
  }

  /**
   * Resolves the staff row linked to the account.
   *
   * @throws AppException {@link ErrorCode#STAFF_NOT_FOUND} when the account is not linked to any
   *     staff member
   */
  private Staff requireStaff(Long userId) {
    return staffRepository
        .findByUserId(userId)
        .orElseThrow(() -> new AppException(ErrorCode.STAFF_NOT_FOUND));
  }
}
