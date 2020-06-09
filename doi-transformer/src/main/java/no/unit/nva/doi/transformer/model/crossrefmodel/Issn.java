package no.unit.nva.doi.transformer.model.crossrefmodel;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Arrays;

public class Issn {

    @JsonProperty("value")
    private String value;
    @JsonProperty("type")
    private IssnType type;

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public IssnType getType() {
        return type;
    }

    public void setType(String type) {
        this.type = IssnType.getType(type);
    }

    public enum IssnType {
        PRINT("print"),
        ELECTRONIC("electronic");

        private final String name;

        IssnType(String name) {
            this.name = name;
        }

        public static IssnType getType(String name) {
            return Arrays.stream(values()).filter(issnType -> issnType.name.equals(name)).findFirst()
                         .orElseThrow(RuntimeException::new);
        }

        public String getName() {
            return name;
        }
    }
}
