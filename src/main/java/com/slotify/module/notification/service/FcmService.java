package com.slotify.module.notification.service;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.MessagingErrorCode;
import com.slotify.config.AppProperties;
import com.slotify.module.notification.dto.PushTestResponse;
import com.slotify.module.user.entity.DeviceToken;
import com.slotify.module.user.repository.DeviceTokenRepository;
import jakarta.annotation.PostConstruct;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
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
 * path is blank or the file does not exist yet the service is a no-op so the API works without
 * Firebase (e.g. in tests and local development). Tokens rejected as unregistered are deleted.
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
    if (!Files.isRegularFile(Path.of(path))) {
      log.warn(
          "Firebase service-account file not found at {} – push notifications disabled. "
              + "Download it from Firebase console > Project settings > Service accounts.",
          path);
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

  /** Whether Firebase credentials were loaded and pushes can be sent. */
  public boolean isEnabled() {
    return enabled;
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

  /**
   * Sends a test push to every device of the user synchronously and reports how many devices
   * Firebase accepted it for, so a developer can verify the setup end to end.
   */
  @Transactional
  public PushTestResponse sendTestToUser(Long userId, String title, String body) {
    List<DeviceToken> tokens = deviceTokenRepository.findAllByUserId(userId);
    if (!enabled) {
      return new PushTestResponse(false, tokens.size(), 0);
    }
    int delivered = 0;
    for (DeviceToken token : tokens) {
      if (send(token, title, body, Map.of("type", "TEST"))) {
        delivered++;
      }
    }
    return new PushTestResponse(true, tokens.size(), delivered);
  }

  /** Sends one message; returns whether Firebase accepted it. */
  private boolean send(DeviceToken token, String title, String body, Map<String, String> data) {
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
      return true;
    } catch (FirebaseMessagingException ex) {
      if (ex.getMessagingErrorCode() == MessagingErrorCode.UNREGISTERED
          || ex.getMessagingErrorCode() == MessagingErrorCode.INVALID_ARGUMENT) {
        log.info("Removing invalid FCM token for user {}", token.getUser().getId());
        deviceTokenRepository.delete(token);
      } else {
        log.warn("FCM send failed: {}", ex.getMessage());
      }
      return false;
    }
  }
}
