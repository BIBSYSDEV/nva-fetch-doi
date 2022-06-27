package no.unit.nva.doi.transformer.utils;

import static java.util.Objects.isNull;
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
import java.util.regex.Pattern;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import no.sikt.nva.doi.fetch.jsonconfig.Json;
import nva.commons.core.Environment;
import nva.commons.core.JacocoGenerated;
import nva.commons.core.paths.UriWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CristinProxyClient {

    private static final Logger logger = LoggerFactory.getLogger(CristinProxyClient.class);

    protected static final String API_HOST_ENV_KEY = "API_HOST";
    private static final String CRISTIN = "cristin";
    private static final String PERSON = "person";
    private static final Pattern ORCID_PATTERN = Pattern.compile("[0-9]{4}-[0-9]{4}-[0-9]{4}-[0-9]{3}[0-9|Xx]");
    private static final String ERROR_INVALID_ORCID = "Supplied ORCID is not valid";
    private static final int TIMEOUT_DURATION = 30;
    private static final String LOGGING_MESSAGE_FETCH_FAILED = "Upstream returned error with status {} when calling {}";
    private static final String IDENTIFIER_JSON_POINTER = "/id";

    private final HttpClient httpClient;
    private final String apiHost;

    @JacocoGenerated
    public CristinProxyClient() {
        this(HttpClient.newHttpClient(), new Environment());
    }

    public CristinProxyClient(HttpClient httpClient, Environment environment) {
        this.httpClient = httpClient;
        this.apiHost = environment.readEnv(API_HOST_ENV_KEY);
    }

    /**
     * Get an (optional) Cristin proxy person identifier from an orcid.
     *
     * @param orcid given orcid from metadata
     * @return a URI with person identifier from Cristin proxy for the given orcid
     */
    public Optional<URI> lookupIdentifierFromOrcid(String orcid) {
        var strippedOrcid = stripAndValidateOrcid(orcid);
        var proxyUrl = createUrlToCristinProxy(strippedOrcid);
        var request = createRequest(proxyUrl);
        var response = fetchFromUpstream(request);
        return response.flatMap(this::extractIdentifierFromResponse);
    }

    private String stripAndValidateOrcid(String orcid) {
        var strippedOrcid = attempt(() -> UriWrapper.fromUri(orcid).getLastPathElement()).orElse(fail -> orcid);
        if (isNull(strippedOrcid) || !ORCID_PATTERN.matcher(strippedOrcid).matches()) {
            throw new IllegalArgumentException(ERROR_INVALID_ORCID);
        }
        return strippedOrcid;
    }

    protected URI createUrlToCristinProxy(String strippedOrcid) {
        return UriWrapper.fromHost(apiHost).addChild(CRISTIN).addChild(PERSON).addChild(strippedOrcid).getUri();
    }

    private HttpRequest createRequest(URI proxyUri) {
        return HttpRequest.newBuilder(proxyUri)
                   .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON)
                   .timeout(Duration.ofSeconds(TIMEOUT_DURATION))
                   .GET()
                   .build();
    }

    protected Optional<String> fetchFromUpstream(HttpRequest request) {
        var response = attempt(() -> httpClient.sendAsync(request, BodyHandlers.ofString()).get())
                           .orElseThrow();
        if (responseIsSuccessful(response)) {
            return Optional.ofNullable(response.body());
        } else {
            logger.info(LOGGING_MESSAGE_FETCH_FAILED, response.statusCode(), request.uri().toString());
            return Optional.empty();
        }
    }

    private boolean responseIsSuccessful(HttpResponse<String> response) {
        var statusCode = response.statusCode();
        return statusCode >= HttpURLConnection.HTTP_OK && statusCode < HttpURLConnection.HTTP_MULT_CHOICE;
    }

    protected Optional<URI> extractIdentifierFromResponse(String response) {
        return attempt(() -> Json.readTree(response)).toOptional()
                   .map(node -> node.at(IDENTIFIER_JSON_POINTER))
                   .map(JsonNode::textValue)
                   .map(URI::create);
    }

}
