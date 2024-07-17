package no.unit.nva.doi.transformer;

import java.util.stream.Stream;
import no.unit.nva.doi.fetch.commons.publication.model.PublicationDate;
import no.unit.nva.doi.transformer.language.LanguageDetector;
import no.unit.nva.doi.transformer.utils.TextLang;
import nva.commons.doi.DoiConverter;

public class AbstractConverter {

    public static final String PLAIN_NAME_SEPARATOR = " ";

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

    protected TextLang detectLanguage(String title) {
        return new TextLang(title, languageDetector.detectLangWithDefault(title));
    }
}
