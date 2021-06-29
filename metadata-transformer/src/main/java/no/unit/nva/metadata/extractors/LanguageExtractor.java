package no.unit.nva.metadata.extractors;

import no.unit.nva.model.EntityDescription;
import nva.commons.core.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.function.Function;

public final class LanguageExtractor {
    private static final Logger logger = LoggerFactory.getLogger(LanguageExtractor.class);
    public static final String LEXVO_ORG = "http://lexvo.org/id/iso639-3/";
    public static final String ISO3_LANGUAGE_CODE_UNDEFINED = "und";
    public static final Function<ExtractionPair, EntityDescription> apply = LanguageExtractor::extract;

    private LanguageExtractor() {

    }

    private static EntityDescription extract(ExtractionPair extractionPair) {
        if (extractionPair.isLanguage()) {
            addLanguage(extractionPair);
        }
        return extractionPair.getEntityDescription();
    }

    private static void addLanguage(ExtractionPair extractionPair) {
        extractionPair.getEntityDescription().setLanguage(toLexvoUri(extractionPair.getStatementLiteral()));
    }

    private static URI toLexvoUri(String language) {
        String iso3LanguageCode = ISO3_LANGUAGE_CODE_UNDEFINED;
        if (!StringUtils.isEmpty(language)) {
            try {
                iso3LanguageCode = new Locale(language).getISO3Language();
            } catch (MissingResourceException e) {
                logger.warn("Could not map two-letter BCP-47 language code to three-letter ISO639-3 language code.", e);
            }
        }

        return URI.create(LEXVO_ORG + iso3LanguageCode);
    }
}
