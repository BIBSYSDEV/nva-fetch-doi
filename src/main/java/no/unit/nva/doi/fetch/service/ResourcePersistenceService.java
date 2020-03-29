package no.unit.nva.doi.fetch.service;

import static org.apache.http.HttpHeaders.AUTHORIZATION;
import static org.apache.http.HttpHeaders.CONTENT_TYPE;
import static org.apache.http.entity.ContentType.APPLICATION_JSON;

import com.fasterxml.jackson.databind.JsonNode;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import no.unit.nva.doi.fetch.MainHandler;
import no.unit.nva.doi.fetch.exceptions.InsertPublicationException;

public class ResourcePersistenceService extends RestClient {

    public static final String PATH = "/resource";
    public static final String WARNING_MESSAGE = "Inserting publication failed.";
    public static final String INSERTING_PUBLICATION_FAILED = WARNING_MESSAGE + "\nAPI-URL:%s\nRequestBody:%s\n";
    private final HttpClient client;

    public ResourcePersistenceService(HttpClient client) {
        super();
        this.client = client;
    }

    public ResourcePersistenceService() {
        this(HttpClient.newBuilder().build());
    }

    /**
     * Insert Resource in Database.
     *
     * @param publication   dataciteData
     * @param apiUrl        apiUrl
     * @param authorization authorization
     * @throws IOException when json parsing failes
     * @throws InterruptedException When HttpClient throws it.
     * @throws InsertPublicationException When publication-service responsds with failure.
     * @throws URISyntaxException when the input URL is invalid.
     */
    public void insertPublication(JsonNode publication, String apiUrl, String authorization)
        throws IOException, InterruptedException, InsertPublicationException, URISyntaxException {
        String requestBodyString = MainHandler.jsonParser.writeValueAsString(publication);
        HttpRequest request = HttpRequest.newBuilder().POST(BodyPublishers.ofString(requestBodyString))
                                         .uri(createURI(apiUrl, PATH)).header(AUTHORIZATION, authorization)
                                         .header(CONTENT_TYPE, APPLICATION_JSON.getMimeType()).build();
        HttpResponse<String> response = client.send(request, BodyHandlers.ofString());

        if (!responseIsSuccessful(response)) {
            throw new InsertPublicationException(insertionErrorMessage(apiUrl, requestBodyString));
        }
    }

    private String insertionErrorMessage(String apiUrl, String requestBodyString) {
        return String.format(INSERTING_PUBLICATION_FAILED, apiUrl, requestBodyString);
    }
}


