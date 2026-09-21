package no.unit.nva.doi.transformer;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;

import java.util.Locale;
import no.unit.nva.doi.transformer.language.LanguageDetector;
import nva.commons.doi.DoiConverter;
import org.junit.jupiter.api.Test;

public class AbstractConverterTest {

  private static final String SOME_TITLE = "Some title";

  @Test
  public void detectLanguageReturnsUndeterminedWhenDetectorHasNoLanguage() {
    assertThat(detectLanguageWith(Locale.ROOT), is(equalTo("und")));
  }

  @Test
  public void detectLanguageKeepsRegionAndScriptSubtags() {
    assertThat(detectLanguageWith(Locale.forLanguageTag("nb-NO")), is(equalTo("nb-NO")));
    assertThat(detectLanguageWith(Locale.forLanguageTag("zh-Hant-TW")), is(equalTo("zh-Hant-TW")));
  }

  private String detectLanguageWith(Locale detectedLocale) {
    LanguageDetector detector = input -> detectedLocale;
    return new AbstractConverter(detector, new DoiConverter()).detectLanguage(SOME_TITLE);
  }
}
