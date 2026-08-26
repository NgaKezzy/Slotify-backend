package com.slotify.module.auth.repository;

import com.slotify.module.auth.entity.RefreshToken;
import java.time.Instant;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

/** Data access for {@link RefreshToken}. */
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

  Optional<RefreshToken> findByTokenHash(String tokenHash);

  @Modifying
  @Query(
      "update RefreshToken t set t.revoked = true where t.user.id = :userId and t.revoked = false")
  int revokeAllByUserId(Long userId);

  @Modifying
  @Query("delete from RefreshToken t where t.expiresAt < :before or t.revoked = true")
  int deleteExpiredOrRevoked(Instant before);
}
