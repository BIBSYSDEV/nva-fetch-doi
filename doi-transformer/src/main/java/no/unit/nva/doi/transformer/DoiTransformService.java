package no.unit.nva.doi.transformer;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URISyntaxException;
import no.unit.nva.doi.fetch.ObjectMapperConfig;
import no.unit.nva.doi.transformer.exception.MissingClaimException;
import no.unit.nva.model.Publication;
import no.unit.nva.model.exceptions.InvalidIssnException;
import no.unit.nva.model.exceptions.InvalidPageTypeException;

public class DoiTransformService {

    private final PublicationTransformer publicationTransformer;
    private final ObjectMapper objectMapper = ObjectMapperConfig.createObjectMapper();

    public DoiTransformService(PublicationTransformer publicationTransformer) {
        this.publicationTransformer = publicationTransformer;
    }

    /**
     * Method for transforming a publication inside another lambda, without calling the lambda handler.
     *
     * @param metadata  metadata json to transform
     * @param metadataSource    source of metadata
     * @param event    a ApiGateway lambda event
     * @return a {@link Publication}
     * @throws JsonProcessingException when {@link PublicationTransformer} throws exception
     * @throws MissingClaimException    when {@link PublicationTransformer} throws exception
     * @throws URISyntaxException    when {@link PublicationTransformer} throws exception
     * @throws InvalidIssnException when {@link PublicationTransformer} throws exception
     * @throws InvalidPageTypeException when {@link PublicationTransformer} throws exception
     */
    public Publication transform(JsonNode metadata, String metadataSource, JsonNode event)
        throws JsonProcessingException, MissingClaimException, URISyntaxException, InvalidIssnException,
               InvalidPageTypeException {
        String body = objectMapper.writeValueAsString(metadata);
        return publicationTransformer
            .transformPublication(event, body, metadataSource);
    }

}
