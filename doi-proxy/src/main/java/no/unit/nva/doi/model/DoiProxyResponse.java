package no.unit.nva.doi.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.Objects;
import nva.commons.utils.JacocoGenerated;

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

    @JacocoGenerated
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        DoiProxyResponse that = (DoiProxyResponse) o;
        return Objects.equals(getJsonNode(), that.getJsonNode())
            && Objects.equals(getMetadataSource(), that.getMetadataSource());
    }

    @JacocoGenerated
    @Override
    public int hashCode() {
        return Objects.hash(getJsonNode(), getMetadataSource());
    }
}
