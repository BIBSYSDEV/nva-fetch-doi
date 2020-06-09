package no.unit.nva.doi.transformer.exception;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class MissingClaimExceptionTest {

    public static final String EXPECTED_MESSAGE = "Some message";

    @DisplayName("MissingClaimException can be thrown")
    @Test
    void missingClaimExceptionIsThrown() {
        assertThrows(MissingClaimException.class, () -> {
            throw new MissingClaimException(EXPECTED_MESSAGE);
        });
    }

    @DisplayName("MissingClaimException has message when thrown")
    @Test
    void missingClaimExceptionThrowsAndHasMessage() {
        Exception exception = assertThrows(MissingClaimException.class, () -> {
            throw new MissingClaimException(EXPECTED_MESSAGE);
        });

        assertEquals(EXPECTED_MESSAGE, exception.getMessage());
    }
}