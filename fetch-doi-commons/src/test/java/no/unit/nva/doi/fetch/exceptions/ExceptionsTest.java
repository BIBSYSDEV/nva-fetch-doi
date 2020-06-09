package no.unit.nva.doi.fetch.exceptions;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;

public class ExceptionsTest {

    public static final String MESSAGE = "message";

    @Test
    public void canCreateInsertPublicationException() {
        InsertPublicationException exception = new InsertPublicationException(MESSAGE);
        assertNotNull(exception);
    }

    @Test
    public void canCreateMalformedRequestException() {
        MalformedRequestException exception = new MalformedRequestException(MESSAGE);
        assertNotNull(exception);
    }

    @Test
    public void canCreateMetadataNotFoundException() {
        MetadataNotFoundException exception = new MetadataNotFoundException(MESSAGE);
        assertNotNull(exception);
    }

    @Test
    public void canCreateNoContentLocationFoundException() {
        NoContentLocationFoundException exception = new NoContentLocationFoundException(MESSAGE);
        assertNotNull(exception);
    }

    @Test
    public void canCreateNoPublicationException() {
        NoPublicationException exception = new NoPublicationException(MESSAGE);
        assertNotNull(exception);
    }

    @Test
    public void canCreateTransformFailedException() {
        TransformFailedException exception = new TransformFailedException(MESSAGE);
        assertNotNull(exception);
    }

}
