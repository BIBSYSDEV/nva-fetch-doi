package no.unit.nva.doi.fetch.service;

import static no.unit.nva.doi.fetch.MainHandler.jsonParser;
import static org.apache.http.HttpHeaders.ACCEPT;
import static org.apache.http.HttpHeaders.AUTHORIZATION;
import static org.apache.http.HttpHeaders.CONTENT_LOCATION;
import static org.apache.http.entity.ContentType.APPLICATION_JSON;

import com.fasterxml.jackson.databind.JsonNode;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import no.unit.nva.doi.fetch.exceptions.TransformFailedException;

public class DoiTransformService extends RestClient {

    public static final String PATH = "/doi-transform";
    public static final String TRASFORMATION_ERROR_MESSAGE = "Transform failed. ApiUrl:%s\n Path:%s\n RequestBody:%s";
    private final HttpClient client;

    protected DoiTransformService(HttpClient client) {
        super();
        this.client = client;
    }

    public DoiTransformService() {
        this(HttpClient.newBuilder().build());
    }

    /**
     * Transform Datacite data to NVA data.
     *
     * @param doiProxyResponse doiProxyResponse
     * @param apiUrl           apiUrl
     * @param authorization    authorization
     * @return jsonNode
     */
    public JsonNode transform(DoiProxyResponse doiProxyResponse, String apiUrl, String authorization)
        throws IOException, URISyntaxException, InterruptedException, TransformFailedException {
        String requestBody = jsonParser.writeValueAsString(doiProxyResponse.getJsonNode());
        HttpRequest request = HttpRequest.newBuilder()
                                         .uri(buildUriWithPath(apiUrl, PATH))
                                         .header(ACCEPT, APPLICATION_JSON.getMimeType())
                                         .header(AUTHORIZATION, authorization)
                                         .header(CONTENT_LOCATION, doiProxyResponse.getMetadataSource())
                                         .POST(BodyPublishers.ofString(requestBody))
                                         .build();
        HttpResponse<String> response = client.send(request, BodyHandlers.ofString());
        if (responseIsSuccessful(response)) {
            return jsonParser.readTree(response.body());
        } else {
            throw new TransformFailedException(getTrasformationErrorMessage(apiUrl, requestBody));
        }
    }

    private String getTrasformationErrorMessage(String apiUrl, String requestBody) {
        return String.format(TRASFORMATION_ERROR_MESSAGE, apiUrl, PATH, requestBody);
    }
}
