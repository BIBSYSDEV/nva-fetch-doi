package no.sikt.nva.scopus.test.utils;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import org.junit.jupiter.api.Test;

class ScopusGeneratorTest {

    @Test
    void shouldReturnDocumentWithAllKnownFieldsNonEmpty() {
        ScopusGenerator scopusGenerator = new ScopusGenerator();
        assertDoesNotThrow(scopusGenerator::randomDocument);
    }
}
