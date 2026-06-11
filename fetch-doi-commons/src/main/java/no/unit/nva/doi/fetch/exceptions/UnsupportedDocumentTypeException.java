package no.unit.nva.doi.fetch.exceptions;

import java.net.HttpURLConnection;
import nva.commons.apigateway.exceptions.ApiGatewayException;

public class UnsupportedDocumentTypeException extends ApiGatewayException {

  public UnsupportedDocumentTypeException(String message) {
    super(message);
  }

  @Override
  protected Integer statusCode() {
    return HttpURLConnection.HTTP_BAD_GATEWAY;
  }
}
