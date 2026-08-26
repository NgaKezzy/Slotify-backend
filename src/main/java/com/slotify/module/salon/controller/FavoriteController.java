package com.slotify.module.salon.controller;

import com.slotify.common.api.ApiResponse;
import com.slotify.module.salon.dto.SalonSummaryResponse;
import com.slotify.module.salon.service.FavoriteService;
import com.slotify.security.CurrentUser;
import com.slotify.security.UserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Favourite salons of the signed-in user. */
@Tag(name = "Me", description = "Profile of the signed-in user")
@RestController
@RequestMapping("/api/v1/me/favorites")
@RequiredArgsConstructor
public class FavoriteController {

  private final FavoriteService favoriteService;

  @Operation(summary = "List my favourite salons")
  @GetMapping
  public ApiResponse<List<SalonSummaryResponse>> list(@CurrentUser UserPrincipal principal) {
    return ApiResponse.ok(favoriteService.list(principal.id()));
  }

  @Operation(summary = "Add a salon to my favourites")
  @PostMapping("/{salonId}")
  public ApiResponse<Void> add(@CurrentUser UserPrincipal principal, @PathVariable Long salonId) {
    favoriteService.add(principal.id(), salonId);
    return ApiResponse.ok();
  }

  @Operation(summary = "Remove a salon from my favourites")
  @DeleteMapping("/{salonId}")
  public ApiResponse<Void> remove(
      @CurrentUser UserPrincipal principal, @PathVariable Long salonId) {
    favoriteService.remove(principal.id(), salonId);
    return ApiResponse.ok();
  }
}
