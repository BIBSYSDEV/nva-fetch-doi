package no.unit.nva.doi.transformer;

import static nva.commons.utils.JsonUtils.objectMapper;

import com.fasterxml.jackson.core.JsonProcessingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.time.Instant;
import java.util.UUID;
import no.unit.nva.doi.transformer.model.crossrefmodel.CrossRefDocument;
import no.unit.nva.doi.transformer.model.crossrefmodel.CrossrefApiResponse;
import no.unit.nva.doi.transformer.model.datacitemodel.DataciteResponse;
import no.unit.nva.model.Publication;
import no.unit.nva.model.exceptions.InvalidIssnException;
import no.unit.nva.model.util.OrgNumberMapper;

public class DoiTransformService {

    private final DataciteResponseConverter dataciteConverter;
    private final CrossRefConverter crossRefConverter;


    public DoiTransformService() {
        this(new DataciteResponseConverter(),new CrossRefConverter());
    }

    /**
     * Constructor with parameters.
     *
     * @param dataciteConverter dataciteConverter.
     * @param crossRefConverter crossrefConverter.
     *
     */
    public DoiTransformService(DataciteResponseConverter dataciteConverter, CrossRefConverter crossRefConverter) {

        this.dataciteConverter = dataciteConverter;
        this.crossRefConverter = crossRefConverter;
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
     * @throws URISyntaxException       when the input contains invalid URIs
     * @throws InvalidIssnException     thrown if a provided ISSN is invalid.
     *                                  the publication instance type.
     */
    public Publication transformPublication(String body, String contentLocation, String owner, String orgNumber)
            throws JsonProcessingException, URISyntaxException, InvalidIssnException {
        UUID uuid = UUID.randomUUID();
        URI publisherID = toPublisherId(orgNumber);
        Instant now = Instant.now();
        return convertInputToPublication(body, contentLocation, now, owner, uuid, publisherID);
    }

    protected Publication convertInputToPublication(String body, String contentLocation, Instant now, String owner,
                                                    UUID identifier, URI publisher)
            throws JsonProcessingException, URISyntaxException, InvalidIssnException {

        MetadataLocation metadataLocation = MetadataLocation.lookup(contentLocation);
        if (metadataLocation.equals(MetadataLocation.CROSSREF)) {
            return convertFromCrossRef(body, now, owner, identifier, publisher);
        } else {
            return convertFromDatacite(body, now, owner, identifier, publisher);
        }
    }

    private Publication convertFromDatacite(String body, Instant now, String owner, UUID uuid, URI publisherId)
            throws JsonProcessingException, URISyntaxException, InvalidIssnException {
        DataciteResponse dataciteResponse = objectMapper.readValue(body, DataciteResponse.class);
        return dataciteConverter.toPublication(dataciteResponse, now, uuid, owner, publisherId);
    }

    private Publication convertFromCrossRef(String body, Instant now, String owner, UUID identifier, URI publisherId)
            throws JsonProcessingException, InvalidIssnException {

        CrossRefDocument document = objectMapper.readValue(body, CrossrefApiResponse.class).getMessage();
        return crossRefConverter.toPublication(document, now, owner, identifier, publisherId);
    }

    private URI toPublisherId(String orgNumber) {
        return OrgNumberMapper.toCristinId(orgNumber);
    }
}
