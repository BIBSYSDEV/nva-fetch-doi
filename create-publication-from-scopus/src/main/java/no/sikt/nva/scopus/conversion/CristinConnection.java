package no.sikt.nva.scopus.conversion;

import static nva.commons.core.attempt.Try.attempt;
import com.google.gson.Gson;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.util.Optional;
import no.sikt.nva.scopus.conversion.model.cristin.Person;
import nva.commons.core.Environment;
import nva.commons.core.JacocoGenerated;
import nva.commons.core.attempt.Failure;
import nva.commons.core.paths.UriWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class CristinConnection {
    private static final Logger logger = LoggerFactory.getLogger(CristinConnection.class);

    public static final String CRISTIN_PERSON_PATH = "/cristin/person/";

    public static final String CRISTIN_RESPONDED_WITH_BAD_STATUS_CODE_ERROR_MESSAGE = "cristin responded with status "
                                                                                      + "code: ";
    public static final String COULD_NOT_EXTRACT_CRISTIN_PERSON_ERROR_MESSAGE = "could not extract cristin person";

    private static final String NVA_DOMAIN = new Environment().readEnv("API_HOST");

    private final HttpClient httpClient;

    public CristinConnection(HttpClient httpClient) {
        this.httpClient = httpClient;
    }

    @JacocoGenerated
    public CristinConnection() {
        this(HttpClient.newBuilder().version(HttpClient.Version.HTTP_2).build());
    }

    public Optional<Person> getCristinPersonByCristinId(URI cristinId) {


        return Optional.ofNullable(attempt(() -> createRequest(cristinId))
                                       .map(this::getCristinResponse)
                                       .map(this::getBodyFromResponse)
                                       .map(this::getPiaAuthorResponse)
                                       .orElse(this::logFailureAndReturnNull));


    }

    private Person logFailureAndReturnNull(Failure<Person> failure) {
        logger.info(COULD_NOT_EXTRACT_CRISTIN_PERSON_ERROR_MESSAGE, failure.getException());
        return null;
    }

    private Person getPiaAuthorResponse(String json) {
        var gson = new Gson();
        return gson.fromJson(json, Person.class);
    }

    private String getBodyFromResponse(HttpResponse<String> response) {
        if (response.statusCode() != HttpURLConnection.HTTP_OK) {
            logger.info(CRISTIN_RESPONDED_WITH_BAD_STATUS_CODE_ERROR_MESSAGE + response.statusCode());
            throw new RuntimeException();
        }
        return response.body();
    }

    private HttpResponse<String> getCristinResponse(HttpRequest httpRequest) throws IOException, InterruptedException {
        return httpClient.send(httpRequest, BodyHandlers.ofString());
    }

    private HttpRequest createRequest(URI uri) {
        return HttpRequest.newBuilder()
                   .uri(uri)
                   .GET()
                   .build();
    }
}
