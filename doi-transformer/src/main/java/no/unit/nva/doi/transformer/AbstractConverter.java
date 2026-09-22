package no.unit.nva.doi.transformer;

import java.util.Locale;
import java.util.Set;
import java.util.stream.Stream;
import no.unit.nva.doi.fetch.commons.publication.model.PublicationDate;
import no.unit.nva.doi.transformer.language.LanguageDetector;
import nva.commons.core.StringUtils;
import nva.commons.doi.DoiConverter;

public class AbstractConverter {

  public static final String PLAIN_NAME_SEPARATOR = " ";
  public static final String UNDETERMINED_LANGUAGE = "und";
  private static final Set<String> ISO_LANGUAGE_CODES = Set.of(Locale.getISOLanguages());

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
    return toLanguageKey(languageDetector.detectLocale(title).getLanguage());
  }

  protected String toLanguageKey(String languageTag) {
    return StringUtils.isNotBlank(languageTag)
        ? toPrimarySubtag(languageTag)
        : UNDETERMINED_LANGUAGE;
  }

  private String toPrimarySubtag(String languageTag) {
    String primarySubtag = Locale.forLanguageTag(languageTag).getLanguage();
    return ISO_LANGUAGE_CODES.contains(primarySubtag) ? primarySubtag : UNDETERMINED_LANGUAGE;
  }
}
