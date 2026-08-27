package com.slotify.module.notification.service;

import com.slotify.common.api.PageResponse;
import com.slotify.common.exception.AppException;
import com.slotify.common.exception.ErrorCode;
import com.slotify.module.notification.dto.BroadcastRequest;
import com.slotify.module.notification.dto.BroadcastResponse;
import com.slotify.module.notification.dto.NotificationResponse;
import com.slotify.module.notification.entity.Notification;
import com.slotify.module.notification.entity.NotificationType;
import com.slotify.module.notification.repository.NotificationRepository;
import com.slotify.module.user.entity.User;
import com.slotify.module.user.repository.UserRepository;
import java.time.Clock;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.MessageSource;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Creates in-app notifications (persisted, localised in the recipient's language) and fans them out
 * as push messages. Real-time WebSocket events are published separately by the booking module.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class NotificationService {

  private final NotificationRepository notificationRepository;
  private final UserRepository userRepository;
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

  /**
   * Stores a free-text announcement for every user of the audience and pushes it to their devices.
   * Text is sent as written (no per-locale templates), so admins write it in the language of their
   * user base.
   */
  public BroadcastResponse broadcast(BroadcastRequest request) {
    List<User> recipients = userRepository.findAllByRoleIn(request.audience().roles());
    Map<String, Object> data = Map.of("audience", request.audience().name());
    List<Notification> notifications =
        recipients.stream()
            .map(
                user ->
                    Notification.of(
                        user, NotificationType.ANNOUNCEMENT, request.title(), request.body(), data))
            .toList();
    notificationRepository.saveAll(notifications);
    Map<String, String> pushData = Map.of("type", NotificationType.ANNOUNCEMENT.name());
    for (User user : recipients) {
      fcmService.sendToUser(user.getId(), request.title(), request.body(), pushData);
    }
    log.info(
        "Broadcast '{}' sent to {} {} users",
        request.title(),
        recipients.size(),
        request.audience());
    return new BroadcastResponse(recipients.size());
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
