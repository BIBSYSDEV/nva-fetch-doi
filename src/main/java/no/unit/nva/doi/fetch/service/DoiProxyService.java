package no.unit.nva.doi.fetch.service;

import static org.apache.http.HttpHeaders.ACCEPT;
import static org.apache.http.HttpHeaders.AUTHORIZATION;
import static org.apache.http.HttpHeaders.CONTENT_TYPE;
import static org.apache.http.entity.ContentType.APPLICATION_JSON;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.util.Optional;
import no.unit.nva.doi.fetch.MainHandler;
import no.unit.nva.doi.fetch.exceptions.MalformedRequestException;
import no.unit.nva.doi.fetch.exceptions.MetadataNotFoundException;
import no.unit.nva.doi.fetch.exceptions.NoContentLocationFoundException;
import no.unit.nva.doi.fetch.utils.JacocoGenerated;
import org.apache.http.HttpHeaders;

public class DoiProxyService extends RestClient {

    public static final String DATACITE_JSON = "application/vnd.datacite.datacite+json";
    public static final String PATH = "doi";
    public static final String ERROR_READING_METADATA = "Could not get publication metadata.DOI:";
    public static final String REQUEST_BODY_MALFORMED = "Request body is malformed";

    private final HttpClient client;
    private final ObjectMapper jsonParser = MainHandler.createObjectMapper();

    public DoiProxyService(HttpClient client) {
        super();
        this.client = client;
    }

    public DoiProxyService() {
        this(HttpClient.newBuilder().build());
    }

    /**
     * Lookup data from DOI URL.
     *
     * @param doiUrl        doiUrl
     * @param apiUrl        apiUrl
     * @param authorization authorization
     * @return jsonNode
     * @throws NoContentLocationFoundException thrown when the header Content-Location is missing from the response
     * @throws MetadataNotFoundException       when nva-doi-proxy sends a failed response
     * @throws URISyntaxException              when the input URL is not correct
     * @throws IOException                     when IO error happens.
     * @throws InterruptedException            when a thread error happens.
     * @throws MalformedRequestException       when request is malformed
     */
    public DoiProxyResponse lookup(URL doiUrl, String apiUrl, String authorization)
        throws NoContentLocationFoundException, MetadataNotFoundException, URISyntaxException, IOException,
        InterruptedException, MalformedRequestException {
        Request request = validateRequest(new Request(doiUrl));
        String requestBody = jsonParser.writeValueAsString(request);

        HttpRequest post = HttpRequest.newBuilder().uri(createURI(apiUrl, PATH))
                                      .POST(BodyPublishers.ofString(requestBody)).header(ACCEPT, DATACITE_JSON)
                                      .header(AUTHORIZATION, authorization)
                                      .header(CONTENT_TYPE, APPLICATION_JSON.getMimeType()).build();
        HttpResponse<String> response = client.send(post, BodyHandlers.ofString());
        if (responseIsSuccessful(response)) {
            return returnBodyAndContentLocation(response);
        } else {
            throw new MetadataNotFoundException(ERROR_READING_METADATA + doiUrl);
        }
    }

    private Request validateRequest(Request request) throws MalformedRequestException {
        if (request.doi == null) {
            throw new MalformedRequestException(REQUEST_BODY_MALFORMED);
        }
        return request;
    }

    private DoiProxyResponse returnBodyAndContentLocation(HttpResponse<String> response)
        throws NoContentLocationFoundException, JsonProcessingException {
        String contentLocation = exctractContentLocation(response);
        String responseBodyString = response.body();
        JsonNode responseJson = jsonParser.readTree(responseBodyString);
        return new DoiProxyResponse(responseJson, contentLocation);
    }

    private String exctractContentLocation(HttpResponse<String> response) throws NoContentLocationFoundException {
        Optional<String> contentOpt = response.headers().firstValue(HttpHeaders.CONTENT_LOCATION);
        return contentOpt.filter(str -> !str.isEmpty()).orElseThrow(this::contentLocationNotFound);
    }

    private NoContentLocationFoundException contentLocationNotFound() {
        return new NoContentLocationFoundException("ContentLocation header should not be empty");
    }

    public static class Request {

        private URL doi;

        @JacocoGenerated
        public Request() {
        }

        public Request(URL doi) {
            this.doi = doi;
        }

        public URL getDoi() {
            return doi;
        }

        public void setDoi(URL doi) {
            this.doi = doi;
        }
    }
}
