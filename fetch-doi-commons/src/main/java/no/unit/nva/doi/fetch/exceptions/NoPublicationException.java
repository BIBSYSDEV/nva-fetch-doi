package no.unit.nva.doi.fetch.exceptions;

import nva.commons.apigateway.exceptions.ApiGatewayException;
import java.net.HttpURLConnection;

public class NoPublicationException extends ApiGatewayException {

    public NoPublicationException(String message) {
        super(message);
    }

    @Override
    protected Integer statusCode() {
        return HttpURLConnection.HTTP_BAD_GATEWAY;
    }
}
