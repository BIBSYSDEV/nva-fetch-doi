package no.unit.nva.doi.transformer.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class IssnCleanerTest {

    @DisplayName("IssnCleaner cleans valid, misformatted ISSNs")
    @ParameterizedTest
    @ValueSource(strings = {"00189219", "0018 9219", "0018_9219", "0018–9219", "0018—9219"})
    void issnCleanerCleansValidMisformattedIssns(String value) {
        assertEquals("0018-9219", IssnCleaner.clean(value));
    }

    @DisplayName("IssnCleaner leaves valid, well-formatted ISSNs alone")
    @ParameterizedTest
    @ValueSource(strings = {"0018-9219", "1476-4687", "1945-662X"})
    void issnCleanerReturnsIssnWhenInputIsValidAndWellFormattedIssn(String issn) {
        assertEquals(issn, IssnCleaner.clean(issn));
    }

    @DisplayName("IssnCleaner leaves null alone")
    @Test
    void issnCleanerReturnsNullWhenInputIsNull() {
        assertNull(IssnCleaner.clean(null));
    }

    @DisplayName("IssnCleaner returns null when input is empty")
    void issnCleanerReturnsNullWhenInputIsEmptyString() {
        assertNull(IssnCleaner.clean(""));
    }

    @DisplayName("IssnCleaner returns null if ISSN is invalid")
    @ParameterizedTest
    @ValueSource(strings = {"asd", "12332114", "1234-123X", "X123-1234"})
    void issnCleanerReturnsNullWhenInputIsInvalidIssn(String value) {
        assertNull(IssnCleaner.clean(value));
    }
}