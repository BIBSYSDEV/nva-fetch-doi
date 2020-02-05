package no.unit.nva.doi.fetch.service;

import com.fasterxml.jackson.databind.JsonNode;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import java.net.URL;

import static java.util.Collections.singletonMap;
import static javax.ws.rs.core.HttpHeaders.AUTHORIZATION;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;

public class DoiProxyService {

    public static final String DATACITE_JSON = "application/vnd.datacite.datacite+json";
    public static final String PATH = "/doi";
    public static final String DOI_URL = "doiUrl";
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
     * @param doiUrl    doiUrl
     * @param apiUrl  apiUrl
     * @param authorization authorization
     * @return  jsonNode
     */
    public JsonNode lookup(URL doiUrl, String apiUrl, String authorization) {
        return client.target(apiUrl).path(PATH)
                .request(DATACITE_JSON)
                .header(AUTHORIZATION, authorization)
                .post(Entity.entity(singletonMap(DOI_URL, doiUrl), APPLICATION_JSON), JsonNode.class);
    }
}
