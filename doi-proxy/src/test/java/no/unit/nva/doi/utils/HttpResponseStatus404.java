package no.unit.nva.doi.utils;

import java.net.HttpURLConnection;

public class HttpResponseStatus404<S> extends AbstractHttpResponse<S> {

    public HttpResponseStatus404(S responseBody) {
        super(responseBody);
    }

    @Override
    public int statusCode() {
        return HttpURLConnection.HTTP_NOT_FOUND;
    }

    @Override
    public S body() {
        return responseBody;
    }
}
