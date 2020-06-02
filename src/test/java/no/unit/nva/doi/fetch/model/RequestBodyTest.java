package no.unit.nva.doi.fetch.model;


import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.net.MalformedURLException;
import java.net.URL;
import org.junit.jupiter.api.Test;

public class RequestBodyTest {

    @Test
    public void test() throws MalformedURLException {
        RequestBody requestBody = new RequestBody();
        requestBody.setDoiUrl(new URL("http://example.org"));
        assertNotNull(requestBody.getDoiUrl());
    }
}
