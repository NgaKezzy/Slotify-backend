package com.slotify.module.auth.repository;

import com.slotify.module.auth.entity.PasswordResetToken;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/** Data access for {@link PasswordResetToken}. */
public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, Long> {

  Optional<PasswordResetToken> findByTokenHash(String tokenHash);

  void deleteAllByUserId(Long userId);
}
