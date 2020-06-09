package no.unit.nva.doi.fetch;

import java.util.Optional;
import no.unit.nva.doi.fetch.utils.JacocoGenerated;

@JacocoGenerated
public class Environment {

    public static final String ENV_VARIABLE_MISSING = "Environment variable missing:";

    /**
     * Get environment variable.
     *
     * @param name name of environment variable
     * @return optional with value of environment variable
     */
    public Optional<String> getOptional(String name) {
        String environmentVariable = System.getenv(name);

        if (environmentVariable == null || environmentVariable.isEmpty()) {
            return Optional.empty();
        }

        return Optional.of(environmentVariable);
    }

    /**
     * Get an environment variable or throw Exception.
     *
     * @param variable the name of the variable.
     * @return the value of the variable.
     */
    public String get(String variable) {
        String value = System.getenv().get(variable);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException(ENV_VARIABLE_MISSING + variable);
        }
        return value;
    }
}
