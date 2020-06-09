package no.unit.nva.doi.fetch;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import java.net.URISyntaxException;
import no.unit.nva.doi.fetch.model.DoiProxyResponse;
import no.unit.nva.doi.fetch.service.DoiTransformService;
import no.unit.nva.doi.transformer.PublicationTransformer;
import no.unit.nva.doi.transformer.exception.MissingClaimException;
import no.unit.nva.model.Publication;
import no.unit.nva.model.exceptions.InvalidIssnException;
import no.unit.nva.model.exceptions.InvalidPageTypeException;

public class LocalDoiTransformService extends DoiTransformService {

    private final PublicationTransformer publicationTransformer;

    public LocalDoiTransformService(PublicationTransformer publicationTransformer) {
        super();
        this.publicationTransformer = publicationTransformer;
    }

    /**
     * Method for transforming a publication inside another lambda, without calling the lambda handler.
     *
     * @param response a {@link DoiProxyResponse}
     * @param event    a ApiGateway lambda event
     * @return a {@link Publication}
     * @throws JsonProcessingException when {@link PublicationTransformer} throws exception
     * @throws MissingClaimException    when {@link PublicationTransformer} throws exception
     * @throws URISyntaxException    when {@link PublicationTransformer} throws exception
     * @throws InvalidIssnException when {@link PublicationTransformer} throws exception
     * @throws InvalidPageTypeException when {@link PublicationTransformer} throws exception
     */
    public Publication transformLocally(DoiProxyResponse response, JsonNode event)
        throws JsonProcessingException, MissingClaimException, URISyntaxException, InvalidIssnException,
               InvalidPageTypeException {
        String body = objectMapper.writeValueAsString(response.getJsonNode());
        return publicationTransformer
            .transformPublication(event, body, response.getMetadataSource());
    }

}
