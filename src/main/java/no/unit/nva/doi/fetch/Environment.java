package no.unit.nva.doi.fetch;

import java.util.Optional;

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

    public String get(String variable) {
        String value = System.getenv().get(variable);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException(ENV_VARIABLE_MISSING + variable);
        }
        return value;
    }
}
