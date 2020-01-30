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
     * @param host  host
     * @param authorization authorization
     * @return  jsonNode
     */
    public JsonNode lookup(URL doiUrl, String host, String authorization) {

        JsonNode response = client.target(host).path(PATH)
                .request(DATACITE_JSON)
                .header(AUTHORIZATION, authorization)
                .post(Entity.entity(singletonMap("doiUrl", doiUrl), APPLICATION_JSON), JsonNode.class);

        System.out.println(response);

        return response;
    }
}
