package com.slotify.module.salon.service;

import com.slotify.module.salon.dto.SalonSummaryResponse;
import com.slotify.module.salon.entity.Favorite;
import com.slotify.module.salon.mapper.SalonMapper;
import com.slotify.module.salon.repository.FavoriteRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Customer favourites ({@code /me/favorites}). */
@Service
@RequiredArgsConstructor
@Transactional
public class FavoriteService {

  private final FavoriteRepository favoriteRepository;
  private final SalonQueryService salonQueryService;
  private final SalonMapper mapper;

  @Transactional(readOnly = true)
  public List<SalonSummaryResponse> list(Long userId) {
    return favoriteRepository.findSalonsByUserId(userId).stream().map(mapper::toSummary).toList();
  }

  public void add(Long userId, Long salonId) {
    salonQueryService.requireActive(String.valueOf(salonId));
    Favorite.Key key = new Favorite.Key(userId, salonId);
    if (!favoriteRepository.existsById(key)) {
      favoriteRepository.save(Favorite.of(userId, salonId));
    }
  }

  public void remove(Long userId, Long salonId) {
    favoriteRepository.deleteById(new Favorite.Key(userId, salonId));
  }
}
