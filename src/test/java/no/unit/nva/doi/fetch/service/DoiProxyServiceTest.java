package no.unit.nva.doi.fetch.service;

import org.junit.Test;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.client.WebTarget;
import java.io.IOException;
import java.net.URL;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class DoiProxyServiceTest {

    @Test
    public void test() throws IOException {

        Client client = mock(Client.class);
        WebTarget webTarget = mock(WebTarget.class);
        when(client.target(anyString())).thenReturn(webTarget);
        when(webTarget.path(anyString())).thenReturn(webTarget);
        Invocation.Builder builder = mock(Invocation.Builder.class);
        when(webTarget.request(anyString())).thenReturn(builder);
        when(builder.header(anyString(),anyString())).thenReturn(builder);
        when(builder.post(any(), (Class<Object>) any())).thenReturn(null);

        DoiProxyService doiProxyService = new DoiProxyService(client);

        doiProxyService.lookup(new URL("http://example.org"), "http://example.org", "some api key");
    }

}
