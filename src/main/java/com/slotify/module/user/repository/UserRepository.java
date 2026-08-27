package com.slotify.module.user.repository;

import com.slotify.module.user.entity.Role;
import com.slotify.module.user.entity.User;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** Data access for {@link User}. Soft-deleted users are excluded automatically. */
public interface UserRepository extends JpaRepository<User, Long> {

  Optional<User> findByEmailIgnoreCase(String email);

  boolean existsByEmailIgnoreCase(String email);

  long countByRole(Role role);

  /** Every user holding one of the given roles (announcement audiences). */
  List<User> findAllByRoleIn(Collection<Role> roles);

  /**
   * Platform user search.
   *
   * @param pattern lower-cased {@code LIKE} pattern matched against email and full name, or {@code
   *     null} for no text filter
   * @param role role filter, or {@code null} for every role
   */
  @Query(
      """
      SELECT u FROM User u
      WHERE (:role IS NULL OR u.role = :role)
        AND (:pattern IS NULL OR LOWER(u.email) LIKE :pattern OR LOWER(u.fullName) LIKE :pattern)
      """)
  Page<User> search(@Param("pattern") String pattern, @Param("role") Role role, Pageable pageable);
}
