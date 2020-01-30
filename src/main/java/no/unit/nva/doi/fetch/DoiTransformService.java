package no.unit.nva.doi.fetch;

import com.fasterxml.jackson.databind.JsonNode;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;

import static javax.ws.rs.core.HttpHeaders.AUTHORIZATION;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;

public class DoiTransformService {

    public static final String PATH = "/doi-transform";
    private final Client client;

    protected DoiTransformService(Client client) {
        this.client = client;
    }

    public DoiTransformService() {
        this(ClientBuilder.newClient());
    }

    /**
     * Transform Datacite data to NVA data.
     *
     * @param dataciteData  dataciteData
     * @param host  host
     * @param authorization authorization
     * @return  jsonNode
     */
    public JsonNode transform(JsonNode dataciteData, String host, String authorization) {

        JsonNode response = client.target(host).path(PATH)
                .request(APPLICATION_JSON)
                .header(AUTHORIZATION, authorization)
                .post(Entity.entity(dataciteData, APPLICATION_JSON), JsonNode.class);

        System.out.println(response);

        return response;
    }
}
