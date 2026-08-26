package com.slotify.module.user.repository;

import com.slotify.module.user.entity.DeviceToken;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/** Data access for {@link DeviceToken}. */
public interface DeviceTokenRepository extends JpaRepository<DeviceToken, Long> {

  Optional<DeviceToken> findByFcmToken(String fcmToken);

  List<DeviceToken> findAllByUserId(Long userId);

  void deleteByFcmToken(String fcmToken);
}
