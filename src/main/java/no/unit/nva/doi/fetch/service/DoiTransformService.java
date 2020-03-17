package no.unit.nva.doi.fetch.service;

import static javax.ws.rs.core.HttpHeaders.AUTHORIZATION;
import static javax.ws.rs.core.HttpHeaders.CONTENT_LOCATION;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;

import com.fasterxml.jackson.databind.JsonNode;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import no.unit.nva.doi.fetch.service.DoiProxyService.DoiProxyResponse;

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
     * @param doiProxyResponse doiProxyResponse
     * @param apiUrl           apiUrl
     * @param authorization    authorization
     * @return jsonNode
     */
    public JsonNode transform(DoiProxyResponse doiProxyResponse, String apiUrl, String authorization) {
        return client.target(apiUrl).path(PATH)
                     .request(APPLICATION_JSON)
                     .header(AUTHORIZATION, authorization)
                     .header(CONTENT_LOCATION, doiProxyResponse.getMetadataSource())
                     .post(Entity.entity(doiProxyResponse.getJsonNode(), APPLICATION_JSON), JsonNode.class);
    }
}
