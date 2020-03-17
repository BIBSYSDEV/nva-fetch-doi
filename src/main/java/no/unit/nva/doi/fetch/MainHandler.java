package no.unit.nva.doi.fetch;

import static org.apache.http.HttpHeaders.CONTENT_TYPE;
import static org.apache.http.HttpStatus.SC_BAD_GATEWAY;
import static org.apache.http.HttpStatus.SC_BAD_REQUEST;
import static org.apache.http.HttpStatus.SC_INTERNAL_SERVER_ERROR;
import static org.apache.http.HttpStatus.SC_OK;
import static org.apache.http.entity.ContentType.APPLICATION_JSON;
import static org.zalando.problem.Status.BAD_GATEWAY;
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
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import javax.ws.rs.ProcessingException;
import javax.ws.rs.WebApplicationException;
import no.unit.nva.doi.fetch.model.RequestBody;
import no.unit.nva.doi.fetch.model.Summary;
import no.unit.nva.doi.fetch.service.DoiProxyService;
import no.unit.nva.doi.fetch.service.DoiTransformService;
import no.unit.nva.doi.fetch.service.PublicationConverter;
import no.unit.nva.doi.fetch.service.ResourcePersistenceService;
import org.zalando.problem.Problem;
import org.zalando.problem.ProblemModule;

public class MainHandler implements RequestStreamHandler {

    public static final String ACCESS_CONTROL_ALLOW_ORIGIN = "Access-Control-Allow-Origin";
    public static final String ALLOWED_ORIGIN_ENV = "ALLOWED_ORIGIN";
    public static final String API_HOST_ENV = "API_HOST";
    public static final String API_SCHEME_ENV = "API_SCHEME";
    public static final String HEADERS_AUTHORIZATION = "/headers/Authorization";
    public static final String BODY = "body";

    private final transient ObjectMapper objectMapper;
    private final transient PublicationConverter publicationConverter;
    private final transient DoiTransformService doiTransformService;
    private final transient DoiProxyService doiProxyService;
    private final transient ResourcePersistenceService resourcePersistenceService;
    private final transient String allowedOrigin;
    private final transient String apiHost;
    private final transient String apiScheme;

    public MainHandler() {
        this(createObjectMapper(), new PublicationConverter(), new DoiTransformService(), new DoiProxyService(),
             new ResourcePersistenceService(), new Environment());
    }

    /**
     * Constructor for MainHandler.
     *
     * @param objectMapper objectMapper
     * @param environment  environment
     */
    public MainHandler(ObjectMapper objectMapper, PublicationConverter publicationConverter,
                       DoiTransformService doiTransformService, DoiProxyService doiProxyService,
                       ResourcePersistenceService resourcePersistenceService,
                       Environment environment) {
        this.objectMapper = objectMapper;
        this.publicationConverter = publicationConverter;
        this.doiTransformService = doiTransformService;
        this.doiProxyService = doiProxyService;
        this.resourcePersistenceService = resourcePersistenceService;
        this.allowedOrigin = environment.get(ALLOWED_ORIGIN_ENV).orElseThrow(IllegalStateException::new);
        this.apiHost = environment.get(API_HOST_ENV).orElseThrow(IllegalStateException::new);
        this.apiScheme = environment.get(API_SCHEME_ENV).orElseThrow(IllegalStateException::new);
    }

    @Override
    public void handleRequest(InputStream input, OutputStream output, Context context) throws IOException {
        RequestBody requestBody;
        String authorization;
        try {
            JsonNode event = objectMapper.readTree(input);
            authorization = Optional.ofNullable(event.at(HEADERS_AUTHORIZATION).textValue())
                                    .orElseThrow(IllegalArgumentException::new);
            String body = event.get(BODY).textValue();
            requestBody = objectMapper.readValue(body, RequestBody.class);
        } catch (Exception e) {
            log(e.getMessage());
            objectMapper.writeValue(output, new GatewayResponse<>(objectMapper.writeValueAsString(
                Problem.valueOf(BAD_REQUEST, e.getMessage())), headers(), SC_BAD_REQUEST));
            return;
        }

        try {
            String apiUrl = String.join("://", apiScheme, apiHost);

            Optional<JsonNode> publication =
                getPublicationMetadata(requestBody, authorization, apiUrl);

            publication.ifPresent(p ->
                                      resourcePersistenceService.insertPublication(p, apiUrl, authorization));

            Optional<Summary> summary = publication.map(publicationConverter::toSummary);
            writeSummaryToOutput(output, summary);
        } catch (ProcessingException | WebApplicationException e) {
            log(e.getMessage());
            objectMapper.writeValue(output, new GatewayResponse<>(objectMapper.writeValueAsString(
                Problem.valueOf(BAD_GATEWAY, e.getMessage())), headers(), SC_BAD_GATEWAY));
        } catch (Exception e) {
            log(e.getMessage());
            objectMapper.writeValue(output, new GatewayResponse<>(objectMapper.writeValueAsString(
                Problem.valueOf(INTERNAL_SERVER_ERROR, e.getMessage())), headers(), SC_INTERNAL_SERVER_ERROR));
        }
    }

    private void writeSummaryToOutput(OutputStream output, Optional<Summary> summary) throws IOException {
        if (summary.isPresent()) {
            Summary s = summary.get();
            writeOutput(output, s);
        }
    }

    private void writeOutput(OutputStream output, Summary summary) throws IOException {
        objectMapper.writeValue(output, new GatewayResponse<>(
            objectMapper.writeValueAsString(summary), headers(), SC_OK));
    }

    private Optional<JsonNode> getPublicationMetadata(RequestBody requestBody, String authorization,
                                                      String apiUrl) {
        return doiProxyService.lookup(requestBody.getDoiUrl(), apiUrl, authorization)
                              .map(response -> doiTransformService.transform(response, apiUrl, authorization));
    }

    private Map<String, String> headers() {
        Map<String, String> headers = new ConcurrentHashMap<>();
        headers.put(ACCESS_CONTROL_ALLOW_ORIGIN, allowedOrigin);
        headers.put(CONTENT_TYPE, APPLICATION_JSON.getMimeType());
        return headers;
    }

    /**
     * Create ObjectMapper.
     *
     * @return objectMapper
     */
    public static ObjectMapper createObjectMapper() {
        return new ObjectMapper()
            .registerModule(new ProblemModule())
            .registerModule(new JavaTimeModule())
            .configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)
            .configure(SerializationFeature.INDENT_OUTPUT, true)
            .setSerializationInclusion(JsonInclude.Include.NON_NULL)
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    public static void log(String message) {
        System.out.println(message);
    }
}
