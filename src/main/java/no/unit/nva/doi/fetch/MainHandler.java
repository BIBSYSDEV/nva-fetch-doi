package no.unit.nva.doi.fetch;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestStreamHandler;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import no.unit.nva.doi.fetch.model.RequestBody;
import no.unit.nva.doi.fetch.model.Summary;
import no.unit.nva.doi.fetch.service.DoiProxyService;
import no.unit.nva.doi.fetch.service.DoiTransformService;
import no.unit.nva.doi.fetch.service.PublicationConverter;
import org.zalando.problem.Problem;
import org.zalando.problem.ProblemModule;

import javax.ws.rs.ProcessingException;
import javax.ws.rs.WebApplicationException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import static org.apache.http.HttpHeaders.CONTENT_TYPE;
import static org.apache.http.HttpStatus.SC_BAD_GATEWAY;
import static org.apache.http.HttpStatus.SC_BAD_REQUEST;
import static org.apache.http.HttpStatus.SC_INTERNAL_SERVER_ERROR;
import static org.apache.http.HttpStatus.SC_OK;
import static org.apache.http.entity.ContentType.APPLICATION_JSON;
import static org.zalando.problem.Status.BAD_REQUEST;
import static org.zalando.problem.Status.INTERNAL_SERVER_ERROR;

public class MainHandler implements RequestStreamHandler {

    public static final String ACCESS_CONTROL_ALLOW_ORIGIN = "Access-Control-Allow-Origin";
    public static final String ALLOWED_ORIGIN_ENV = "ALLOWED_ORIGIN";
    public static final String API_HOST_ENV = "API_HOST";

    private final transient ObjectMapper objectMapper;
    private final transient PublicationConverter publicationConverter;
    private final transient DoiTransformService doiTransformService;
    private final transient DoiProxyService doiProxyService;
    private final transient String allowedOrigin;
    private final transient String apiHost;

    public MainHandler() {
        this(createObjectMapper(), new PublicationConverter(), new DoiTransformService(), new DoiProxyService(),
                new Environment());
    }

    /**
     * Constructor for MainHandler.
     *
     * @param objectMapper  objectMapper
     * @param environment   environment
     */
    public MainHandler(ObjectMapper objectMapper, PublicationConverter publicationConverter,
                       DoiTransformService doiTransformService, DoiProxyService doiProxyService,
                       Environment environment) {
        this.objectMapper = objectMapper;
        this.publicationConverter = publicationConverter;
        this.doiTransformService = doiTransformService;
        this.doiProxyService = doiProxyService;
        this.allowedOrigin = environment.get(ALLOWED_ORIGIN_ENV).orElseThrow(IllegalStateException::new);
        this.apiHost = environment.get(API_HOST_ENV).orElseThrow(IllegalStateException::new);
    }

    @Override
    public void handleRequest(InputStream input, OutputStream output, Context context) throws IOException {
        RequestBody requestBody;
        String authorization;
        try {
            JsonNode event = objectMapper.readTree(input);
            authorization = Optional.ofNullable(event.at("/headers/Authorization").textValue())
                    .orElseThrow(IllegalArgumentException::new);
            String body = event.get("body").textValue();
            requestBody = objectMapper.readValue(body, RequestBody.class);
        } catch (Exception e) {
            System.out.println(e.getMessage());
            objectMapper.writeValue(output, new GatewayResponse<>(objectMapper.writeValueAsString(
                    Problem.valueOf(BAD_REQUEST, e.getMessage())), headers(), SC_BAD_REQUEST));
            return;
        }

        try {
            JsonNode dataciteData = doiProxyService.lookup(requestBody.getDoiUrl(), apiHost, authorization);
            JsonNode publication = doiTransformService.transform(dataciteData, apiHost, authorization);
            UUID identifier = UUID.randomUUID();
            Summary summary = publicationConverter.toSummary(publication, identifier);
            objectMapper.writeValue(output, new GatewayResponse<>(
                    objectMapper.writeValueAsString(summary), headers(), SC_OK));
        } catch (ProcessingException | WebApplicationException e) {
            System.out.println(e.getMessage());
            objectMapper.writeValue(output, new GatewayResponse<>(objectMapper.writeValueAsString(
                    Problem.valueOf(INTERNAL_SERVER_ERROR, e.getMessage())), headers(), SC_BAD_GATEWAY));
        } catch (Exception e) {
            System.out.println(e.getMessage());
            objectMapper.writeValue(output, new GatewayResponse<>(objectMapper.writeValueAsString(
                    Problem.valueOf(INTERNAL_SERVER_ERROR, e.getMessage())), headers(), SC_INTERNAL_SERVER_ERROR));
        }
    }

    private Map<String,String> headers() {
        Map<String,String> headers = new ConcurrentHashMap<>();
        headers.put(ACCESS_CONTROL_ALLOW_ORIGIN, allowedOrigin);
        headers.put(CONTENT_TYPE, APPLICATION_JSON.getMimeType());
        return headers;
    }

    /**
     * Create ObjectMapper.
     *
     * @return  objectMapper
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


}
