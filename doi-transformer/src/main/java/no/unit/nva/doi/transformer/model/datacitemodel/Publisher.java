package no.unit.nva.doi.transformer.model.datacitemodel;

import com.fasterxml.jackson.annotation.JsonCreator;

import java.util.Map;

import static java.util.Objects.isNull;

public record Publisher(String value) {

    @JsonCreator
    public static Publisher fromJson(Object object) {
        return isNull(object) ? null : convert(object);
    }

    private static Publisher convert(Object object) {
        return switch (object) {
            case String string -> new Publisher(string);
            case Map<?, ?> hashMap -> new Publisher((String) hashMap.get("name"));
            default -> throw new IllegalArgumentException("Could not map publisher");
        };
    }
}
