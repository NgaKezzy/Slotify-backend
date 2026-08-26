package com.slotify.module.salon.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.Table;
import java.io.Serializable;
import java.time.Instant;
import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

/** A salon bookmarked by a customer (composite key user + salon, no surrogate id). */
@Entity
@Table(name = "favorites")
@EntityListeners(AuditingEntityListener.class)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Favorite {

  /** Composite primary key of {@link Favorite}. */
  @Embeddable
  @Getter
  @EqualsAndHashCode
  @NoArgsConstructor(access = AccessLevel.PROTECTED)
  public static class Key implements Serializable {

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "salon_id", nullable = false)
    private Long salonId;

    public Key(Long userId, Long salonId) {
      this.userId = userId;
      this.salonId = salonId;
    }
  }

  @EmbeddedId private Key id;

  @CreatedDate
  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;

  public static Favorite of(Long userId, Long salonId) {
    Favorite favorite = new Favorite();
    favorite.id = new Key(userId, salonId);
    return favorite;
  }
}
