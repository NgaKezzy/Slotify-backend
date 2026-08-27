package com.slotify.module.salon.repository;

import com.slotify.module.salon.entity.Favorite;
import com.slotify.module.salon.entity.Salon;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

/** Data access for {@link Favorite}. */
public interface FavoriteRepository extends JpaRepository<Favorite, Favorite.Key> {

  @Query(
      "select s from Salon s where s.id in "
          + "(select f.id.salonId from Favorite f where f.id.userId = :userId) order by s.name")
  List<Salon> findSalonsByUserId(Long userId);

  /** Removes every favourite of a user (GDPR deletion). */
  void deleteAllByIdUserId(Long userId);
}
