package no.unit.nva.doi;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import org.apache.commons.lang3.StringUtils;

/**
 * POJO containing response object for API Gateway.
 */
public class GatewayResponse {

    public static final String CORS_ALLOW_ORIGIN_HEADER = "Access-Control-Allow-Origin";
    public static final String ERROR_KEY = "error";
    private final String body;
    private final transient Map<String, String> customHeaders;
    private transient Map<String, String> headers;
    private final int statusCode;

    /**
     * GatewayResponse constructor.
     */
    public GatewayResponse(final String body, final int status, String contentType) {
        this(body, status, contentType, Collections.EMPTY_MAP);
    }

    /**
     * Constructor that allows to add some custom headers in the response.
     */
    public GatewayResponse(final String body, final int status, String contentType, Map<String, String> customHeaders) {
        this.statusCode = status;
        this.body = body;
        this.customHeaders = customHeaders;
        generateHeaders(contentType);
    }

    public String getBody() {
        return body;
    }

    public Map<String, String> getHeaders() {
        return headers;
    }

    public int getStatusCode() {
        return statusCode;
    }

    private void generateHeaders(String contentType) {
        Map<String, String> headers = new ConcurrentHashMap<>();
        headers.put(HttpHeaders.CONTENT_TYPE, contentType);
        headers.putAll(customHeaders);

        final String corsAllowDomain = Config.getInstance().getCorsHeader();
        if (StringUtils.isNotEmpty(corsAllowDomain)) {
            headers.put(CORS_ALLOW_ORIGIN_HEADER, corsAllowDomain);
        }
        this.headers = Collections.unmodifiableMap(new HashMap<>(headers));
    }

    /**
     * Create error GatewayResponse.
     *
     * @param message    message
     * @param statusCode statusCode
     * @return GatewayResponse
     */
    @SuppressWarnings("PMD.UseConcurrentHashMap")
    public static GatewayResponse errorGatewayResponse(String message, int statusCode, ObjectMapper objectMapper) {
        String errorMessage = null;
        try {
            Map errorMap = new HashMap();
            errorMap.put(ERROR_KEY, message);
            errorMessage = objectMapper.writeValueAsString(errorMap);
        } catch (JsonProcessingException e) {
            System.out.println(e.getMessage());
        }
        return new GatewayResponse(errorMessage, statusCode, MediaType.APPLICATION_JSON);
    }
}
