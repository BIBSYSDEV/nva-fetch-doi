package no.unit.nva.doi.fetch.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.client.WebTarget;
import no.unit.nva.doi.fetch.MainHandler;
import org.junit.Test;

public class ResourcePersistenceServiceTest {

    private ObjectMapper objectMapper = MainHandler.createObjectMapper();

    @Test
    public void test() {

        Client client = mock(Client.class);
        WebTarget webTarget = mock(WebTarget.class);
        when(client.target(anyString())).thenReturn(webTarget);
        when(webTarget.path(anyString())).thenReturn(webTarget);
        Invocation.Builder builder = mock(Invocation.Builder.class);
        when(webTarget.request(anyString())).thenReturn(builder);
        when(builder.header(anyString(), anyString())).thenReturn(builder);
        when(builder.post(any(), (Class<Object>) any())).thenReturn(null);

        ResourcePersistenceService resourcePersistenceService = new ResourcePersistenceService(client);

        resourcePersistenceService
            .insertPublication(objectMapper.createObjectNode(), "http://example.org", "some api key");
    }
}
