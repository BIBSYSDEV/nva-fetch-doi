package no.unit.nva.doi.fetch.service;

import static org.apache.http.HttpHeaders.AUTHORIZATION;
import static org.apache.http.HttpHeaders.CONTENT_TYPE;
import static org.apache.http.entity.ContentType.APPLICATION_JSON;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import no.unit.nva.PublicationMapper;
import no.unit.nva.api.CreatePublicationRequest;
import no.unit.nva.api.PublicationResponse;
import no.unit.nva.doi.fetch.exceptions.InsertPublicationException;
import no.unit.nva.model.Publication;
import nva.commons.utils.JacocoGenerated;
import nva.commons.utils.JsonUtils;

public class PublicationPersistenceService extends RestClient {

    public static final String PATH = "publication";
    public static final String WARNING_MESSAGE = "Inserting publication failed.";
    public static final String INSERTING_PUBLICATION_FAILED = WARNING_MESSAGE + "\nAPI-URL:%s\nRequestBody:%s\n";
    private final HttpClient client;
    private final ObjectMapper objectMapper = JsonUtils.objectMapper;

    public PublicationPersistenceService(HttpClient client) {
        super();
        this.client = client;
    }

    @JacocoGenerated
    public PublicationPersistenceService() {
        this(HttpClient.newBuilder().build());
    }

    /**
     * Insert Publication in Database.
     *
     * @param publication   publication
     * @param apiUrl        apiUrl
     * @param authorization authorization
     * @throws IOException                when json parsing fails
     * @throws InterruptedException       When HttpClient throws it.
     * @throws InsertPublicationException When publication api service responds with failure.
     * @throws URISyntaxException         when the input URL is invalid.
     */
    public PublicationResponse insertPublication(Publication publication, URI apiUrl, String authorization)
        throws IOException, InterruptedException, InsertPublicationException, URISyntaxException {
        CreatePublicationRequest requestBody =
            PublicationMapper.convertValue(publication, CreatePublicationRequest.class);
        String requestBodyString = objectMapper.writeValueAsString(requestBody);

        HttpRequest request = HttpRequest.newBuilder()
            .uri(createURI(apiUrl, PATH))
            .header(AUTHORIZATION, authorization)
            .header(CONTENT_TYPE, APPLICATION_JSON.getMimeType())
            .POST(BodyPublishers.ofString(requestBodyString))
            .build();

        HttpResponse<String> response = client.send(request, BodyHandlers.ofString());

        if (!responseIsSuccessful(response)) {
            throw new InsertPublicationException(insertionErrorMessage(apiUrl, requestBodyString));
        }

        return objectMapper.readValue(response.body(), PublicationResponse.class);
    }

    private String insertionErrorMessage(URI apiUrl, String requestBodyString) {
        return String.format(INSERTING_PUBLICATION_FAILED, apiUrl, requestBodyString);
    }
}


