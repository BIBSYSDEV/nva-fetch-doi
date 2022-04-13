package no.sikt.nva.doi.fetch.jsonconfig;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import no.unit.nva.commons.json.JsonUtils;

@SuppressWarnings({"PMD.ShortClassName","PMD.AvoidFieldNameMatchingTypeName"})
public final class Json {

    private static final ObjectMapper JSON = JsonUtils.dtoObjectMapper;

    private Json() {

    }

    public static <T> T readValue(String input, Class<T> valueType) throws JsonProcessingException {
        return JSON.readValue(input, valueType);
    }

    public static String writeValueAsString(Object value) throws JsonProcessingException {
        return JSON.writeValueAsString(value);
    }

    public static JsonNode readTree(String input) throws JsonProcessingException {
        return JSON.readTree(input);
    }

    public static JsonNode convertValue(Object fromValue, Class<JsonNode> jsonNodeClass) {
        return JSON.convertValue(fromValue,jsonNodeClass);
    }
}
