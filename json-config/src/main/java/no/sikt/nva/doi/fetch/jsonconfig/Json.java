package no.sikt.nva.doi.fetch.jsonconfig;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import no.unit.nva.commons.json.JsonUtils;

@SuppressWarnings({"PMD.ShortClassName"})
public final class Json {

    private static final ObjectMapper OBJECT_MAPPER = JsonUtils.dtoObjectMapper;

    private Json() {

    }

    public static <T> T readValue(String input, Class<T> valueType) throws JsonProcessingException {
        return OBJECT_MAPPER.readValue(input, valueType);
    }

    public static String writeValueAsString(Object value) throws JsonProcessingException {
        return OBJECT_MAPPER.writeValueAsString(value);
    }

    public static JsonNode readTree(String input) throws JsonProcessingException {
        return OBJECT_MAPPER.readTree(input);
    }

    public static JsonNode convertValue(Object fromValue, Class<JsonNode> jsonNodeClass) {
        return OBJECT_MAPPER.convertValue(fromValue, jsonNodeClass);
    }
}
