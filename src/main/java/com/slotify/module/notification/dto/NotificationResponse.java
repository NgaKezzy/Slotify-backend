package com.slotify.module.notification.dto;

import com.slotify.module.notification.entity.NotificationType;
import java.time.Instant;
import java.util.Map;

/** In-app notification as shown in the notification centre. */
public record NotificationResponse(
    Long id,
    NotificationType type,
    String title,
    String body,
    Map<String, Object> data,
    boolean read,
    Instant createdAt) {}
