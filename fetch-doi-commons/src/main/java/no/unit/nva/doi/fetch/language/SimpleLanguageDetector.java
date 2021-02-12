package no.unit.nva.doi.fetch.language;

import java.util.Locale;

public class SimpleLanguageDetector implements LanguageDetector {

    @Override
    public Locale detectLocale(String input) {
        return Locale.ENGLISH;
    }
}
