package com.slotify.module.notification.entity;

import com.slotify.common.entity.BaseEntity;
import com.slotify.module.user.entity.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.Map;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * In-app notification shown in the notification centre of the apps. The same event is also pushed
 * through FCM and WebSocket; this row is the persistent record.
 */
@Entity
@Table(name = "notifications")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Notification extends BaseEntity {

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "user_id", nullable = false)
  private User user;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 50)
  private NotificationType type;

  @Column(nullable = false, length = 150)
  private String title;

  @Column(nullable = false, length = 500)
  private String body;

  /** Deep-link payload, e.g. {@code {"bookingId": 42}}. */
  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name = "data_json")
  private Map<String, Object> data;

  @Column(name = "read_at")
  private Instant readAt;

  public static Notification of(
      User user, NotificationType type, String title, String body, Map<String, Object> data) {
    Notification notification = new Notification();
    notification.user = user;
    notification.type = type;
    notification.title = title;
    notification.body = body;
    notification.data = data;
    return notification;
  }

  public boolean isRead() {
    return readAt != null;
  }
}
