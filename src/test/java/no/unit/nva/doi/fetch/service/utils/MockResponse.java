package no.unit.nva.doi.fetch.service.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.node.ObjectNode;
import no.bibsys.aws.tools.JsonUtils;
import no.unit.nva.doi.fetch.MainHandler;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpStatus;

import javax.net.ssl.SSLSession;
import java.net.URI;
import java.net.http.HttpClient.Version;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class MockResponse implements HttpResponse {

    public static final String AuthorizationHeaderValue = "AuthorizationHeader";
    public static final String ContentLocationHeaderValue = "ContentLocationHeader";
    public static final ObjectNode mockJsonObject = JsonUtils.jsonParser.createObjectNode();

    @Override
    public int statusCode() {
        return HttpStatus.SC_OK;
    }

    @Override
    public HttpRequest request() {
        return null;
    }

    @Override
    public Optional<HttpResponse> previousResponse() {
        return Optional.empty();
    }

    @Override
    public java.net.http.HttpHeaders headers() {
        Map<String, List<String>> map = new HashMap<>();
        map.put(HttpHeaders.AUTHORIZATION, Collections.singletonList(AuthorizationHeaderValue));
        map.put(HttpHeaders.CONTENT_LOCATION, Collections.singletonList(ContentLocationHeaderValue));
        return java.net.http.HttpHeaders.of(map, (header, value) -> true);
    }

    @Override
    public Object body() {
        try {
            return MainHandler.jsonParser.writeValueAsString(mockJsonObject);
        } catch (JsonProcessingException e) {
            return "";
        }
    }

    @Override
    public Optional<SSLSession> sslSession() {
        return Optional.empty();
    }

    @Override
    public URI uri() {
        return null;
    }

    @Override
    public Version version() {
        return null;
    }
}
