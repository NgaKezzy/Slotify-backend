package com.slotify.module.salon.service;

import com.slotify.module.salon.repository.SalonRepository;
import java.text.Normalizer;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/** Builds unique, URL-safe slugs for salons ("Glow & Go Berlin" → {@code glow-go-berlin}). */
@Component
@RequiredArgsConstructor
public class SlugGenerator {

  private static final int MAX_BASE_LENGTH = 140;

  private final SalonRepository salonRepository;

  /**
   * Returns a slug derived from the name that no other salon uses (appends -2, -3, … if needed).
   */
  public String uniqueSlugFor(String name) {
    String base = slugify(name);
    String candidate = base;
    int suffix = 2;
    while (salonRepository.existsBySlug(candidate)) {
      candidate = base + "-" + suffix++;
    }
    return candidate;
  }

  /** Lower-cases, strips accents and replaces anything non alphanumeric with a dash. */
  public static String slugify(String input) {
    String normalized = Normalizer.normalize(input, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
    String slug =
        normalized
            .toLowerCase(Locale.ROOT)
            .replaceAll("[^a-z0-9]+", "-")
            .replaceAll("(^-+|-+$)", "");
    if (slug.isEmpty()) {
      slug = "salon";
    }
    return slug.length() > MAX_BASE_LENGTH ? slug.substring(0, MAX_BASE_LENGTH) : slug;
  }
}
