package com.slotify.config;

import java.util.Locale;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.core.convert.converter.ConverterFactory;
import org.springframework.format.FormatterRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Binds enum query parameters case-insensitively, so clients may send {@code ?type=excel} or {@code
 * ?granularity=day} as documented in the plan while the Java enums stay upper-case.
 *
 * <p>Only affects request parameters and path variables; JSON bodies are handled by Jackson.
 */
@Configuration
public class WebConversionConfig implements WebMvcConfigurer {

  @Override
  public void addFormatters(FormatterRegistry registry) {
    registry.addConverterFactory(new CaseInsensitiveEnumConverterFactory());
  }

  /** Converts a string to any enum type by upper-casing it first. */
  static final class CaseInsensitiveEnumConverterFactory
      implements ConverterFactory<String, Enum<?>> {

    @Override
    @SuppressWarnings({"unchecked", "rawtypes"})
    public <T extends Enum<?>> Converter<String, T> getConverter(Class<T> targetType) {
      return source -> (T) Enum.valueOf((Class) targetType, source.trim().toUpperCase(Locale.ROOT));
    }
  }
}
