package no.unit.nva.doi.fetch.service;

import com.fasterxml.jackson.databind.JsonNode;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;

import static javax.ws.rs.core.HttpHeaders.AUTHORIZATION;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;

public class ResourcePersistenceService {

    public static final String PATH = "/resource";
    private final Client client;

    protected ResourcePersistenceService(Client client) {
        this.client = client;
    }

    public ResourcePersistenceService() {
        this(ClientBuilder.newClient());
    }

    /**
     * Insert Resource in Database.
     *
     * @param publication  dataciteData
     * @param apiUrl  apiUrl
     * @param authorization authorization
     */

    public void insertPublication(JsonNode publication, String apiUrl, String authorization) {

        client.target(apiUrl).path(PATH)
                .request(APPLICATION_JSON)
                .header(AUTHORIZATION, authorization)
                .post(Entity.entity(publication, APPLICATION_JSON), JsonNode.class);
    }

}
