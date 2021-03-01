package no.unit.nva.metadata;

import com.fasterxml.jackson.core.JsonProcessingException;
import no.unit.nva.api.CreatePublicationRequest;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Pattern;

import static nva.commons.core.JsonUtils.objectMapper;

public class MetadataConverter {

    public static final String DOI_PREFIX = "doi:";
    public static final String DOI_ORG = "https://doi.org/";
    public static final String ANY_23 = "http://vocab.sindice.net/any23#";
    public static final String DC_IDENTIFIER = "dc.identifier";
    public static final String DC_IDENTIFIER_UPPER_CASE = "DC.identifier";
    public static final String DOI_START = "10\\.[0-9]+\\/.*";
    public final Pattern doiStartPattern;

    private static final Logger logger = LoggerFactory.getLogger(MetadataConverter.class);
    private static final ValueFactory valueFactory = SimpleValueFactory.getInstance();

    private final Model metadata;
    private final String jsonld;

    public MetadataConverter(Model metadata, String jsonld) {
        this.metadata = metadata;
        this.jsonld = jsonld;
        this.doiStartPattern = Pattern.compile(DOI_START);
    }

    public CreatePublicationRequest toRequest() throws JsonProcessingException {
        CreatePublicationRequest request = objectMapper.readValue(jsonld, CreatePublicationRequest.class);
        addAdditionalFieldsFromMetadata(request, metadata);
        return request;
    }

    private void addAdditionalFieldsFromMetadata(CreatePublicationRequest request, Model metadata) {
        addDoi(request, metadata);
    }

    private void addDoi(CreatePublicationRequest request, Model metadata) {
        Optional<URI> doi = getDoiFromMetadata(metadata);
        if (doi.isPresent()) {
            request.addReferenceDoi(doi.get());
        }
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

    private boolean isDcIdentifier(Statement statement) {
        return List.of(
                valueFactory.createIRI(ANY_23, DC_IDENTIFIER),
                valueFactory.createIRI(ANY_23, DC_IDENTIFIER_UPPER_CASE))
                .contains(statement.getPredicate());
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
