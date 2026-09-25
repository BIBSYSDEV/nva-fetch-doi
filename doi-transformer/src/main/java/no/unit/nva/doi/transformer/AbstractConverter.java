package no.unit.nva.doi.transformer;

import java.util.Arrays;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import no.unit.nva.doi.fetch.commons.publication.model.PublicationDate;
import no.unit.nva.doi.transformer.language.LanguageDetector;
import nva.commons.core.StringUtils;
import nva.commons.doi.DoiConverter;

public class AbstractConverter {

  public static final String PLAIN_NAME_SEPARATOR = " ";
  public static final String UNDETERMINED_LANGUAGE = "und";
  private static final Set<String> ISO_LANGUAGE_CODES = Set.of(Locale.getISOLanguages());
  private static final Map<String, String> BIBLIOGRAPHIC_TO_TWO_LETTER_CODES =
      Map.ofEntries(
          Map.entry("alb", "sq"),
          Map.entry("arm", "hy"),
          Map.entry("baq", "eu"),
          Map.entry("bur", "my"),
          Map.entry("chi", "zh"),
          Map.entry("cze", "cs"),
          Map.entry("dut", "nl"),
          Map.entry("fre", "fr"),
          Map.entry("geo", "ka"),
          Map.entry("ger", "de"),
          Map.entry("gre", "el"),
          Map.entry("ice", "is"),
          Map.entry("mac", "mk"),
          Map.entry("mao", "mi"),
          Map.entry("may", "ms"),
          Map.entry("per", "fa"),
          Map.entry("rum", "ro"),
          Map.entry("slo", "sk"),
          Map.entry("tib", "bo"),
          Map.entry("wel", "cy"));
  private static final Map<String, String> THREE_LETTER_TO_TWO_LETTER_CODES =
      threeLetterToTwoLetterCodes();

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
    var primarySubtag = Locale.forLanguageTag(languageTag).getLanguage();
    return ISO_LANGUAGE_CODES.contains(primarySubtag)
        ? primarySubtag
        : THREE_LETTER_TO_TWO_LETTER_CODES.getOrDefault(primarySubtag, UNDETERMINED_LANGUAGE);
  }

  private static Map<String, String> threeLetterToTwoLetterCodes() {
    return Stream.concat(terminologyCodes(), BIBLIOGRAPHIC_TO_TWO_LETTER_CODES.entrySet().stream())
        .collect(
            Collectors.toUnmodifiableMap(
                Entry::getKey, Entry::getValue, (firstCode, duplicateCode) -> firstCode));
  }

  private static Stream<Entry<String, String>> terminologyCodes() {
    return Arrays.stream(Locale.getISOLanguages())
        .map(twoLetterCode -> Map.entry(Locale.of(twoLetterCode).getISO3Language(), twoLetterCode));
  }
}
