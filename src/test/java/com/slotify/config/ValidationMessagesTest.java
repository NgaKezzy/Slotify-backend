package com.slotify.config;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import java.math.BigDecimal;
import java.util.Locale;
import java.util.Set;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * Guards the Bean Validation bundles: the Vietnamese file must translate both the standard
 * constraint keys (Hibernate Validator ships no "vi" bundle) and the project's custom keys, and
 * the custom keys must resolve for the default (English) locale.
 */
class ValidationMessagesTest {

  private static Locale originalDefault;

  record Sample(
      @NotBlank String name,
      @DecimalMax("90.0") BigDecimal lat,
      @Pattern(regexp = "^\\+?[0-9 ()-]{6,32}$", message = "{validation.phone}") String phone) {}

  @BeforeAll
  static void setUp() {
    originalDefault = Locale.getDefault();
  }

  @AfterAll
  static void tearDown() {
    Locale.setDefault(originalDefault);
  }

  /** Hibernate Validator captures the default locale when the factory is built. */
  private static Validator validatorFor(Locale locale) {
    Locale.setDefault(locale);
    return Validation.buildDefaultValidatorFactory().getValidator();
  }

  @Test
  void vietnameseBundleTranslatesStandardAndCustomConstraints() {
    Set<ConstraintViolation<Sample>> violations =
        validatorFor(Locale.of("vi")).validate(new Sample("", new BigDecimal("105"), "abc"));

    assertThat(violations)
        .extracting(ConstraintViolation::getMessage)
        .containsExactlyInAnyOrder(
            "không được để trống",
            "phải nhỏ hơn hoặc bằng 90.0",
            "phải là số điện thoại hợp lệ");
  }

  @Test
  void englishBundleResolvesCustomConstraints() {
    Set<ConstraintViolation<Sample>> violations =
        validatorFor(Locale.ENGLISH).validate(new Sample("x", BigDecimal.ONE, "abc"));

    assertThat(violations)
        .extracting(ConstraintViolation::getMessage)
        .containsExactly("must be a valid phone number");
  }
}
