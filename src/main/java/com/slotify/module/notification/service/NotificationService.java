package com.slotify.module.notification.service;

import com.slotify.common.api.PageResponse;
import com.slotify.common.exception.AppException;
import com.slotify.common.exception.ErrorCode;
import com.slotify.module.notification.dto.NotificationResponse;
import com.slotify.module.notification.entity.Notification;
import com.slotify.module.notification.entity.NotificationType;
import com.slotify.module.notification.repository.NotificationRepository;
import com.slotify.module.user.entity.User;
import java.time.Clock;
import java.time.Instant;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.context.MessageSource;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Creates in-app notifications (persisted, localised in the recipient's language) and fans them out
 * as push messages. Real-time WebSocket events are published separately by the booking module.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class NotificationService {

  private final NotificationRepository notificationRepository;
  private final FcmService fcmService;
  private final MessageSource messageSource;
  private final Clock clock;

  /**
   * Notifies a user. Title and body come from {@code notification.<type>.title|body} with the given
   * arguments; {@code data} is attached for deep links (values are also sent as FCM data).
   */
  public Notification notify(
      User user, NotificationType type, Map<String, Object> data, Object... args) {
    Locale locale = Locale.forLanguageTag(user.getLocale());
    String title = messageSource.getMessage(type.messageKey() + ".title", args, locale);
    String body = messageSource.getMessage(type.messageKey() + ".body", args, locale);

    Notification notification =
        notificationRepository.save(Notification.of(user, type, title, body, data));

    Map<String, String> pushData = new HashMap<>();
    pushData.put("type", type.name());
    pushData.put("notificationId", String.valueOf(notification.getId()));
    if (data != null) {
      data.forEach((k, v) -> pushData.put(k, String.valueOf(v)));
    }
    fcmService.sendToUser(user.getId(), title, body, pushData);
    return notification;
  }

  @Transactional(readOnly = true)
  public PageResponse<NotificationResponse> list(Long userId, int page, int size) {
    return PageResponse.from(
        notificationRepository.findAllByUserIdOrderByCreatedAtDesc(
            userId, PageRequest.of(page, Math.min(size, 50))),
        this::toResponse);
  }

  @Transactional(readOnly = true)
  public long unreadCount(Long userId) {
    return notificationRepository.countByUserIdAndReadAtIsNull(userId);
  }

  public void markRead(Long userId, Long notificationId) {
    Notification notification =
        notificationRepository
            .findByIdAndUserId(notificationId, userId)
            .orElseThrow(
                () -> new AppException(ErrorCode.NOT_FOUND, "Notification", notificationId));
    if (!notification.isRead()) {
      notification.setReadAt(Instant.now(clock));
    }
  }

  public void markAllRead(Long userId) {
    notificationRepository.markAllRead(userId, Instant.now(clock));
  }

  private NotificationResponse toResponse(Notification n) {
    return new NotificationResponse(
        n.getId(),
        n.getType(),
        n.getTitle(),
        n.getBody(),
        n.getData(),
        n.isRead(),
        n.getCreatedAt());
  }
}
