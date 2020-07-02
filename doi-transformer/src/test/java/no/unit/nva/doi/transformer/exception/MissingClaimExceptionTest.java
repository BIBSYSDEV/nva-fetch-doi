package no.unit.nva.doi.transformer.exception;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.apache.http.HttpStatus;
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

    @DisplayName("MissingClaimException has status code when thrown")
    @Test
    void missingClaimExceptionThrowsAndHasStatuscode() {
        MissingClaimException exception = assertThrows(MissingClaimException.class, () -> {
            throw new MissingClaimException(EXPECTED_MESSAGE);
        });

        assertEquals(HttpStatus.SC_BAD_REQUEST, exception.statusCode());
    }
}