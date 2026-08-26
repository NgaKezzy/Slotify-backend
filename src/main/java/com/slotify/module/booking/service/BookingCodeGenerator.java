package com.slotify.module.booking.service;

import java.security.SecureRandom;
import org.springframework.stereotype.Component;

/**
 * Generates short human-friendly booking references such as {@code SLT-8F3K2}.
 *
 * <p>Uses an unambiguous alphabet (no 0/O, 1/I) so codes can be read over the phone. Uniqueness is
 * enforced by the database; callers retry on collision.
 */
@Component
public class BookingCodeGenerator {

  /** Prefix shown before the dash; change to rebrand ("SLT" = Slotify). */
  public static final String PREFIX = "SLT";

  private static final String ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
  private static final int LENGTH = 5;

  private final SecureRandom random = new SecureRandom();

  public String next() {
    StringBuilder code = new StringBuilder(PREFIX).append('-');
    for (int i = 0; i < LENGTH; i++) {
      code.append(ALPHABET.charAt(random.nextInt(ALPHABET.length())));
    }
    return code.toString();
  }
}
