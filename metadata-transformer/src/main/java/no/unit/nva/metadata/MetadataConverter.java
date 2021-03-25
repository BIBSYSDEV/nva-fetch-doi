package no.unit.nva.metadata;

import static nva.commons.core.JsonUtils.objectMapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import java.net.URI;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.Optional;
import no.unit.nva.api.CreatePublicationRequest;
import nva.commons.core.StringUtils;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MetadataConverter {

    public static final String DCTERMS = "http://purl.org/dc/terms/";
    public static final String LANGUAGE = "language";
    public static final String LEXVO_ORG = "https://lexvo.org/id/iso639-3/";
    public static final String ISO3_LANGUAGE_CODE_UNDEFINED = "und";
    private static final Logger logger = LoggerFactory.getLogger(MetadataConverter.class);
    private static final ValueFactory valueFactory = SimpleValueFactory.getInstance();
    private final Model metadata;
    private final String jsonld;

    public MetadataConverter(Model metadata, String jsonld) {
        this.metadata = metadata;
        this.jsonld = jsonld;
    }

    public CreatePublicationRequest toRequest() throws JsonProcessingException {
        CreatePublicationRequest request = objectMapper.readValue(jsonld, CreatePublicationRequest.class);
        getLanguageFromMetadata(metadata).ifPresent(language -> request.getEntityDescription().setLanguage(language));
        return request;
    }

    private Optional<URI> getLanguageFromMetadata(Model metadata) {
        return metadata
            .stream()
            .filter(this::isDcLanguage)
            .map(Statement::getObject)
            .map(Value::stringValue)
            .map(this::toLexvoUri)
            .findFirst();
    }

    private boolean isDcLanguage(Statement statement) {
        return valueFactory.createIRI(DCTERMS, LANGUAGE).equals(statement.getPredicate());
    }

    private URI toLexvoUri(String language) {
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
