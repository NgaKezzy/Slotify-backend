package com.slotify.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Properties;
import java.util.Set;
import org.junit.jupiter.api.Test;

/**
 * Guards the translation bundles: every supported locale must define exactly the keys of the
 * English base file, so no client ever sees a raw message key or a silent English fallback.
 */
class MessageBundlesTest {

  private static final String BASE_PATH = "i18n/messages";

  @Test
  void everyLocaleHasTheSameKeysAsTheBaseBundle() throws IOException {
    Set<String> baseKeys = load(BASE_PATH + ".properties").stringPropertyNames();
    assertThat(baseKeys).isNotEmpty();

    for (Locale locale : I18nConfig.SUPPORTED_LOCALES) {
      if (locale.equals(Locale.ENGLISH)) {
        continue; // English is the base file itself
      }
      String path = BASE_PATH + "_" + locale.getLanguage() + ".properties";
      Properties bundle = load(path);
      assertThat(bundle.stringPropertyNames())
          .as("keys of %s", path)
          .containsExactlyInAnyOrderElementsOf(baseKeys);
      bundle.forEach(
          (key, value) -> assertThat(value.toString()).as("%s in %s", key, path).isNotBlank());
    }
  }

  private static Properties load(String classpathResource) throws IOException {
    Properties properties = new Properties();
    try (InputStream in =
        MessageBundlesTest.class.getClassLoader().getResourceAsStream(classpathResource)) {
      assertThat(in).as("resource %s", classpathResource).isNotNull();
      properties.load(new InputStreamReader(in, StandardCharsets.UTF_8));
    }
    return properties;
  }
}
