package com.slotify.common.api;

import java.util.List;
import java.util.function.Function;
import org.springframework.data.domain.Page;

/**
 * Pagination envelope used by every list endpoint.
 *
 * @param items items of the current page
 * @param page zero-based page index
 * @param size requested page size
 * @param totalItems total number of matching items
 * @param totalPages total number of pages
 * @param <T> item type
 */
public record PageResponse<T>(List<T> items, int page, int size, long totalItems, int totalPages) {

  /** Wraps a Spring Data page as-is. */
  public static <T> PageResponse<T> from(Page<T> page) {
    return new PageResponse<>(
        page.getContent(),
        page.getNumber(),
        page.getSize(),
        page.getTotalElements(),
        page.getTotalPages());
  }

  /** Wraps a Spring Data page, mapping each element (typically entity to DTO). */
  public static <E, T> PageResponse<T> from(Page<E> page, Function<E, T> mapper) {
    return from(page.map(mapper));
  }
}
