package no.unit.nva.doi;

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
import nva.commons.core.Environment;
import nva.commons.core.JacocoGenerated;
import nva.commons.core.paths.UriWrapper;
import nva.commons.secrets.ErrorReadingSecretException;
import nva.commons.secrets.SecretsReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CrossRefClient {

    public static final String CROSSREF_LINK = "https://api.crossref.org";
    public static final String WORKS = "works";

    public static final int TIMEOUT_DURATION = 30;
    public static final String COULD_NOT_FIND_ENTRY_WITH_DOI = "Could not find entry with DOI:";
    public static final String UNKNOWN_ERROR_MESSAGE = "Something went wrong. StatusCode:";
    public static final String FETCH_ERROR = "CrossRefClient failed while trying to fetch:";
    public static final String CROSSREF_USER_AGENT =
        "nva-fetch-doi/1.0 (https://github.com/BIBSYSDEV/nva-fetch-doi; mailto:support@unit.no)";
    public static final String ADDING_TOKEN_IN_HEADER =
        "CrossRef Api PLUS token is present, adding token in header";
    public static final String CROSSREFPLUSAPITOKEN_NAME_ENV = "CROSSREFPLUSAPITOKEN_NAME";
    public static final String CROSSREFPLUSAPITOKEN_KEY_ENV = "CROSSREFPLUSAPITOKEN_KEY";
    public static final String CROSSREF_API_KEY_SECRET_NOT_FOUND_TEMPLATE =
        "Crossref API token could not be found with name: {} and key: {}";
    public static final String CROSSREF_SECRETS_NOT_FOUND = "Crossref secrets not found";

    private static final String CROSSREF_PLUSAPI_HEADER = "Crossref-Plus-API-Token";
    private static final String CROSSREF_PLUSAPI_AUTHORZATION_HEADER_BASE = "Bearer %s";
    private static final String DOI_EXAMPLES = "10.1000/182, https://doi.org/10.1000/182";
    public static final String ILLEGAL_DOI_MESSAGE = "Illegal DOI:%s. Valid examples:" + DOI_EXAMPLES;
    private static final Logger logger = LoggerFactory.getLogger(CrossRefClient.class);
    private final transient HttpClient httpClient;
    private final String secretName;
    private final String secretKey;
    private final SecretsReader secretsReader;

    @JacocoGenerated
    public CrossRefClient() {
        this(HttpClient.newHttpClient(), new Environment(), new SecretsReader());
    }

    public CrossRefClient(HttpClient httpClient, Environment environment, SecretsReader secretsReader) {
        this.httpClient = httpClient;
        this.secretsReader = secretsReader;
        secretName = environment.readEnv(CROSSREFPLUSAPITOKEN_NAME_ENV);
        secretKey = environment.readEnv(CROSSREFPLUSAPITOKEN_KEY_ENV);
    }

    /**
     * The method returns the object containing the metadata (title, author, etc.) of the publication with the specific
     * DOI, and the source where the metadata were acquired.
     *
     * @param doi a doi identifier or URL.
     * @return FetchResult contains the JSON object and the location from where it was fetched.
     */
    public Optional<MetadataAndContentLocation> fetchDataForDoi(String doi) {
        URI targetUri = createUrlToCrossRef(doi);
        return fetchJson(targetUri);
    }

    protected URI createUrlToCrossRef(String doi) {
        var crossRefWorks = UriWrapper.fromUri(CROSSREF_LINK).addChild(WORKS);
        return Optional.ofNullable(doi)
            .map(URI::create)
            .map(URI::getPath)
            .filter(path -> !path.isBlank())
            .map(crossRefWorks::addChild)
            .map(UriWrapper::getUri)
            .orElseThrow(() -> new IllegalArgumentException(String.format(ILLEGAL_DOI_MESSAGE,doi)));
    }

    private Optional<MetadataAndContentLocation> fetchJson(URI doiUri) {
        HttpRequest request = createRequest(doiUri);
        try {
            return Optional.ofNullable(getFromWeb(request))
                .map(json -> new MetadataAndContentLocation(CROSSREF_LINK, json));
        } catch (InterruptedException
                     | ExecutionException
                     | NotFoundException
                     | BadRequestException e) {
            String details = FETCH_ERROR + doiUri;
            logger.warn(details);
            logger.warn(e.getMessage());
            return Optional.empty();
        }
    }

    private HttpRequest createRequest(URI doiUri) {
        HttpRequest.Builder builder = HttpRequest.newBuilder(doiUri)
            .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON)
            .header(HttpHeaders.USER_AGENT, CROSSREF_USER_AGENT)
            .timeout(Duration.ofSeconds(TIMEOUT_DURATION))
            .GET();

        logger.info(ADDING_TOKEN_IN_HEADER);
        builder.setHeader(CROSSREF_PLUSAPI_HEADER,
                          String.format(CROSSREF_PLUSAPI_AUTHORZATION_HEADER_BASE, getCrossRefApiPlusToken()));

        return builder.build();
    }

    private String getFromWeb(HttpRequest request)
        throws InterruptedException, ExecutionException {
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
        return response.statusCode() == HttpURLConnection.HTTP_OK;
    }

    private String getCrossRefApiPlusToken() {
        try {
            return secretsReader.fetchSecret(secretName, secretKey);
        } catch (ErrorReadingSecretException e) {
            logger.error(CROSSREF_API_KEY_SECRET_NOT_FOUND_TEMPLATE, secretName, secretKey);
            throw new RuntimeException(e);
        }
    }
}
