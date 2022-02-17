package no.sikt.nva.scopus.exception;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import org.junit.jupiter.api.Test;

public class ExceptionsTest {

    public static final String MESSAGE = "message";

    @Test
    public void canCreateUnsupportedXmlElementException() {
        UnsupportedXmlElementException exception = new UnsupportedXmlElementException(MESSAGE);
        assertNotNull(exception);
    }

    @Test
    public void canCreateUnsupportedScrTypeExcetption() {
        UnsupportedSrcTypeException exception = new UnsupportedSrcTypeException(MESSAGE);
        assertNotNull(exception);
    }
}
