package no.unit.nva.doi.fetch.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import no.unit.nva.doi.fetch.MainHandler;
import no.unit.nva.doi.fetch.exceptions.TransformFailedException;
import no.unit.nva.doi.transformer.PublicationTransformer;
import no.unit.nva.doi.transformer.excpetions.MisingClaimException;
import no.unit.nva.model.Publication;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;

import static no.unit.nva.doi.fetch.MainHandler.jsonParser;
import static org.apache.http.HttpHeaders.ACCEPT;
import static org.apache.http.HttpHeaders.AUTHORIZATION;
import static org.apache.http.HttpHeaders.CONTENT_LOCATION;
import static org.apache.http.HttpHeaders.CONTENT_TYPE;
import static org.apache.http.entity.ContentType.APPLICATION_JSON;

public class DoiTransformService extends RestClient {

    public static final String PATH = "doi-transform";
    public static final String WARNING_MESSAGE = "Transform failed.";
    public static final String TRANSFORMATION_ERROR_MESSAGE =
        WARNING_MESSAGE + "\nApiUrl:%s\n Path:%s\n RequestBody:%s";
    private final HttpClient client;
    private final PublicationTransformer publicationTransformer;
    private final ObjectMapper objectMapper;

    /**
     * Transform sevice with custom client.
     *
     * @param client an {@link HttpClient}
     */
    public DoiTransformService(HttpClient client) {
        super();
        this.client = client;
        this.publicationTransformer = new PublicationTransformer();
        this.objectMapper = MainHandler.createObjectMapper();
    }

    /**
     * Default constructor.
     */
    public DoiTransformService() {
        this(HttpClient.newBuilder().build());
    }

    /**
     * Method for transforming a publication inside another lambda, without calling the lambda handler.
     *
     * @param response a {@link DoiProxyResponse}
     * @param event    a ApiGateway lambda event
     * @return a {@link Publication}
     * @throws JsonProcessingException when {@link PublicationTransformer} throws exception
     * @throws MisingClaimException    when {@link PublicationTransformer} throws exception
     * @throws URISyntaxException    when {@link PublicationTransformer} throws exception
     */
    public Publication transformLocally(DoiProxyResponse response, JsonNode event)
        throws JsonProcessingException, MisingClaimException, URISyntaxException {
        String body = objectMapper.writeValueAsString(response.getJsonNode());
        Publication publication = publicationTransformer
            .transformPublication(event, body, response.getMetadataSource());
        return publication;
    }

    /**
     * Transform Datacite data to NVA data.
     *
     * @param doiProxyResponse doiProxyResponse
     * @param apiUrl           apiUrl
     * @param authorization    authorization
     * @return jsonNode
     * @throws IOException              when HttpClient throws IoException.
     * @throws URISyntaxException       when the URI is not correct.
     * @throws InterruptedException     when HttpClient throws InterruptedException.
     * @throws TransformFailedException when the nva-doi-transform service returns a failed message.
     */
    public JsonNode transform(DoiProxyResponse doiProxyResponse, String apiUrl, String authorization)
        throws IOException, URISyntaxException, InterruptedException, TransformFailedException {
        String requestBody = jsonParser.writeValueAsString(doiProxyResponse.getJsonNode());
        HttpRequest request = HttpRequest.newBuilder().uri(createURI(apiUrl, PATH))
                                         .header(ACCEPT, APPLICATION_JSON.getMimeType())
                                         .header(AUTHORIZATION, authorization)
                                         .header(CONTENT_TYPE, APPLICATION_JSON.getMimeType())
                                         .header(CONTENT_LOCATION, doiProxyResponse.getMetadataSource())
                                         .POST(BodyPublishers.ofString(requestBody)).build();

        HttpResponse<String> response = client.send(request, BodyHandlers.ofString());
        if (responseIsSuccessful(response)) {
            return jsonParser.readTree(response.body());
        } else {
            throw new TransformFailedException(getTransformationErrorMessage(apiUrl, requestBody));
        }
    }

    private String getTransformationErrorMessage(String apiUrl, String requestBody) {
        return String.format(TRANSFORMATION_ERROR_MESSAGE, apiUrl, PATH, requestBody);
    }
}
