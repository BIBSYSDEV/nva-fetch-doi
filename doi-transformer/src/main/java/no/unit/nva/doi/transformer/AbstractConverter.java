package no.unit.nva.doi.transformer;

import java.net.URI;
import java.util.stream.Stream;
import no.unit.nva.doi.transformer.language.LanguageDetector;
import no.unit.nva.doi.transformer.utils.TextLang;
import no.unit.nva.model.Organization;
import no.unit.nva.model.PublicationDate;
import no.unit.nva.model.PublicationStatus;
import nva.commons.utils.doi.DoiConverter;

public class AbstractConverter {

    public static final PublicationStatus DEFAULT_NEW_PUBLICATION_STATUS = PublicationStatus.NEW;
    public static final String FAMILY_NAME_GIVEN_NAME_SEPARATOR = ", ";

    protected DoiConverter doiConverter;
    protected LanguageDetector languageDetector;

    public AbstractConverter(LanguageDetector detector, DoiConverter doiConverter) {
        this.languageDetector = detector;
        this.doiConverter = doiConverter;
    }

    protected String toName(String familyName, String givenName) {
        return String.join(FAMILY_NAME_GIVEN_NAME_SEPARATOR, familyName, givenName);
    }

    protected PublicationDate toDate(Integer publicationYear) {
        return new PublicationDate.Builder()
            .withYear(publicationYear.toString())
            .build();
    }

    protected String getMainTitle(Stream<String> titles) {
        return titles.findFirst().orElse(null);
    }

    protected Organization toPublisher(URI publisherId) {
        return new Organization.Builder()
            .withId(publisherId)
            .build();
    }

    protected TextLang detectLanguage(String title) {
        return new TextLang(title, languageDetector.detectLangWithDefault(title));
    }
}
