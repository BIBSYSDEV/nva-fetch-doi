package no.unit.nva.doi.transformer;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;

import java.util.Locale;
import no.unit.nva.doi.transformer.language.LanguageDetector;
import nva.commons.doi.DoiConverter;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

public class AbstractConverterTest {

  private static final String SOME_TITLE = "Some title";
  private static final String UNDETERMINED_LANGUAGE = "und";

  @Test
  public void detectLanguageReturnsUndeterminedWhenDetectorHasNoLanguage() {
    assertThat(detectLanguageWith(Locale.ROOT), is(equalTo(UNDETERMINED_LANGUAGE)));
  }

  @Test
  public void detectLanguageReturnsPrimarySubtagWhenDetectorHasLanguage() {
    assertThat(detectLanguageWith(Locale.ENGLISH), is(equalTo("en")));
  }

  @ParameterizedTest
  @CsvSource({"nb-NO,nb", "zh-Hant-TW,zh", "en-US,en", "nb,nb", "en,en"})
  public void toLanguageKeyReducesTagToPrimarySubtag(String languageTag, String expectedKey) {
    assertThat(converterWith(Locale.ROOT).toLanguageKey(languageTag), is(equalTo(expectedKey)));
  }

  @ParameterizedTest
  @NullAndEmptySource
  @ValueSource(
      strings = {" ", "not a language tag", "garbage", "xyz", "zzz", "not", "123", "en_US"})
  public void toLanguageKeyReturnsUndeterminedForUnusableTags(String languageTag) {
    assertThat(
        converterWith(Locale.ROOT).toLanguageKey(languageTag), is(equalTo(UNDETERMINED_LANGUAGE)));
  }

  private String detectLanguageWith(Locale detectedLocale) {
    return converterWith(detectedLocale).detectLanguage(SOME_TITLE);
  }

  private AbstractConverter converterWith(Locale detectedLocale) {
    LanguageDetector detector = input -> detectedLocale;
    return new AbstractConverter(detector, new DoiConverter());
  }
}
