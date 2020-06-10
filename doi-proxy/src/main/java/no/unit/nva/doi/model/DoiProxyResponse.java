package no.unit.nva.doi.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;

public class DoiProxyResponse {

    private final JsonNode jsonNode;
    private final String metadataSource;

    @JsonCreator
    public DoiProxyResponse(
        @JsonProperty("jsonNode") JsonNode jsonNode,
        @JsonProperty("metadataSource") String metadataSource) {
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
