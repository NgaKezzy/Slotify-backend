package com.slotify.config;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.ResourceBundleMessageSource;
import org.springframework.web.servlet.LocaleResolver;
import org.springframework.web.servlet.i18n.AcceptHeaderLocaleResolver;

/**
 * Internationalisation setup.
 *
 * <p>Messages live in {@code src/main/resources/i18n/messages_<lang>.properties}. The locale is
 * taken from the {@code Accept-Language} request header and falls back to English.
 */
@Configuration
public class I18nConfig {

  /** Languages shipped with the template. Add a properties file to support another one. */
  public static final List<Locale> SUPPORTED_LOCALES =
      List.of(
          Locale.ENGLISH,
          Locale.GERMAN,
          Locale.FRENCH,
          Locale.of("es"),
          Locale.ITALIAN,
          Locale.of("vi"));

  @Bean
  MessageSource messageSource() {
    ResourceBundleMessageSource source = new ResourceBundleMessageSource();
    source.setBasename("i18n/messages");
    source.setDefaultEncoding(StandardCharsets.UTF_8.name());
    source.setFallbackToSystemLocale(false);
    return source;
  }

  @Bean
  LocaleResolver localeResolver() {
    AcceptHeaderLocaleResolver resolver = new AcceptHeaderLocaleResolver();
    resolver.setDefaultLocale(Locale.ENGLISH);
    resolver.setSupportedLocales(SUPPORTED_LOCALES);
    return resolver;
  }
}
