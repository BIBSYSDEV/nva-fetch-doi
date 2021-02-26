package no.unit.nva.metadata;

import com.fasterxml.jackson.core.JsonProcessingException;
import no.unit.nva.api.CreatePublicationRequest;

import static nva.commons.core.JsonUtils.objectMapper;

public final class MetadataConverter {

    private MetadataConverter() {
    }

    public static CreatePublicationRequest fromJsonLd(String jsonld) throws JsonProcessingException {
        return objectMapper
                .readValue(jsonld, CreatePublicationRequest.class);
    }
}
