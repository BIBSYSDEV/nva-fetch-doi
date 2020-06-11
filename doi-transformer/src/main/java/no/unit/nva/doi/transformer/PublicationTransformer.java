package no.unit.nva.doi.transformer;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.net.URISyntaxException;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import no.unit.nva.doi.fetch.ObjectMapperConfig;
import no.unit.nva.doi.transformer.exception.MissingClaimException;
import no.unit.nva.doi.transformer.model.crossrefmodel.CrossRefDocument;
import no.unit.nva.doi.transformer.model.crossrefmodel.CrossrefApiResponse;
import no.unit.nva.doi.transformer.model.internal.external.DataciteResponse;
import no.unit.nva.model.Publication;
import no.unit.nva.model.exceptions.InvalidIssnException;
import no.unit.nva.model.exceptions.InvalidPageTypeException;
import no.unit.nva.model.util.OrgNumberMapper;

public class PublicationTransformer {

    private final DataciteResponseConverter dataciteConverter;
    private final CrossRefConverter crossRefConverter;
    private final ObjectMapper objectMapper;

    public PublicationTransformer() {
        this(new DataciteResponseConverter(),
                new CrossRefConverter(), ObjectMapperConfig.createObjectMapper());
    }

    /**
     * Constructor with parameters.
     *
     * @param dataciteConverter dataciteConverter.
     * @param crossRefConverter crossrefConverter.
     * @param objectMapper      jsonParser.
     */
    public PublicationTransformer(DataciteResponseConverter dataciteConverter, CrossRefConverter crossRefConverter,
                                  ObjectMapper objectMapper) {

        this.dataciteConverter = dataciteConverter;
        this.crossRefConverter = crossRefConverter;
        this.objectMapper = objectMapper;
    }

    /**
     * Transforms publication.
     *
     * @param body            the request body as extracted from the event.
     * @param contentLocation crossref or datacite.
     * @param owner           the owner.
     * @param orgNumber       the orgNumber.
     * @return a Publication.
     * @throws JsonProcessingException  when cannot process json.
     * @throws MissingClaimException     when request does not have the required claims.
     * @throws URISyntaxException       when the input contains invalid URIs
     * @throws InvalidIssnException     thrown if a provided ISSN is invalid.
     * @throws InvalidPageTypeException thrown if the provided page type is incompatible with
     *                                  the publication instance type.
     */
    public Publication transformPublication(String body, String contentLocation, String owner, String orgNumber)
            throws JsonProcessingException, MissingClaimException, URISyntaxException, InvalidIssnException,
                   InvalidPageTypeException {
        UUID uuid = UUID.randomUUID();
        URI publisherID = toPublisherId(orgNumber);
        Instant now = Instant.now();
        return convertInputToPublication(body, contentLocation, now, owner, uuid, publisherID);
    }

    protected Publication convertInputToPublication(String body, String contentLocation, Instant now, String owner,
                                                    UUID identifier, URI publisher)
            throws JsonProcessingException, URISyntaxException, InvalidIssnException, InvalidPageTypeException {

        MetadataLocation metadataLocation = MetadataLocation.lookup(contentLocation);
        if (metadataLocation.equals(MetadataLocation.CROSSREF)) {
            return convertFromCrossRef(body, now, owner, identifier, publisher);
        } else {
            return convertFromDatacite(body, now, owner, identifier, publisher);
        }
    }

    private Publication convertFromDatacite(String body, Instant now, String owner, UUID uuid, URI publisherId)
            throws JsonProcessingException, URISyntaxException, InvalidPageTypeException, InvalidIssnException {
        DataciteResponse dataciteResponse = objectMapper.readValue(body, DataciteResponse.class);
        return dataciteConverter.toPublication(dataciteResponse, now, uuid, owner, publisherId);
    }

    private Publication convertFromCrossRef(String body, Instant now, String owner, UUID identifier, URI publisherId)
            throws JsonProcessingException, InvalidIssnException, InvalidPageTypeException {

        CrossRefDocument document = objectMapper.readValue(body, CrossrefApiResponse.class).getMessage();
        return crossRefConverter.toPublication(document, now, owner, identifier, publisherId);
    }

    private URI toPublisherId(String orgNumber) {
        return OrgNumberMapper.toCristinId(orgNumber);
    }
}
