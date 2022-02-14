package no.sikt.nva.scopus.test.utils;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import no.scopus.generated.CitationtypeAtt;
import org.junit.jupiter.api.Test;

class ScopusGeneratorTest {

    @Test
    void shouldReturnDocumentWithAllKnownFieldsNonEmpty() {
        assertDoesNotThrow(() -> ScopusGenerator.randomDocument(CitationtypeAtt.AR));
    }
}
