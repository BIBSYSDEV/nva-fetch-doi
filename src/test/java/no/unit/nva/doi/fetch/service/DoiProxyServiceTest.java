package no.unit.nva.doi.fetch.service;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.JsonNode;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.http.HttpClient;
import java.net.http.HttpHeaders;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiPredicate;
import no.unit.nva.doi.fetch.exceptions.MetadataNotFoundException;
import no.unit.nva.doi.fetch.exceptions.NoContentLocationFoundException;
import no.unit.nva.doi.fetch.service.utils.RequestBodyReader;
import org.apache.http.HttpStatus;
import org.junit.Test;
import org.junit.jupiter.api.DisplayName;
import org.mockito.invocation.InvocationOnMock;

public class DoiProxyServiceTest {

    public static final String EXPECTED_FIELD_IN_POST_BODY = "doi";
    public static final String SOME_CONTENT_LOCATION = "SomeContentLocation";
    public static final BiPredicate<String, String> INCLUDE_ALL = (l, r) -> true;

    @Test
    public void lookupShouldNotThrowExceptionForWellformedRequest()
        throws IOException, NoContentLocationFoundException, URISyntaxException, InterruptedException,
        MetadataNotFoundException {
        HttpClient client = mock(HttpClient.class);
        when(client.send(any(), any())).thenAnswer(this::responseEchoingRequestBody);
        DoiProxyService doiProxyService = new DoiProxyService(client);
        doiProxyService.lookup(new URL("http://example.org"), "http://example.org", "some api key");
    }

    @Test
    @DisplayName("lookup sends a query with a json object containing the field 'doi' ")
    public void lookupShouldSendAQueryWithAJsonObjectWthTheFieldDoi()
        throws IOException, NoContentLocationFoundException, InterruptedException, URISyntaxException,
        MetadataNotFoundException {
        HttpClient client = mock(HttpClient.class);
        when(client.send(any(HttpRequest.class), any())).thenAnswer(this::responseEchoingRequestBody);

        DoiProxyService doiProxyService = new DoiProxyService(client);
        String exampleDoiValue = "http://doivalue.org";

        DoiProxyResponse result = doiProxyService
            .lookup(new URL(exampleDoiValue), "http://example.org", "some api key");

        JsonNode actualFieldValue = result.getJsonNode().findValue(EXPECTED_FIELD_IN_POST_BODY);
        assertNotNull(actualFieldValue);
        assertThat(actualFieldValue.asText(), is(equalTo(exampleDoiValue)));
    }

    private HttpResponse<String> responseEchoingRequestBody(InvocationOnMock invocation) {
        HttpRequest request = invocation.getArgument(0);
        String body = RequestBodyReader.requestBody(request);
        return mockResponse(body);
    }

    private HttpResponse<String> mockResponse(String body) {
        HttpResponse<String> response = mock(HttpResponse.class);
        when(response.statusCode()).thenReturn(HttpStatus.SC_OK);
        when(response.body()).thenReturn(body);
        when(response.headers()).thenReturn(mockHttpHeaders());
        return response;
    }

    private HttpHeaders mockHttpHeaders() {
        Map<String, List<String>> headersMap = new HashMap<>();
        headersMap.put(org.apache.http.HttpHeaders.CONTENT_LOCATION, Collections.singletonList(SOME_CONTENT_LOCATION));
        return HttpHeaders.of(headersMap, INCLUDE_ALL);
    }
}
