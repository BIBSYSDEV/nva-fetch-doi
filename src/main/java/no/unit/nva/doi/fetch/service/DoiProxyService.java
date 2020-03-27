package no.unit.nva.doi.fetch.service;

import static java.util.Collections.singletonMap;
import static javax.ws.rs.core.HttpHeaders.AUTHORIZATION;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;

import com.fasterxml.jackson.databind.JsonNode;
import java.net.URL;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
import no.unit.nva.doi.fetch.service.exceptions.NoContentLocationFoundException;
import org.apache.http.HttpStatus;

public class DoiProxyService {

    public static final String DATACITE_JSON = "application/vnd.datacite.datacite+json";
    public static final String PATH = "/doi";
    public static final String DOI = "doi";
    private final Client client;

    protected DoiProxyService(Client client) {
        this.client = client;
    }

    public DoiProxyService() {
        this(ClientBuilder.newClient());
    }

    /**
     * Lookup data from DOI URL.
     *
     * @param doiUrl        doiUrl
     * @param apiUrl        apiUrl
     * @param authorization authorization
     * @return jsonNode
     * @throws NoContentLocationFoundException thrown when the header Content-Location is missing from the response
     */
    public Optional<DoiProxyResponse> lookup(URL doiUrl, String apiUrl, String authorization)
        throws NoContentLocationFoundException {
        Invocation invocation = client.target(apiUrl)
                                      .path(PATH)
                                      .request(DATACITE_JSON)
                                      .header(AUTHORIZATION, authorization)
                                      .buildPost(Entity.entity(queryBody(doiUrl), APPLICATION_JSON));
        try (Response response = invocation.invoke()) {
            if (responseIsSuccessful(response)) {
                return Optional.of(returnBodyAndContentLocation(response));
            } else {
                return Optional.empty();
            }
        }
    }

    private Map<String, URL> queryBody(URL doiUrl) {
        return singletonMap(DOI, doiUrl);
    }

    private DoiProxyResponse returnBodyAndContentLocation(Response response) throws NoContentLocationFoundException {
        String contentLocation = response.getStringHeaders().getFirst(HttpHeaders.CONTENT_LOCATION);
        if (isNullOrEmpty(contentLocation)) {
            throw new NoContentLocationFoundException("ContentLocation header should not be empty");
        }
        JsonNode body = response.readEntity(JsonNode.class);
        return new DoiProxyResponse(body, contentLocation);
    }

    private boolean isNullOrEmpty(String contentLocation) {
        return Objects.isNull(contentLocation) || contentLocation.length() == 0;
    }

    private boolean responseIsSuccessful(Response response) {
        int status = response.getStatus();
        // status should be in the range [200,300)
        return status >= HttpStatus.SC_OK && status < HttpStatus.SC_MULTIPLE_CHOICES;
    }
}
