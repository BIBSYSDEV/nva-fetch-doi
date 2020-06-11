package no.unit.nva.doi.fetch.exceptions;

import nva.commons.exceptions.ApiGatewayException;
import org.apache.http.HttpStatus;

public class MetadataNotFoundException extends ApiGatewayException {

    public MetadataNotFoundException(String message) {
        super(message);
    }

    @Override
    protected Integer statusCode() {
        return HttpStatus.SC_BAD_GATEWAY;
    }
}
