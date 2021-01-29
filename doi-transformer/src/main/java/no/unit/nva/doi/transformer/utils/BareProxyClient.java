package no.unit.nva.doi.transformer.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import nva.commons.utils.JacocoGenerated;
import nva.commons.utils.JsonUtils;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpStatus;
import org.apache.http.client.utils.URIBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.BadRequestException;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.core.MediaType;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.ExecutionException;

public class BareProxyClient {

    public static final String AUTHORITY_ID_JSON_POINTER = "/0/id";
    public static final String PERSON = "person";
    public static final String ORCID = "orcid";

    public static final int TIMEOUT_DURATION = 30;
    public static final String COULD_NOT_FIND_ENTRY_WITH_DOI = "Could not find authority entry with DOI:";
    public static final String UNKNOWN_ERROR_MESSAGE = "Something went wrong. StatusCode:";
    public static final String FETCH_ERROR = "BareProxyClient failed while trying to fetch:";
    private static final String ORCID_EXAMPLE = "https://orcid.org/0000-0003-4902-0240";
    public static final String ILLEGAL_ORCID_MESSAGE = "Illegal Orcid:%s. Valid examples:" + ORCID_EXAMPLE;
    private static final ObjectMapper mapper = JsonUtils.objectMapper;
    private static final Logger logger = LoggerFactory.getLogger(BareProxyClient.class);
    private final transient HttpClient httpClient;

    @JacocoGenerated
    public BareProxyClient() {
        this(HttpClient.newHttpClient());
    }

    public BareProxyClient(HttpClient httpClient) {
        this.httpClient = httpClient;
    }

    /**
     * Get en (optional) arp-identifier for an orcid.
     *
     * @param orcid given orcid from metadata
     * @return a string with arpid for the given orcid
     * @throws IllegalArgumentException when there is something wrong with the orcid
     * @throws URISyntaxException       error creating URI to access BareProxyService
     */
    public Optional<String> lookupArpidForOrcid(URI apiUrl, String orcid) throws URISyntaxException {
        try {
            Optional<String> authorityDataForOrcid = fetchAuthorityDataForOrcid(apiUrl, orcid);
            if (authorityDataForOrcid.isPresent()) {
                return extractArpid(authorityDataForOrcid);
            }
        } catch (JsonProcessingException e) {
            logger.warn(e.getMessage());
        }
        return Optional.empty();
    }

    private Optional<String> extractArpid(Optional<String> authorityDataForOrcid) throws JsonProcessingException {
        JsonNode node = mapper.readTree(authorityDataForOrcid.get());
        return Optional.of(node.at(AUTHORITY_ID_JSON_POINTER).asText());
    }

    private Optional<String> fetchAuthorityDataForOrcid(URI apiUrl, String orcid) throws URISyntaxException {
        URI targetUri = createUrlToBareProxy(apiUrl, orcid);
        return fetchJson(targetUri);
    }

    private Optional<String> fetchJson(URI bareProxyUri) {
        HttpRequest request = createRequest(bareProxyUri);
        try {
            return Optional.ofNullable(getFromWeb(request));
        } catch (InterruptedException
                | ExecutionException
                | NotFoundException
                | BadRequestException e) {
            String details = FETCH_ERROR + bareProxyUri;
            logger.warn(details);
            logger.warn(e.getMessage());
            return Optional.empty();
        }
    }

    private HttpRequest createRequest(URI doiUri) {
        return HttpRequest.newBuilder(doiUri)
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON)
                .timeout(Duration.ofSeconds(TIMEOUT_DURATION))
                .GET()
                .build();
    }

    private String getFromWeb(HttpRequest request) throws InterruptedException, ExecutionException {
        HttpResponse<String> response = httpClient.sendAsync(request, BodyHandlers.ofString()).get();
        if (responseIsSuccessful(response)) {
            return response.body();
        } else {
            return handleError(request, response);
        }
    }

    private String handleError(HttpRequest request, HttpResponse<String> response) {
        if (response.statusCode() == HttpStatus.SC_NOT_FOUND) {
            throw new NotFoundException(COULD_NOT_FIND_ENTRY_WITH_DOI + request.uri().toString());
        }
        throw new BadRequestException(UNKNOWN_ERROR_MESSAGE + response.statusCode());
    }

    private boolean responseIsSuccessful(HttpResponse<String> response) {
        int statusCode = response.statusCode();
        return statusCode >= HttpStatus.SC_OK && statusCode < HttpStatus.SC_MULTIPLE_CHOICES;
    }

    protected URI createUrlToBareProxy(URI apiUrl, String orcid) throws URISyntaxException {
        try {
            String strippetOrcid = new URI(orcid).toString().substring(orcid.lastIndexOf('/') + 1);
            return new URIBuilder(apiUrl)
                    .setPathSegments(PERSON)
                    .setParameter(ORCID, strippetOrcid)
                    .build();
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException(ILLEGAL_ORCID_MESSAGE);
        }
    }
}
