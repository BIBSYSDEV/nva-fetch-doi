package no.unit.nva.doi.transformer.model.crossrefmodel;

import com.fasterxml.jackson.annotation.JsonProperty;
import nva.commons.core.JacocoGenerated;

import java.util.Arrays;
import java.util.stream.Collectors;

@SuppressWarnings("PMD.ShortClassName")
public class Isxn {

    @JsonProperty("value")
    private String value;
    @JsonProperty("type")
    private IsxnType type;

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public IsxnType getType() {
        return type;
    }

    public void setType(String type) {
        this.type = IsxnType.getType(type);
    }

    public enum IsxnType {
        PRINT("print"),
        ELECTRONIC("electronic");

        private final String name;
        private static final String DELIMITER = ", ";
        private static String ISSN_TYPE_NAMES = Arrays.stream(values()).map(issnType -> issnType.getName()).collect(
            Collectors.joining(DELIMITER));

        private static String ERROR_MESSAGE = "Invalid Type:%s.  Allowed types are: " + ISSN_TYPE_NAMES;

        IsxnType(String name) {
            this.name = name;
        }

        /**
         * Get IssnType from String. Case-insensitive.
         * @param name The string representation of the type
         * @return an IssnType if the string has a valid value or throw a RuntimeException if not.
         */
        @JacocoGenerated
        public static IsxnType getType(String name) {
            return Arrays.stream(values())
                .filter(issnType -> issnType.name.equalsIgnoreCase(name)).findFirst()
                .orElseThrow(() -> new RuntimeException(String.format(ERROR_MESSAGE, name)));
        }

        public String getName() {
            return name;
        }
    }
}
