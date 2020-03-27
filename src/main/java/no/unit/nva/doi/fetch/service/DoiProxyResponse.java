package no.unit.nva.doi.fetch.service;

import com.fasterxml.jackson.databind.JsonNode;

public class DoiProxyResponse {

    private final JsonNode jsonNode;
    private final String metadataSource;

    public DoiProxyResponse(JsonNode jsonNode, String metadataSource) {
        this.jsonNode = jsonNode;
        this.metadataSource = metadataSource;
    }

    public JsonNode getJsonNode() {
        return jsonNode;
    }

    public String getMetadataSource() {
        return metadataSource;
    }
}
