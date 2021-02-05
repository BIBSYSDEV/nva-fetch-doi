package no.unit.nva.doi.fetch.exceptions;

import nva.commons.apigateway.exceptions.ApiGatewayException;
import java.net.HttpURLConnection;

public class InsertPublicationException extends ApiGatewayException {

    public InsertPublicationException(String message) {
        super(message);
    }

    @Override
    protected Integer statusCode() {
        return HttpURLConnection.HTTP_BAD_GATEWAY;
    }
}
