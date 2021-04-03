package no.unit.nva.metadata.extractors;

import no.unit.nva.metadata.type.DcTerms;
import no.unit.nva.model.EntityDescription;
import nva.commons.core.JacocoGenerated;
import nva.commons.core.StringUtils;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.Value;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.util.Locale;
import java.util.MissingResourceException;

public final class LanguageExtractor {
    private static final Logger logger = LoggerFactory.getLogger(LanguageExtractor.class);
    public static final String LEXVO_ORG = "https://lexvo.org/id/iso639-3/";
    public static final String ISO3_LANGUAGE_CODE_UNDEFINED = "und";

    @JacocoGenerated
    private LanguageExtractor() {

    }

    public static void extract(EntityDescription entityDescription, Statement statement) {
        if (isLanguage(statement.getPredicate())) {
            addLanguage(entityDescription, statement);
        }
    }

    private static void addLanguage(EntityDescription entityDescription, Statement statement) {
        Value object = statement.getObject();
        if (ExtractorUtil.isNotLiteral(object)) {
            return;
        }
        entityDescription.setLanguage(toLexvoUri(object.stringValue()));
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

    private static boolean isLanguage(IRI candidate) {
        return DcTerms.LANGUAGE.getIri().equals(candidate);
    }
}
