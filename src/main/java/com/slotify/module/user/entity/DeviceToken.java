package com.slotify.module.user.entity;

import com.slotify.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Firebase Cloud Messaging registration token of one installed app instance.
 *
 * <p>A user may own several tokens (multiple devices, customer and staff app). Tokens are removed
 * when FCM reports them as invalid or when the user signs out on that device.
 */
@Entity
@Table(name = "device_tokens")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class DeviceToken extends BaseEntity {

  /** Mobile platform of the device. */
  public enum Platform {
    ANDROID,
    IOS,
    WEB
  }

  /** Which of the two Flutter apps registered the token. */
  public enum AppType {
    CUSTOMER,
    STAFF
  }

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "user_id", nullable = false)
  private User user;

  @Column(name = "fcm_token", nullable = false, length = 512)
  private String fcmToken;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private Platform platform;

  @Enumerated(EnumType.STRING)
  @Column(name = "app_type", nullable = false)
  private AppType appType;

  /** Creates a token registration. */
  public static DeviceToken of(User user, String fcmToken, Platform platform, AppType appType) {
    DeviceToken token = new DeviceToken();
    token.user = user;
    token.fcmToken = fcmToken;
    token.platform = platform;
    token.appType = appType;
    return token;
  }
}
