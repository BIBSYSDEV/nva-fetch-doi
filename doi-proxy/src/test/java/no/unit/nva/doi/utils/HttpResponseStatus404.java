package no.unit.nva.doi.utils;

import org.apache.http.HttpStatus;

public class HttpResponseStatus404<S> extends AbstractHttpResponse<S> {

    public HttpResponseStatus404(S responseBody) {
        super(responseBody);
    }

    @Override
    public int statusCode() {
        return HttpStatus.SC_NOT_FOUND;
    }

    @Override
    public S body() {
        return responseBody;
    }
}
