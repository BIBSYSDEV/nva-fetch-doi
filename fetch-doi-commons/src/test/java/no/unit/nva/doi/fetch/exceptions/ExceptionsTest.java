package no.unit.nva.doi.fetch.exceptions;

import static java.net.HttpURLConnection.HTTP_BAD_GATEWAY;
import static java.net.HttpURLConnection.HTTP_BAD_REQUEST;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;

class ExceptionsTest {

  private static final String MESSAGE = "message";

  @Test
  void canCreateInsertPublicationException() {
    CreatePublicationException exception = new CreatePublicationException(MESSAGE);
    assertNotNull(exception);
    assertEquals(HTTP_BAD_GATEWAY, exception.statusCode());
  }

  @Test
  void canCreateMalformedRequestException() {
    MalformedRequestException exception = new MalformedRequestException(MESSAGE);
    assertNotNull(exception);
    assertEquals(HTTP_BAD_REQUEST, exception.statusCode());
  }

  @Test
  void canCreateMetadataFetchException() {
    MetadataFetchException exception = new MetadataFetchException(MESSAGE, null);
    assertNotNull(exception);
    assertEquals(HTTP_BAD_GATEWAY, exception.statusCode());
  }

  @Test
  void canCreateMetadataNotFoundException() {
    MetadataNotFoundException exception = new MetadataNotFoundException(MESSAGE);
    assertNotNull(exception);
    assertEquals(HTTP_BAD_REQUEST, exception.statusCode());
  }

  @Test
  void canCreateNoContentLocationFoundException() {
    NoContentLocationFoundException exception = new NoContentLocationFoundException(MESSAGE);
    assertNotNull(exception);
    assertEquals(HTTP_BAD_GATEWAY, exception.statusCode());
  }

  @Test
  void canCreateNoPublicationException() {
    NoPublicationException exception = new NoPublicationException(MESSAGE);
    assertNotNull(exception);
    assertEquals(HTTP_BAD_GATEWAY, exception.statusCode());
  }

  @Test
  void canCreateTransformFailedException() {
    TransformFailedException exception = new TransformFailedException(MESSAGE);
    assertNotNull(exception);
    assertEquals(HTTP_BAD_GATEWAY, exception.statusCode());
  }

  @Test
  void canCreateUnsupportedDocumentTypeException() {
    UnsupportedDocumentTypeException exception = new UnsupportedDocumentTypeException(MESSAGE);
    assertNotNull(exception);
    assertEquals(HTTP_BAD_GATEWAY, exception.statusCode());
  }
}
