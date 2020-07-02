package no.unit.nva.doi.utils;

import org.apache.http.HttpStatus;

public class HttpResponseStatus200<S> extends AbstractHttpResponse<S> {

    public HttpResponseStatus200(S responseBody) {
        super(responseBody);
    }

    @Override
    public int statusCode() {
        return HttpStatus.SC_OK;
    }

    @Override
    public S body() {
        return responseBody;
    }
}
