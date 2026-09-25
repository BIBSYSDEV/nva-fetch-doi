package no.unit.nva.doi.transformer.language;

import java.util.Locale;

@FunctionalInterface
public interface LanguageDetector {

  Locale detectLocale(String input);
}
