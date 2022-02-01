package no.sikt.nva.scopus.exception;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import org.junit.jupiter.api.Test;

public class ExceptionsTest {

    public static final String MESSAGE = "message";

    @Test
    public void canCreateInsertPublicationException() {
        UnsupportedXmlElementException exception = new UnsupportedXmlElementException(MESSAGE);
        assertNotNull(exception);
    }
}
