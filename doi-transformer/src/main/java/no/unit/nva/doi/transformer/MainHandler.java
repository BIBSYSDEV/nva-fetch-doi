package no.unit.nva.doi.transformer;

import static org.apache.http.HttpHeaders.CONTENT_LOCATION;
import static org.apache.http.HttpHeaders.CONTENT_TYPE;
import static org.apache.http.HttpStatus.SC_BAD_REQUEST;
import static org.apache.http.HttpStatus.SC_INTERNAL_SERVER_ERROR;
import static org.apache.http.HttpStatus.SC_OK;
import static org.zalando.problem.Status.BAD_REQUEST;
import static org.zalando.problem.Status.INTERNAL_SERVER_ERROR;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestStreamHandler;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import no.unit.nva.model.Publication;
import org.apache.http.entity.ContentType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zalando.problem.Problem;
import org.zalando.problem.ProblemModule;

public class MainHandler implements RequestStreamHandler {

    public static final String ACCESS_CONTROL_ALLOW_ORIGIN = "Access-Control-Allow-Origin";
    public static final String BODY = "body";
    public static final String HEADERS = "headers";

    public static final String ALLOWED_ORIGIN = "ALLOWED_ORIGIN";
    public static final String APPLICATION_PROBLEM = "application/problem+json";
    public static final String ENVIRONMENT_VARIABLE_NOT_SET = "Environment variable not set: ";

    public final transient ObjectMapper objectMapper;
    private final transient String allowedOrigin;
    private final PublicationTransformer publicationTransformer;

    private static final Logger logger = LoggerFactory.getLogger(MainHandler.class);

    @JacocoGenerated
    public MainHandler() {
        this(createObjectMapper(), new DataciteResponseConverter(),
            new CrossRefConverter(), new Environment());
    }

    /**
     * Non-default constructor.
     *
     * @param objectMapper      json mapper.
     * @param dataciteConverter datacite converter.
     * @param crossRefConverter crossref converter
     * @param environment       environment variables.
     */
    public MainHandler(ObjectMapper objectMapper, DataciteResponseConverter dataciteConverter,
                       CrossRefConverter crossRefConverter, Environment environment) {
        this.objectMapper = objectMapper;
        this.publicationTransformer = new PublicationTransformer(dataciteConverter, crossRefConverter,
                createObjectMapper());
        this.allowedOrigin = environment.get(ALLOWED_ORIGIN).orElseThrow(
            () -> new IllegalStateException(ENVIRONMENT_VARIABLE_NOT_SET + ALLOWED_ORIGIN));
    }

    @Override
    public void handleRequest(InputStream input, OutputStream output, Context context) throws IOException {
        JsonNode event;
        String body;
        String contentLocation;
        try {
            event = objectMapper.readTree(input);
            body = extractRequestBody(event);
            contentLocation = extractContentLocationHeader(event);
        } catch (Exception e) {
            e.printStackTrace();
            objectMapper.writeValue(output,
                    new GatewayResponse<>(objectMapper.writeValueAsString(Problem.valueOf(BAD_REQUEST, e.getMessage())),
                            failureResponseHeaders(), SC_BAD_REQUEST));
            return;
        }

        try {
            Publication publication = publicationTransformer.transformPublication(event, body, contentLocation);
            logger.info(objectMapper.writeValueAsString(publication));
            objectMapper.writeValue(output,
                new GatewayResponse<>(objectMapper.writeValueAsString(publication), sucessResponseHeaders(), SC_OK));
        } catch (Exception e) {
            e.printStackTrace();
            objectMapper.writeValue(output, new GatewayResponse<>(
                    objectMapper.writeValueAsString(Problem.valueOf(INTERNAL_SERVER_ERROR, e.getMessage())),
                    failureResponseHeaders(), SC_INTERNAL_SERVER_ERROR));
        }
    }

    private String extractRequestBody(JsonNode event) {
        JsonNode body = event.get(BODY);
        if (body.isValueNode()) {
            return body.asText();
        } else {
            return body.textValue();
        }
    }

    private String extractContentLocationHeader(JsonNode event) {
        JsonNode headers = requestHeaders(event);
        JsonNode contentLocation = headers.get(CONTENT_LOCATION);
        if (contentLocation.isArray()) {
            List<String> contentLocations = objectMapper.convertValue(contentLocation, List.class);
            if (!contentLocations.isEmpty()) {
                return contentLocations.get(0);
            }
        } else {
            return contentLocation.textValue();
        }
        return null;
    }

    private JsonNode requestHeaders(JsonNode root) {
        return root.get(HEADERS);
    }

    private Map<String, String> sucessResponseHeaders() {
        Map<String, String> headers = new ConcurrentHashMap<>();
        headers.put(ACCESS_CONTROL_ALLOW_ORIGIN, allowedOrigin);
        headers.put(CONTENT_TYPE, ContentType.APPLICATION_JSON.getMimeType());
        return headers;
    }

    private Map<String, String> failureResponseHeaders() {
        Map<String, String> headers = new ConcurrentHashMap<>();
        headers.put(ACCESS_CONTROL_ALLOW_ORIGIN, allowedOrigin);
        headers.put(CONTENT_TYPE, APPLICATION_PROBLEM);
        return headers;
    }

    /**
     * Create ObjectMapper.
     *
     * @return objectMapper
     */
    public static ObjectMapper createObjectMapper() {
        return new ObjectMapper().registerModule(new ProblemModule()).registerModule(new JavaTimeModule())
            .configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)
            .setSerializationInclusion(JsonInclude.Include.NON_NULL)
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
            .configure(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY, true);
    }
}
