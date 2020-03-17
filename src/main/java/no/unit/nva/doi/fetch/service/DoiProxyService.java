package no.unit.nva.doi.fetch.service;

import static java.util.Collections.singletonMap;
import static javax.ws.rs.core.HttpHeaders.AUTHORIZATION;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;

import com.fasterxml.jackson.databind.JsonNode;
import java.net.URL;
import java.util.Optional;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
import org.apache.http.HttpStatus;

public class DoiProxyService {

    public static final String DATACITE_JSON = "application/vnd.datacite.datacite+json";
    public static final String PATH = "/doi";
    public static final String DOI = "doi";
    public static final String DATACITE_ORIGIN = "datacite";
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
     */
    public Optional<DoiProxyResponse> lookup(URL doiUrl, String apiUrl, String authorization) {
        Invocation invocation = client.target(apiUrl).path(PATH)
                                      .request(DATACITE_JSON)
                                      .header(AUTHORIZATION, authorization)
                                      .buildPost(Entity.entity(singletonMap(DOI, doiUrl), APPLICATION_JSON));
        Response response = invocation.invoke();
        if (responseIsSuccessful(response)) {
            return Optional.of(returnBodyAndContentLocation(response));
        } else {
            return Optional.empty();
        }
    }

    private DoiProxyResponse returnBodyAndContentLocation(Response response) {
        String contentLocation = response.getHeaders().getFirst(HttpHeaders.CONTENT_LOCATION).toString();
        JsonNode body = response.readEntity(JsonNode.class);
        if (contentLocation.toLowerCase().contains(DATACITE_ORIGIN)) {
            return new DoiProxyResponse(body, MetadataSource.DataCite);
        } else {
            return new DoiProxyResponse(body, MetadataSource.Crossref);
        }
    }

    private boolean responseIsSuccessful(Response response) {
        return response.getStatus() == HttpStatus.SC_OK;
    }

    public class DoiProxyResponse {

        private JsonNode jsonNode;
        private MetadataSource metadataSource;

        public DoiProxyResponse(JsonNode jsonNode, MetadataSource metadataSource) {
            this.jsonNode = jsonNode;
            this.metadataSource = metadataSource;
        }

        public JsonNode getJsonNode() {
            return jsonNode;
        }

        public MetadataSource getMetadataSource() {
            return metadataSource;
        }
    }
}
