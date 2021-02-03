package no.unit.nva.doi.transformer.utils;


import java.net.HttpURLConnection;

public class HttpResponseStatus200<S> extends AbstractHttpResponse<S> {

    public HttpResponseStatus200(S responseBody) {
        super(responseBody);
    }

    @Override
    public int statusCode() {
        return HttpURLConnection.HTTP_OK;
    }

    @Override
    public S body() {
        return responseBody;
    }
}
