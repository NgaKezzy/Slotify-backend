package com.slotify.module.notification.repository;

import com.slotify.module.notification.entity.Notification;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

/** Data access for {@link Notification}. */
public interface NotificationRepository extends JpaRepository<Notification, Long> {

  Page<Notification> findAllByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);

  Optional<Notification> findByIdAndUserId(Long id, Long userId);

  /** Full history of a user (GDPR export). */
  List<Notification> findAllByUserIdOrderByCreatedAtDesc(Long userId);

  /** Removes every notification of a user (GDPR deletion). */
  void deleteAllByUserId(Long userId);

  long countByUserIdAndReadAtIsNull(Long userId);

  @Modifying
  @Query("update Notification n set n.readAt = :now where n.user.id = :userId and n.readAt is null")
  int markAllRead(Long userId, Instant now);
}
