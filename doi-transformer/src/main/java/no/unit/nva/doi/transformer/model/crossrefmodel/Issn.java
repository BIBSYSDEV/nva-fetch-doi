package no.unit.nva.doi.transformer.model.crossrefmodel;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Arrays;
import java.util.stream.Collectors;

@SuppressWarnings("PMD.ShortClassName")
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
        private static final String DELIMITER = ", ";
        private static String ISSN_TYPE_NAMES = Arrays.stream(values()).map(issnType -> issnType.getName()).collect(
            Collectors.joining(DELIMITER));

        private static String ERROR_MESSAGE = "Invalid Type:%s.  Allowed types are: " + ISSN_TYPE_NAMES;

        IssnType(String name) {
            this.name = name;
        }

        public static IssnType getType(String name) {
            return Arrays.stream(values())
                .filter(issnType -> issnType.name.equalsIgnoreCase(name)).findFirst()
                .orElseThrow(() -> new RuntimeException(String.format(ERROR_MESSAGE, name)));
        }

        public String getName() {
            return name;
        }
    }
}
