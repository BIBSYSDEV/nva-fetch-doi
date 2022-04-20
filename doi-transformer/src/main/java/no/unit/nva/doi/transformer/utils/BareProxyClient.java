package no.unit.nva.doi.transformer.utils;

import static nva.commons.core.attempt.Try.attempt;
import com.fasterxml.jackson.databind.JsonNode;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import no.sikt.nva.doi.fetch.jsonconfig.Json;
import nva.commons.core.Environment;
import nva.commons.core.JacocoGenerated;
import nva.commons.core.paths.UriWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BareProxyClient {

    public static final String AUTHORITY_ID_JSON_POINTER = "/0/id";
    public static final String PERSON = "person";
    public static final String ORCID = "orcid";
    public static final int TIMEOUT_DURATION = 30;
    public static final String COULD_NOT_FIND_ENTRY_WITH_DOI = "Could not find authority entry with DOI:";
    public static final String UNKNOWN_ERROR_MESSAGE = "Something went wrong. StatusCode:";
    public static final String FETCH_ERROR = "BareProxyClient failed while trying to fetch:";
    public static final String BARE_PROXY_API_URI_ENV_KEY = "BARE_PROXY_API_URI";
    public static final int WANT_JUST_ONE_HIT = 1;

    private final transient HttpClient httpClient;
    private final URI apiUrl;
    private static final Logger logger = LoggerFactory.getLogger(BareProxyClient.class);

    @JacocoGenerated
    public BareProxyClient() {
        this(HttpClient.newHttpClient(), new Environment());
    }

    public BareProxyClient(HttpClient httpClient, Environment environment) {
        this.httpClient = httpClient;
        this.apiUrl = URI.create(environment.readEnv(BARE_PROXY_API_URI_ENV_KEY));
    }

    /**
     * Get en (optional) arp-identifier for an orcid.
     *
     * @param orcid given orcid from metadata
     * @return a string with arpid for the given orcid
     */
    public Optional<String> lookupArpidForOrcid(String orcid) {
        return fetchAuthorityDataForOrcid(apiUrl, orcid).flatMap(this::extractArpid);
    }

    private Optional<String> extractArpid(String authorityDataForOrcid) {
        return attempt(() -> Json.readTree(authorityDataForOrcid))
            .toOptional(fail -> logger.warn(fail.getException().getMessage()))
            .filter(this::resultIsArrayWithExactlyOneItem)
            .map(node -> node.at(AUTHORITY_ID_JSON_POINTER))
            .map(JsonNode::textValue);
    }

    private boolean resultIsArrayWithExactlyOneItem(JsonNode node) {
        return node.isArray() && node.size() == WANT_JUST_ONE_HIT;
    }

    private Optional<String> fetchAuthorityDataForOrcid(URI apiUrl, String orcid) {
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
        if (response.statusCode() == HttpURLConnection.HTTP_NOT_FOUND) {
            throw new NotFoundException(COULD_NOT_FIND_ENTRY_WITH_DOI + request.uri().toString());
        }
        throw new BadRequestException(UNKNOWN_ERROR_MESSAGE + response.statusCode());
    }

    private boolean responseIsSuccessful(HttpResponse<String> response) {
        int statusCode = response.statusCode();
        return statusCode >= HttpURLConnection.HTTP_OK && statusCode < HttpURLConnection.HTTP_MULT_CHOICE;
    }

    protected URI createUrlToBareProxy(URI apiUrl, String orcid) {
        String strippedOrcid = UriWrapper.fromUri(orcid).getLastPathElement();
        return UriWrapper.fromUri(apiUrl)
            .addChild(PERSON)
            .addQueryParameter(ORCID, strippedOrcid)
            .getUri();
    }
}
