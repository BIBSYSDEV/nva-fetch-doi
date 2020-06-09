package no.unit.nva.doi;

import static javax.ws.rs.core.Response.Status.CREATED;
import static no.unit.nva.doi.GatewayResponse.errorGatewayResponse;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.fasterxml.jackson.databind.ObjectMapper;
import no.unit.nva.doi.Config;
import no.unit.nva.doi.GatewayResponse;
import no.unit.nva.doi.fetch.ObjectMapperConfig;
import org.junit.Test;

public class GatewayResponseTest {

    private static final String EMPTY_STRING = "";

    public static final String CORS_HEADER = "CORS header";
    public static final String MOCK_BODY = "mock";
    public static final String ERROR_BODY = "error";
    public static final String ERROR_JSON = "{\"error\":\"error\"}";

    private ObjectMapper objectMapper = ObjectMapperConfig.createObjectMapper();

    @Test
    public void testErrorResponse() {
        String expectedJson = ERROR_JSON;
        // calling real constructor (no need to mock as this is not talking to the internet)
        // but helps code coverage
        GatewayResponse gatewayResponse = errorGatewayResponse(ERROR_BODY, CREATED.getStatusCode(),objectMapper);
        assertEquals(expectedJson, gatewayResponse.getBody());
    }

    @Test
    public void testNoCorsHeaders() {
        final Config config = Config.getInstance();
        config.setCorsHeader(EMPTY_STRING);
        final String corsHeader = config.getCorsHeader();
        GatewayResponse gatewayResponse = errorGatewayResponse(MOCK_BODY, CREATED.getStatusCode(), objectMapper);
        assertFalse(
            gatewayResponse.getHeaders().containsKey(GatewayResponse.CORS_ALLOW_ORIGIN_HEADER));
        assertFalse(gatewayResponse.getHeaders().containsValue(corsHeader));

        config.setCorsHeader(CORS_HEADER);
        GatewayResponse gatewayResponse1 = errorGatewayResponse(MOCK_BODY, CREATED.getStatusCode(), objectMapper);
        assertTrue(
            gatewayResponse1.getHeaders().containsKey(GatewayResponse.CORS_ALLOW_ORIGIN_HEADER));
    }
}
