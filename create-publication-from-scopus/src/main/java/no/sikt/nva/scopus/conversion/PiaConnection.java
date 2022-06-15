package no.sikt.nva.scopus.conversion;

import static nva.commons.core.attempt.Try.attempt;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.io.IOException;
import java.lang.reflect.Type;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Optional;
import no.sikt.nva.scopus.conversion.model.Author;
import nva.commons.core.Environment;
import nva.commons.core.JacocoGenerated;
import nva.commons.core.paths.UriWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PiaConnection {

    public static final String CRISTIN_PERSON_PATH = "/cristin/person/";
    private static final String PIA_RESPONSE_ERROR = "Pia responded with status code";
    private static final String COULD_NOT_GET_ERROR_MESSAGE = "Could not get response from Pia for scopus id ";
    private final HttpClient httpClient;
    private static final String USERNAME_PASSWORD_DELIMITER = ":";

    private static final String AUTHORIZATION = "Authorization";
    private static final String BASIC_AUTHORIZATION = "Basic %s";
    private static final String PIA_REST_API = new Environment().readEnv("PIA_REST_API");
    private static final String PIA_USERNAME = new Environment().readEnv("PIA_USERNAME");
    private static final String PIA_PASSWORD = new Environment().readEnv("PIA_PASSWORD");

    private static final String NVA_DOMAIN = new Environment().readEnv("API_HOST");

    private static final Logger logger = LoggerFactory.getLogger(PiaConnection.class);
    private final transient String piaAuthorization;
    private final String piaHost;

    public PiaConnection(HttpClient httpClient, String piaHost) {
        this.httpClient = httpClient;
        this.piaHost = piaHost;
        this.piaAuthorization = createAuthorization();
    }

    @JacocoGenerated
    public PiaConnection() {
        this(HttpClient.newBuilder().version(HttpClient.Version.HTTP_2).build(),
             PIA_REST_API);
    }

    private String createAuthorization() {
        String loginPassword = PIA_USERNAME + USERNAME_PASSWORD_DELIMITER + PIA_PASSWORD;
        return String.format(BASIC_AUTHORIZATION, Base64.getEncoder().encodeToString(loginPassword.getBytes()));
    }

    private HttpRequest createRequest(URI uri) {
        return HttpRequest.newBuilder()
                   .uri(uri)
                   .setHeader(AUTHORIZATION, piaAuthorization)
                   .GET()
                   .build();
    }

    private String getPiaJsonAsString(String scopusId) {
        var uri =
            UriWrapper.fromUri(piaHost)
                .addChild("sentralimport")
                .addChild("authors")
                .addQueryParameter(
                    "author_id", "SCOPUS:" + scopusId).getUri();
        return attempt(
            () -> getPiaResponse(uri))
                   .map(this::getBodyFromResponse)
                   .orElseThrow(
                       fail -> logExpectionAndThrowRuntimeError(fail.getException(), COULD_NOT_GET_ERROR_MESSAGE
                                                                                     + scopusId));
    }

    private RuntimeException logExpectionAndThrowRuntimeError(Exception exception, String message) {
        logger.info(message);
        return exception instanceof RuntimeException
                   ? (RuntimeException) exception
                   : new RuntimeException(exception);
    }

    private String getBodyFromResponse(HttpResponse<String> response) {
        if (response.statusCode() != HttpURLConnection.HTTP_OK) {
            logger.info(PIA_RESPONSE_ERROR + response.statusCode());
            throw new RuntimeException();
        }
        return response.body();
    }

    private HttpResponse getPiaResponse(URI uri) throws IOException, InterruptedException {
        var request = createRequest(uri);
        var response = httpClient.send(request, BodyHandlers.ofString());
        return response;
    }

    private List<Author> getPiaAuthorResponse(String scopusID) {
        var piaResponse = getPiaJsonAsString(scopusID);
        Type listType = new TypeToken<ArrayList<Author>>() {
        }.getType();
        var gson = new Gson();
        return gson.fromJson(piaResponse, listType);
    }

    private Optional<Integer> getCristinNumber(List<Author> authors) {
        var optionalAuthWithCristinId = authors.stream().filter(author -> hasCristinId(author)).findFirst();
        return optionalAuthWithCristinId.map(author -> Optional.of(author.getCristinId())).orElse(Optional.empty());
    }

    private boolean hasCristinId(Author author) {
        return author.getCristinId() != 0;
    }

    public URI getCristinID(String scopusId) {
        List<Author> piaAuthorResponse = getPiaAuthorResponse(scopusId);
        var optionalCristinNumber = getCristinNumber(piaAuthorResponse);
        return optionalCristinNumber.map(
                cristinNumber -> UriWrapper.fromUri(NVA_DOMAIN + CRISTIN_PERSON_PATH + cristinNumber).getUri())
                   .orElse(null);
    }
}
