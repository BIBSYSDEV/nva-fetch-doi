package no.unit.nva.doi.fetch.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import no.unit.nva.doi.fetch.MainHandler;
import org.junit.Test;

public class DoiTransformServiceTest {

    private ObjectMapper objectMapper = MainHandler.createObjectMapper();

    //TODO: fix this test
    @Test
    public void test() {

//        Client client = mock(Client.class);
//        WebTarget webTarget = mock(WebTarget.class);
//        when(client.target(anyString())).thenReturn(webTarget);
//        when(webTarget.path(anyString())).thenReturn(webTarget);
//        Invocation.Builder builder = mock(Invocation.Builder.class);
//        when(webTarget.request(anyString())).thenReturn(builder);
//        when(builder.header(anyString(), anyString())).thenReturn(builder);
//        when(builder.post(any(), (Class<Object>) any())).thenReturn(null);
//
//        DoiTransformService doiTransformService = new DoiTransformService(client);
//        JsonNode sampleJson = objectMapper.createObjectNode();
//        String someContentLocation = "www.somecontentlocation.org";
//        DoiProxyResponse doiProxyResponse = new DoiProxyResponse(sampleJson, someContentLocation);
//        doiTransformService.transform(doiProxyResponse, "http://example.org", "some api key");
    }
}
