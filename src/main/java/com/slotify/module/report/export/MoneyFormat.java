package com.slotify.module.report.export;

import java.math.BigDecimal;
import java.util.Currency;

/** Renders minor-unit amounts as text for exports, e.g. {@code 1250 EUR -> "12.50 EUR"}. */
public final class MoneyFormat {

  private static final int DEFAULT_FRACTION_DIGITS = 2;

  private MoneyFormat() {}

  /** Formats the amount using the currency's fraction digits (2 when the code is unknown). */
  public static String format(long amountMinor, String currencyCode) {
    int digits = DEFAULT_FRACTION_DIGITS;
    try {
      digits = Currency.getInstance(currencyCode).getDefaultFractionDigits();
    } catch (IllegalArgumentException ignored) {
      // Unknown ISO code: keep the default.
    }
    return BigDecimal.valueOf(amountMinor, digits).toPlainString() + " " + currencyCode;
  }
}
