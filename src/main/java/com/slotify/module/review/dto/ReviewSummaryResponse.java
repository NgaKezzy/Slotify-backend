package com.slotify.module.review.dto;

import java.math.BigDecimal;
import java.util.Map;

/**
 * Rating overview shown on the salon page.
 *
 * @param average average of visible ratings (two decimals, 0 when there are none)
 * @param count number of visible reviews
 * @param distribution number of visible reviews per rating value 1..5 (every key present)
 */
public record ReviewSummaryResponse(
    BigDecimal average, long count, Map<Integer, Long> distribution) {}
