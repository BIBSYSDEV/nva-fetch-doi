package no.unit.nva.doi.fetch.exceptions;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.apache.http.HttpStatus;
import org.junit.jupiter.api.Test;

public class ExceptionsTest {

    public static final String MESSAGE = "message";

    @Test
    public void canCreateInsertPublicationException() {
        CreatePublicationException exception = new CreatePublicationException(MESSAGE);
        assertNotNull(exception);
        assertEquals(HttpStatus.SC_BAD_GATEWAY, exception.statusCode());
    }

    @Test
    public void canCreateMalformedRequestException() {
        MalformedRequestException exception = new MalformedRequestException(MESSAGE);
        assertNotNull(exception);
        assertEquals(HttpStatus.SC_BAD_REQUEST, exception.statusCode());
    }

    @Test
    public void canCreateMetadataNotFoundException() {
        MetadataNotFoundException exception = new MetadataNotFoundException(MESSAGE);
        assertNotNull(exception);
        assertEquals(HttpStatus.SC_BAD_GATEWAY, exception.statusCode());
    }

    @Test
    public void canCreateNoContentLocationFoundException() {
        NoContentLocationFoundException exception = new NoContentLocationFoundException(MESSAGE);
        assertNotNull(exception);
        assertEquals(HttpStatus.SC_BAD_GATEWAY, exception.statusCode());
    }

    @Test
    public void canCreateNoPublicationException() {
        NoPublicationException exception = new NoPublicationException(MESSAGE);
        assertNotNull(exception);
        assertEquals(HttpStatus.SC_BAD_GATEWAY, exception.statusCode());
    }

    @Test
    public void canCreateTransformFailedException() {
        TransformFailedException exception = new TransformFailedException(MESSAGE);
        assertNotNull(exception);
        assertEquals(HttpStatus.SC_BAD_GATEWAY, exception.statusCode());
    }

    @Test
    public void canCreateUnsupportedDocumentTypeException() {
        UnsupportedDocumentTypeException exception = new UnsupportedDocumentTypeException(MESSAGE);
        assertNotNull(exception);
        assertEquals(HttpStatus.SC_BAD_GATEWAY, exception.statusCode());
    }

}
