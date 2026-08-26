package com.slotify.common.entity;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import java.time.Instant;
import lombok.Getter;
import lombok.Setter;

/**
 * Base class for entities that are never physically removed.
 *
 * <p>Deleting sets {@code deleted_at}; repositories must filter on {@code deletedAt IS NULL} (use
 * {@code @SQLRestriction("deleted_at IS NULL")} on the entity).
 */
@Getter
@Setter
@MappedSuperclass
public abstract class SoftDeletableEntity extends BaseEntity {

  @Column(name = "deleted_at")
  private Instant deletedAt;

  /** Returns {@code true} when the entity has been soft-deleted. */
  public boolean isDeleted() {
    return deletedAt != null;
  }
}
