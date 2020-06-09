package no.unit.nva.doi.utils;

import org.apache.http.HttpStatus;

public class HttpResponseStatus500<S> extends AbstractHttpResponse<S> {

    public HttpResponseStatus500(S responseBody) {
        super(responseBody);
    }

    @Override
    public int statusCode() {
        return HttpStatus.SC_INTERNAL_SERVER_ERROR;
    }

    @Override
    public S body() {
        return responseBody;
    }
}
