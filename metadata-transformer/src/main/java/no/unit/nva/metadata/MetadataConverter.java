package no.unit.nva.metadata;

import static nva.commons.core.JsonUtils.objectMapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Pattern;
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

    public static final String DOI_PREFIX = "doi:";
    public static final String DOI_ORG = "https://doi.org/";
    public static final String DCTERMS = "http://purl.org/dc/terms/";
    public static final String IDENTIFIER = "identifier";
    public static final String DOI_START = "10\\.[0-9]+/.*";
    public static final String LANGUAGE = "language";
    public static final String LEXVO_ORG = "https://lexvo.org/id/iso639-3/";
    public static final String ISO3_LANGUAGE_CODE_UNDEFINED = "und";
    private static final Logger logger = LoggerFactory.getLogger(MetadataConverter.class);
    private static final ValueFactory valueFactory = SimpleValueFactory.getInstance();
    public final Pattern doiStartPattern;
    private final Model metadata;
    private final String jsonld;

    public MetadataConverter(Model metadata, String jsonld) {
        this.metadata = metadata;
        this.jsonld = jsonld;
        this.doiStartPattern = Pattern.compile(DOI_START);
    }

    public CreatePublicationRequest toRequest() throws JsonProcessingException {
        CreatePublicationRequest request = objectMapper.readValue(jsonld, CreatePublicationRequest.class);
        getDoiFromMetadata(metadata).ifPresent(request::addReferenceDoi);
        getLanguageFromMetadata(metadata).ifPresent(language -> request.getEntityDescription().setLanguage(language));
        return request;
    }

    private Optional<URI> getDoiFromMetadata(Model metadata) {
        return metadata
            .stream()
            .filter(this::isDcIdentifier)
            .map(Statement::getObject)
            .map(Value::stringValue)
            .map(this::toDoiUri)
            .filter(Objects::nonNull)
            .findFirst();
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

    private boolean isDcIdentifier(Statement statement) {
        return statement.getPredicate().equals(valueFactory.createIRI(DCTERMS, IDENTIFIER));
    }

    private boolean isDcLanguage(Statement statement) {
        return valueFactory.createIRI(DCTERMS, LANGUAGE).equals(statement.getPredicate());
    }

    private URI toDoiUri(String stringValue) {
        URI doi = null;
        try {
            if (stringValue.startsWith(DOI_PREFIX)) {
                doi = createUriReplaceDoiPrefixWithHost(stringValue);
            } else if (stringValue.startsWith(DOI_ORG)) {
                doi = new URI(stringValue);
            } else if (hasDoiPattern(stringValue)) {
                doi = createUriAddHostToDoiPath(stringValue);
            }
        } catch (URISyntaxException e) {
            logger.warn("Can not create URI from DOI string value", e);
        }
        return doi;
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

    private URI createUriAddHostToDoiPath(String stringValue) throws URISyntaxException {
        return new URI(DOI_ORG + stringValue);
    }

    private boolean hasDoiPattern(String stringValue) {
        return doiStartPattern.matcher(stringValue).matches();
    }

    private URI createUriReplaceDoiPrefixWithHost(String stringValue) throws URISyntaxException {
        return new URI(stringValue.replace(DOI_PREFIX, DOI_ORG));
    }
}
