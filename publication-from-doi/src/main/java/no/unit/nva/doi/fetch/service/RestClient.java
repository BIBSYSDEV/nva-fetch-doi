package no.unit.nva.doi.fetch.service;

import java.net.HttpURLConnection;
import java.net.http.HttpResponse;

public class RestClient {

    protected boolean responseIsSuccessful(HttpResponse<String> response) {
        int status = response.statusCode();
        // status should be in the range [200,300)
        return status >= HttpURLConnection.HTTP_OK && status < HttpURLConnection.HTTP_MULT_CHOICE;
    }
}
