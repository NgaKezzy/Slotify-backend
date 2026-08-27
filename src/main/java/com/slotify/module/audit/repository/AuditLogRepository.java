package com.slotify.module.audit.repository;

import com.slotify.module.audit.entity.AuditAction;
import com.slotify.module.audit.entity.AuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** Data access for {@link AuditLog}. */
public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

  /**
   * Pages the log of one salon, newest first. {@code entity} and {@code action} are optional
   * filters ({@code null} matches everything).
   */
  @Query(
      """
      SELECT a FROM AuditLog a
      WHERE a.salonId = :salonId
        AND (:entity IS NULL OR a.entity = :entity)
        AND (:action IS NULL OR a.action = :action)
      ORDER BY a.createdAt DESC, a.id DESC
      """)
  Page<AuditLog> findBySalon(
      @Param("salonId") Long salonId,
      @Param("entity") String entity,
      @Param("action") AuditAction action,
      Pageable pageable);

  /** Pages the platform-wide log (every salon plus platform events), newest first. */
  @Query(
      """
      SELECT a FROM AuditLog a
      WHERE (:entity IS NULL OR a.entity = :entity)
        AND (:action IS NULL OR a.action = :action)
      ORDER BY a.createdAt DESC, a.id DESC
      """)
  Page<AuditLog> findAllFiltered(
      @Param("entity") String entity, @Param("action") AuditAction action, Pageable pageable);
}
