package no.unit.nva.doi.transformer;

import com.fasterxml.jackson.core.JsonProcessingException;
import no.sikt.nva.doi.fetch.jsonconfig.Json;
import no.unit.nva.doi.fetch.commons.publication.model.CreatePublicationRequest;
import no.unit.nva.doi.transformer.model.crossrefmodel.CrossRefDocument;
import no.unit.nva.doi.transformer.model.crossrefmodel.CrossrefApiResponse;
import no.unit.nva.doi.transformer.model.datacitemodel.DataciteResponse;
import no.unit.nva.doi.transformer.utils.InvalidIssnException;
import nva.commons.core.JacocoGenerated;

public class DoiTransformService {

    private final DataciteResponseConverter dataciteConverter;
    private final CrossRefConverter crossRefConverter;

    @JacocoGenerated
    public DoiTransformService() {
        this(new DataciteResponseConverter(), new CrossRefConverter());
    }

    public DoiTransformService(DataciteResponseConverter dataciteConverter, CrossRefConverter crossRefConverter) {

        this.dataciteConverter = dataciteConverter;
        this.crossRefConverter = crossRefConverter;
    }

    public CreatePublicationRequest transformPublication(String body, String contentLocation)
        throws JsonProcessingException, InvalidIssnException {
        return convertInputToPublication(body, contentLocation);
    }

    protected CreatePublicationRequest convertInputToPublication(String body, String contentLocation)
        throws JsonProcessingException, InvalidIssnException {

        MetadataLocation metadataLocation = MetadataLocation.lookup(contentLocation);
        if (metadataLocation.equals(MetadataLocation.CROSSREF)) {
            return convertFromCrossRef(body);
        } else {
            return convertFromDatacite(body);
        }
    }

    private CreatePublicationRequest convertFromDatacite(String body)
        throws JsonProcessingException, InvalidIssnException {
        DataciteResponse dataciteResponse = Json.readValue(body, DataciteResponse.class);
        return dataciteConverter.toPublication(dataciteResponse);
    }

    private CreatePublicationRequest convertFromCrossRef(String body)
        throws JsonProcessingException {

        CrossRefDocument document = Json.readValue(body, CrossrefApiResponse.class).getMessage();
        return crossRefConverter.toPublication(document);
    }
}
