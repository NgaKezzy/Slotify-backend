package com.slotify.module.audit.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.Map;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

/**
 * Immutable record of a sensitive change (who did what to which entity, and what changed).
 *
 * <p>Rows are append-only; the table has no {@code updated_at}, so this entity deliberately does
 * not extend {@code BaseEntity}. Actor and salon are stored as plain ids (no associations) so that
 * a log line survives the anonymisation or removal of the user it refers to.
 */
@Entity
@Table(name = "audit_logs")
@EntityListeners(AuditingEntityListener.class)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AuditLog {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  /** User who performed the action; {@code null} for system jobs or anonymous callers. */
  @Column(name = "actor_id")
  private Long actorId;

  /** Salon the change belongs to; {@code null} for platform-level events. */
  @Column(name = "salon_id")
  private Long salonId;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 50)
  private AuditAction action;

  /** Logical entity name, e.g. {@code "Booking"} or {@code "SalonSettings"}. */
  @Column(nullable = false, length = 50)
  private String entity;

  @Column(name = "entity_id", nullable = false)
  private Long entityId;

  /** Free-form change description, typically {@code {field: {from, to}}}. */
  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name = "diff_json")
  private Map<String, Object> diff;

  @CreatedDate
  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;

  /** Creates a log entry; {@code createdAt} is filled by JPA auditing on persist. */
  public static AuditLog of(
      Long actorId,
      Long salonId,
      AuditAction action,
      String entity,
      Long entityId,
      Map<String, Object> diff) {
    AuditLog log = new AuditLog();
    log.actorId = actorId;
    log.salonId = salonId;
    log.action = action;
    log.entity = entity;
    log.entityId = entityId;
    log.diff = diff;
    return log;
  }
}
