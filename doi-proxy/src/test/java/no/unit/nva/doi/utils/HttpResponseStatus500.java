package no.unit.nva.doi.utils;

import java.net.HttpURLConnection;

public class HttpResponseStatus500<S> extends AbstractHttpResponse<S> {

    public HttpResponseStatus500(S responseBody) {
        super(responseBody);
    }

    @Override
    public int statusCode() {
        return HttpURLConnection.HTTP_INTERNAL_ERROR;
    }

    @Override
    public S body() {
        return responseBody;
    }
}
