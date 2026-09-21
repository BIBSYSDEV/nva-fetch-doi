package no.unit.nva.doi.transformer;

import java.util.stream.Stream;
import no.unit.nva.doi.fetch.commons.publication.model.PublicationDate;
import no.unit.nva.doi.transformer.language.LanguageDetector;
import nva.commons.core.StringUtils;
import nva.commons.doi.DoiConverter;

public class AbstractConverter {

  public static final String PLAIN_NAME_SEPARATOR = " ";
  public static final String UNDETERMINED_LANGUAGE = "und";

  protected DoiConverter doiConverter;
  protected LanguageDetector languageDetector;

  public AbstractConverter(LanguageDetector detector, DoiConverter doiConverter) {
    this.languageDetector = detector;
    this.doiConverter = doiConverter;
  }

  protected String toName(String givenName, String familyName) {
    return String.join(PLAIN_NAME_SEPARATOR, givenName, familyName);
  }

  protected PublicationDate toDate(Integer publicationYear) {
    return new PublicationDate(publicationYear.toString(), null, null);
  }

  protected String getMainTitle(Stream<String> titles) {
    return titles.findFirst().orElse(null);
  }

  protected String detectLanguage(String title) {
    return toBcp47LanguageTag(languageDetector.detectLocale(title).getLanguage());
  }

  private String toBcp47LanguageTag(String languageCode) {
    return StringUtils.isNotBlank(languageCode) ? languageCode : UNDETERMINED_LANGUAGE;
  }
}
