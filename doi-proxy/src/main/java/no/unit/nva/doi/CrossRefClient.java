package no.unit.nva.doi;

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
import javax.ws.rs.BadRequestException;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;

import com.amazonaws.services.secretsmanager.AWSSecretsManager;
import com.amazonaws.services.secretsmanager.AWSSecretsManagerClientBuilder;
import com.amazonaws.services.secretsmanager.model.DecryptionFailureException;
import com.amazonaws.services.secretsmanager.model.GetSecretValueRequest;
import com.amazonaws.services.secretsmanager.model.GetSecretValueResult;
import com.amazonaws.services.secretsmanager.model.InternalServiceErrorException;
import com.amazonaws.services.secretsmanager.model.InvalidParameterException;
import com.amazonaws.services.secretsmanager.model.InvalidRequestException;
import com.amazonaws.services.secretsmanager.model.ResourceNotFoundException;
import com.amazonaws.services.securitytoken.model.ExpiredTokenException;
import nva.commons.core.Environment;
import nva.commons.core.JacocoGenerated;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.client.utils.URLEncodedUtils;
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
    private static final String CROSSREF_PLUSAPI_HEADER = "Crossref-Plus-API-Token";
    private static final String CROSSREF_PLUSAPI_AUTHORZATION_HEADER_BASE = "Bearer %s";
    private static final String DOI_EXAMPLES = "10.1000/182, https://doi.org/10.1000/182";
    public static final String ILLEGAL_DOI_MESSAGE = "Illegal DOI:%s. Valid examples:" + DOI_EXAMPLES;
    private static final String CROSSREFPLUSAPITOKEN_ENV = "CROSSREFPLUSAPITOKEN_NAME";
    private static final Logger logger = LoggerFactory.getLogger(CrossRefClient.class);
    private final transient HttpClient httpClient;
    private final Optional<String> secretName;
    private final String region;

    @JacocoGenerated
    public CrossRefClient() {
        this(HttpClient.newHttpClient(), new Environment());
    }

    public CrossRefClient(HttpClient httpClient, Environment environment) {
        this.httpClient = httpClient;
        secretName = environment.readEnvOpt(CROSSREFPLUSAPITOKEN_ENV);
        region = "eu-west-1";
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
        HttpRequest.Builder builder = HttpRequest.newBuilder(doiUri);
        builder.header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON);
        builder.header(HttpHeaders.USER_AGENT, CROSSREF_USER_AGENT);
        if (getCrossRefApiPlusToken().isPresent()) {
            logger.info("CrossRefApiPlusToken.isPresent() == true");
            final String token = getCrossRefApiPlusToken().get();
            builder.header(CROSSREF_PLUSAPI_HEADER,
                    String.format(CROSSREF_PLUSAPI_AUTHORZATION_HEADER_BASE, token));
        }
        builder.timeout(Duration.ofSeconds(TIMEOUT_DURATION));
        builder.GET();
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

        Optional<String> secret = Optional.empty();
        if (secretName.isPresent()) {
            // Create a Secrets Manager client
            AWSSecretsManager client = AWSSecretsManagerClientBuilder.standard()
                    .withRegion(region)
                    .build();

            GetSecretValueRequest getSecretValueRequest = new GetSecretValueRequest()
                    .withSecretId(secretName.get());
            GetSecretValueResult getSecretValueResult = null;

            try {
                getSecretValueResult = client.getSecretValue(getSecretValueRequest);
            } catch (DecryptionFailureException | InternalServiceErrorException
                    | InvalidParameterException | InvalidRequestException | ExpiredTokenException e) {
                // Secrets Manager can't decrypt the protected secret text using the provided KMS key.
                // An error occurred on the server side.
                // You provided an invalid value for a parameter.
                // You provided a parameter value that is not valid for the current state of the resource.
                logger.error("Exception decoding CrossRef-Plus-API secret from AWS secretsManager ");
            } catch (ResourceNotFoundException e) {
                // We can't find the resource that you asked for.
                // Deal with the exception here, and/or rethrow at your discretion.
                logger.error("CrossRef-Plus-API secret not found in AWS secretsManager ");
            }

            // Decrypts secret using the associated KMS CMK.
            // Depending on whether the secret is a string or binary, one of these fields will be populated.
            if (getSecretValueResult.getSecretString() != null) {
                secret = Optional.of(getSecretValueResult.getSecretString());
            }
        }
        return secret;
    }
}
