package no.unit.nva.doi.fetch.exceptions;

import java.net.HttpURLConnection;
import nva.commons.apigateway.exceptions.ApiGatewayException;

public class MetadataFetchException extends ApiGatewayException {

  public MetadataFetchException(String message, Exception cause) {
    super(cause, message);
  }

  @Override
  protected Integer statusCode() {
    return HttpURLConnection.HTTP_BAD_GATEWAY;
  }
}
