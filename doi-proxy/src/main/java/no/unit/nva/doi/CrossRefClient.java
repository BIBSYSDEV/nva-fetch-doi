package no.unit.nva.doi;

import com.amazonaws.SdkClientException;
import nva.commons.core.Environment;
import nva.commons.core.JacocoGenerated;
import nva.commons.secrets.ErrorReadingSecretException;
import nva.commons.secrets.SecretsReader;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.client.utils.URLEncodedUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.BadRequestException;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ExecutionException;

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
            "CrossRefApiPlusToken.isPresent() == true, adding token in header";
    public static final String EXCEPTION_READING_API_SECRET_FROM_AWS_SECRETS_MANAGER =
            "Exception decoding CrossRef-Plus-API secret from AWS secretsManager ";
    public static final String NOT_FOUND_IN_AWS_SECRETS_MANAGER =
            "CrossRef-Plus-API secret not found in AWS secretsManager ";
    private static final String CROSSREF_PLUSAPI_HEADER = "Crossref-Plus-API-Token";
    private static final String CROSSREF_PLUSAPI_AUTHORZATION_HEADER_BASE = "Bearer %s";
    private static final String DOI_EXAMPLES = "10.1000/182, https://doi.org/10.1000/182";
    public static final String ILLEGAL_DOI_MESSAGE = "Illegal DOI:%s. Valid examples:" + DOI_EXAMPLES;
    private static final String CROSSREFPLUSAPITOKEN_NAME_ENV = "CROSSREFPLUSAPITOKEN_NAME";
    private static final String CROSSREFPLUSAPITOKEN_KEY_ENV = "CROSSREFPLUSAPITOKEN_KEY";
    private static final Logger logger = LoggerFactory.getLogger(CrossRefClient.class);
    private final transient HttpClient httpClient;
    private final Optional<String> secretName;
    private final Optional<String> secretKey;

    @JacocoGenerated
    public CrossRefClient() {
        this(HttpClient.newHttpClient(), new Environment());
    }

    public CrossRefClient(HttpClient httpClient, Environment environment) {
        this.httpClient = httpClient;
        secretName = environment.readEnvOpt(CROSSREFPLUSAPITOKEN_NAME_ENV);
        secretKey = environment.readEnvOpt(CROSSREFPLUSAPITOKEN_KEY_ENV);
    }

    /**
     * The method returns the object containing the metadata (title, author, etc.) of the publication with the specific
     * DOI, and the source where the metadata were acquired.
     *
     * @param doi a doi identifier or URL.
     * @return FetchResult contains the JSON object and the location from where it was fetched.
     * @throws URISyntaxException when the input cannot be transformed to a valid URI.
     */
    public Optional<MetadataAndContentLocation> fetchDataForDoi(String doi) throws URISyntaxException {
        URI targetUri = createUrlToCrossRef(doi);
        return fetchJson(targetUri);
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
        if (getCrossRefApiPlusToken().isPresent()) {
            logger.info(ADDING_TOKEN_IN_HEADER);
            builder.setHeader(CROSSREF_PLUSAPI_HEADER,
                    String.format(CROSSREF_PLUSAPI_AUTHORZATION_HEADER_BASE, getCrossRefApiPlusToken().get()));
        }
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
        int statusCode = response.statusCode();
        return statusCode >= HttpURLConnection.HTTP_OK && statusCode < HttpURLConnection.HTTP_MULT_CHOICE;
    }

    protected URI createUrlToCrossRef(String doi)
            throws URISyntaxException {
        List<String> doiPathSegments = extractPathSegmentsFromDoiUri(doi);
        List<String> pathSegments = composeAllPathSegmentsForCrossrefUrl(doiPathSegments);
        return addPathSegments(pathSegments);
    }

    private URI addPathSegments(List<String> pathSegments) throws URISyntaxException {
        return new URIBuilder(CROSSREF_LINK)
                .setPathSegments(pathSegments)
                .build();
    }

    private List<String> composeAllPathSegmentsForCrossrefUrl(List<String> doiPathSegments) {
        List<String> pathSegments = new ArrayList<>();
        pathSegments.add(WORKS);
        pathSegments.addAll(doiPathSegments);
        return pathSegments;
    }

    private List<String> extractPathSegmentsFromDoiUri(String doi) {
        String path = URI.create(doi).getPath();
        if (Objects.isNull(path) || path.isBlank()) {
            throw new IllegalArgumentException(ILLEGAL_DOI_MESSAGE + doi);
        }
        return URLEncodedUtils.parsePathSegments(path);
    }


    private Optional<String> getCrossRefApiPlusToken() {
        try {
            return Optional.ofNullable(new SecretsReader().fetchSecret(secretName.get(), secretKey.get()));
        } catch (ErrorReadingSecretException | SdkClientException e) {
            logger.error(EXCEPTION_READING_API_SECRET_FROM_AWS_SECRETS_MANAGER);
            return Optional.empty();
        }
    }
}
