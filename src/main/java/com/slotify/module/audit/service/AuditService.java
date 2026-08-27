package com.slotify.module.audit.service;

import com.slotify.common.api.PageResponse;
import com.slotify.common.exception.AppException;
import com.slotify.common.exception.ErrorCode;
import com.slotify.module.audit.dto.AuditLogResponse;
import com.slotify.module.audit.entity.AuditAction;
import com.slotify.module.audit.entity.AuditLog;
import com.slotify.module.audit.repository.AuditLogRepository;
import com.slotify.module.salon.service.SalonAccess;
import com.slotify.security.UserPrincipal;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Writes and reads the audit trail.
 *
 * <p>{@link #record} is designed to be sprinkled into business services without any risk: it runs
 * in its own transaction (so a failure here never poisons the caller's persistence context) and
 * swallows every exception after logging it. Audit must never break a booking or a payment.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuditService {

  /** Upper bound for page size on the list endpoints. */
  public static final int MAX_PAGE_SIZE = 100;

  private final AuditLogRepository repository;
  private final SalonAccess salonAccess;

  /**
   * Stores an audit entry. Safe to call from anywhere, including outside a transaction or from
   * async listeners; never throws.
   *
   * @param actor who performed the action ({@code null} for system jobs)
   * @param salonId salon scope ({@code null} for platform events)
   * @param action what happened
   * @param entity logical entity name, e.g. {@code "Booking"}
   * @param entityId id of the affected entity
   * @param diff optional structured description of the change (see {@link AuditDiff})
   */
  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public void record(
      UserPrincipal actor,
      Long salonId,
      AuditAction action,
      String entity,
      Long entityId,
      Map<String, Object> diff) {
    try {
      Long actorId = actor == null ? null : actor.id();
      repository.saveAndFlush(AuditLog.of(actorId, salonId, action, entity, entityId, diff));
    } catch (RuntimeException ex) {
      log.error("Failed to write audit log {} {}#{}: {}", action, entity, entityId, ex.toString());
    }
  }

  /** Audit trail of one salon; the caller must own the salon (or be SUPER_ADMIN). */
  @Transactional(readOnly = true)
  public PageResponse<AuditLogResponse> listForSalon(
      Long salonId, UserPrincipal principal, String entity, AuditAction action, Pageable page) {
    salonAccess.requireOwned(salonId, principal);
    return PageResponse.from(
        repository.findBySalon(salonId, blankToNull(entity), action, page), AuditService::toDto);
  }

  /** Platform-wide audit trail (SUPER_ADMIN only, enforced by the controller). */
  @Transactional(readOnly = true)
  public PageResponse<AuditLogResponse> listAll(String entity, AuditAction action, Pageable page) {
    return PageResponse.from(
        repository.findAllFiltered(blankToNull(entity), action, page), AuditService::toDto);
  }

  /** Builds a bounded page request; the repository queries carry their own ordering. */
  public static Pageable pageable(int page, int size) {
    if (page < 0 || size < 1) {
      throw new AppException(ErrorCode.VALIDATION_FAILED);
    }
    return PageRequest.of(page, Math.min(size, MAX_PAGE_SIZE));
  }

  private static AuditLogResponse toDto(AuditLog log) {
    return new AuditLogResponse(
        log.getId(),
        log.getActorId(),
        log.getSalonId(),
        log.getAction(),
        log.getEntity(),
        log.getEntityId(),
        log.getDiff(),
        log.getCreatedAt());
  }

  private static String blankToNull(String value) {
    return value == null || value.isBlank() ? null : value.trim();
  }
}
