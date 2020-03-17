package no.unit.nva.doi.fetch.service;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsMapContaining.hasKey;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Map;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.client.Invocation.Builder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Response;
import no.unit.nva.doi.fetch.service.exceptions.NoContentLocationFoundException;
import no.unit.nva.doi.fetch.service.utils.MockResponse;
import org.junit.Test;
import org.junit.jupiter.api.DisplayName;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

public class DoiProxyServiceTest {

    public static final String EXPECTED_FIELD_IN_GET_BODY = "doi";

    @Test
    public void lookupShouldNotThrowExceptionForWellformedRequest()
        throws IOException, NoContentLocationFoundException {
        Client client = mock(Client.class);
        new CallChain(client);
        DoiProxyService doiProxyService = new DoiProxyService(client);
        doiProxyService.lookup(new URL("http://example.org"), "http://example.org", "some api key");
    }

    @Test
    @DisplayName("lookup sends a query with a json object containing the field 'doi' ")
    public void lookupShouldSendAQueryWithAJsonObjectWththeFieldDoi()
        throws MalformedURLException, NoContentLocationFoundException {
        Client client = mock(Client.class);
        CallChain callChain = new CallChain(client);
        DoiProxyService doiProxyService = new DoiProxyService(client);
        doiProxyService.lookup(new URL("http://example.org"), "http://example.org", "some api key");
        assertThat(callChain.requestBody, hasKey(EXPECTED_FIELD_IN_GET_BODY));
    }

    // Contains all mocks that are necessary for the Client to be mocked
    private class CallChain {

        private final Builder mockInvocationBuilder;
        private final WebTarget mockWebTarget;
        private final Client client;
        private Invocation invocation;
        private Map<String, URL> requestBody;

        public CallChain(Client client) {
            this.client = client;
            this.mockInvocationBuilder = mock(Builder.class);
            this.mockWebTarget = mock(WebTarget.class);
            initialize();
        }

        public void initialize() {
            this.invocation = mockRequestInvocation();

            when(client.target(anyString())).thenReturn(mockWebTarget);

            when(mockWebTarget.path(anyString())).thenReturn(mockWebTarget);
            when(mockWebTarget.request(anyString())).thenReturn(mockInvocationBuilder);

            when(mockInvocationBuilder.header(anyString(), anyString())).thenReturn(mockInvocationBuilder);
            when(mockInvocationBuilder.buildPost(any())).thenAnswer(new Answer<Invocation>() {
                @Override
                public Invocation answer(InvocationOnMock args) throws Throwable {
                    cachePostRequestBody(args);
                    return invocation;
                }
            });
        }

        private void cachePostRequestBody(InvocationOnMock args) {
            Entity<Map<String, URL>> entity = args.getArgument(0);
            requestBody = entity.getEntity();
        }

        private Invocation mockRequestInvocation() {
            Invocation invocation = mock(Invocation.class);
            Response response = new MockResponse();
            when(invocation.invoke()).thenReturn(response);
            return invocation;
        }
    }
}
