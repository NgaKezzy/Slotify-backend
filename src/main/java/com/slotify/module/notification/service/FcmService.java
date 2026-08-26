package com.slotify.module.notification.service;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.MessagingErrorCode;
import com.slotify.config.AppProperties;
import com.slotify.module.user.entity.DeviceToken;
import com.slotify.module.user.repository.DeviceTokenRepository;
import jakarta.annotation.PostConstruct;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Push notifications through Firebase Cloud Messaging.
 *
 * <p>Initialised from the service-account file in {@code app.firebase.credentials-path}; when the
 * path is blank the service is a no-op so the API works without Firebase (e.g. in tests and local
 * development). Tokens rejected as unregistered are deleted.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FcmService {

  private final AppProperties properties;
  private final DeviceTokenRepository deviceTokenRepository;
  private boolean enabled;

  @PostConstruct
  void init() {
    String path = properties.firebase().credentialsPath();
    if (path == null || path.isBlank()) {
      log.info("Firebase credentials not configured – push notifications disabled");
      return;
    }
    try (FileInputStream stream = new FileInputStream(path)) {
      if (FirebaseApp.getApps().isEmpty()) {
        FirebaseApp.initializeApp(
            FirebaseOptions.builder().setCredentials(GoogleCredentials.fromStream(stream)).build());
      }
      enabled = true;
      log.info("Firebase Cloud Messaging initialised");
    } catch (IOException ex) {
      log.error("Failed to initialise Firebase from {}: {}", path, ex.getMessage());
    }
  }

  /** Sends a push to every device of the user. Silently skipped when FCM is disabled. */
  @Async
  @Transactional
  public void sendToUser(Long userId, String title, String body, Map<String, String> data) {
    if (!enabled) {
      return;
    }
    List<DeviceToken> tokens = deviceTokenRepository.findAllByUserId(userId);
    for (DeviceToken token : tokens) {
      send(token, title, body, data);
    }
  }

  private void send(DeviceToken token, String title, String body, Map<String, String> data) {
    Message message =
        Message.builder()
            .setToken(token.getFcmToken())
            .setNotification(
                com.google.firebase.messaging.Notification.builder()
                    .setTitle(title)
                    .setBody(body)
                    .build())
            .putAllData(data == null ? Map.of() : data)
            .build();
    try {
      FirebaseMessaging.getInstance().send(message);
    } catch (FirebaseMessagingException ex) {
      if (ex.getMessagingErrorCode() == MessagingErrorCode.UNREGISTERED
          || ex.getMessagingErrorCode() == MessagingErrorCode.INVALID_ARGUMENT) {
        log.info("Removing invalid FCM token for user {}", token.getUser().getId());
        deviceTokenRepository.delete(token);
      } else {
        log.warn("FCM send failed: {}", ex.getMessage());
      }
    }
  }
}
