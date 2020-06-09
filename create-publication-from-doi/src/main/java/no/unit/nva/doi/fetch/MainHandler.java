package no.unit.nva.doi.fetch;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestStreamHandler;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import no.unit.nva.api.PublicationResponse;
import no.unit.nva.doi.fetch.exceptions.InsertPublicationException;
import no.unit.nva.doi.fetch.exceptions.MalformedRequestException;
import no.unit.nva.doi.fetch.exceptions.MetadataNotFoundException;
import no.unit.nva.doi.fetch.exceptions.NoContentLocationFoundException;
import no.unit.nva.doi.fetch.exceptions.TransformFailedException;
import no.unit.nva.doi.fetch.model.DoiProxyResponse;
import no.unit.nva.doi.fetch.model.RequestBody;
import no.unit.nva.doi.fetch.model.Summary;
import no.unit.nva.doi.fetch.service.DoiProxyService;
import no.unit.nva.doi.fetch.service.PublicationConverter;
import no.unit.nva.doi.fetch.service.PublicationPersistenceService;
import no.unit.nva.doi.fetch.utils.JacocoGenerated;
import no.unit.nva.doi.transformer.PublicationTransformer;
import no.unit.nva.doi.transformer.exception.MissingClaimException;
import no.unit.nva.model.Publication;
import no.unit.nva.model.exceptions.InvalidIssnException;
import no.unit.nva.model.exceptions.InvalidPageTypeException;
import org.zalando.problem.Problem;
import org.zalando.problem.Status;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URISyntaxException;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import static org.apache.http.HttpHeaders.CONTENT_TYPE;
import static org.apache.http.HttpStatus.SC_BAD_REQUEST;
import static org.apache.http.HttpStatus.SC_OK;
import static org.apache.http.entity.ContentType.APPLICATION_JSON;
import static org.zalando.problem.Status.BAD_GATEWAY;
import static org.zalando.problem.Status.BAD_REQUEST;

public class MainHandler implements RequestStreamHandler {

    public static final String ACCESS_CONTROL_ALLOW_ORIGIN = "Access-Control-Allow-Origin";
    public static final String ALLOWED_ORIGIN_ENV = "ALLOWED_ORIGIN";
    public static final String API_HOST_ENV = "API_HOST";
    public static final String API_SCHEME_ENV = "API_SCHEME";
    public static final String HEADERS_AUTHORIZATION = "/headers/Authorization";
    public static final String BODY = "body";
    public static final String MISSING_HEADER = "Missing header:";
    private static final String APPLICATION_PROBLEM_JSON = "application/problem+json";

    private final transient ObjectMapper objectMapper;
    private final transient PublicationConverter publicationConverter;
    private final transient LocalDoiTransformService doiTransformService;
    private final transient DoiProxyService doiProxyService;
    private final transient PublicationPersistenceService publicationPersistenceService;
    private final transient String allowedOrigin;
    private final transient String apiHost;
    private final transient String apiScheme;
    public static final ObjectMapper jsonParser = ObjectMapperConfig.createObjectMapper();

    private static final Map<String, Status> EXCEPTIONS_TO_STATUSES;

    static {
        EXCEPTIONS_TO_STATUSES = Collections.unmodifiableMap(createExceptionMap());
    }

    @JacocoGenerated
    public MainHandler() {
        this(jsonParser, new PublicationConverter(), new LocalDoiTransformService(new PublicationTransformer()),
            new DoiProxyService(), new PublicationPersistenceService(), new Environment());
    }

    /**
     * Constructor for MainHandler.
     *
     * @param objectMapper objectMapper.
     * @param environment  environment.
     */
    public MainHandler(ObjectMapper objectMapper, PublicationConverter publicationConverter,
                       LocalDoiTransformService doiTransformService, DoiProxyService doiProxyService,
                       PublicationPersistenceService publicationPersistenceService, Environment environment) {
        this.objectMapper = objectMapper;
        this.publicationConverter = publicationConverter;
        this.doiTransformService = doiTransformService;
        this.doiProxyService = doiProxyService;
        this.publicationPersistenceService = publicationPersistenceService;
        this.allowedOrigin = environment.get(ALLOWED_ORIGIN_ENV);
        this.apiHost = environment.get(API_HOST_ENV);
        this.apiScheme = environment.get(API_SCHEME_ENV);
    }

    @Override
    public void handleRequest(InputStream input, OutputStream outputStream, Context context) throws IOException {
        RequestBody requestBody;
        String authorization;
        JsonNode event;
        try {
            event = objectMapper.readTree(input);
            authorization = extractAuthorization(event);
            requestBody = extractRequestBody(event);
        } catch (Exception e) {
            log(e.getMessage());
            objectMapper.writeValue(outputStream,
                new GatewayResponse<>(objectMapper.writeValueAsString(Problem.valueOf(BAD_REQUEST, e.getMessage())),
                    failureHeaders(), SC_BAD_REQUEST));
            return;
        }

        try {
            String apiUrl = String.join("://", apiScheme, apiHost);
            Publication publication = getPublicationMetadata(requestBody, authorization, apiUrl, event);

            PublicationResponse publicationResponse = tryInsertPublication(authorization, apiUrl, publication);

            Summary summary = publicationConverter
                .toSummary(objectMapper.convertValue(publicationResponse, JsonNode.class));

            writeOutput(outputStream, summary);
        } catch (NoContentLocationFoundException
                | InsertPublicationException
                | MissingClaimException
                | MalformedRequestException
                | InterruptedException
                | MetadataNotFoundException
                | URISyntaxException
                | RuntimeException
                | InvalidIssnException
                | InvalidPageTypeException e
        ) {
            writeFailure(outputStream, e);
        }
    }

    private Publication getPublicationMetadata(RequestBody requestBody, String authorization, String apiUrl,
                                            JsonNode lambdaEvent)
            throws NoContentLocationFoundException, URISyntaxException, MetadataNotFoundException, IOException,
            InterruptedException, MalformedRequestException, MissingClaimException, InvalidIssnException,
            InvalidPageTypeException {

        DoiProxyResponse externalModel = doiProxyService.lookup(requestBody.getDoiUrl(), apiUrl, authorization);

        return doiTransformService.transformLocally(externalModel, lambdaEvent);
    }

    private PublicationResponse tryInsertPublication(String authorization, String apiUrl, Publication publication)
        throws InterruptedException, IOException, InsertPublicationException, URISyntaxException {
        return insertPublication(authorization, apiUrl, publication);
    }

    private RequestBody extractRequestBody(JsonNode event) throws com.fasterxml.jackson.core.JsonProcessingException {
        RequestBody requestBody;
        String body = event.get(BODY).textValue();
        requestBody = objectMapper.readValue(body, RequestBody.class);
        return requestBody;
    }

    private String extractAuthorization(JsonNode event) {
        return Optional.ofNullable(event.at(HEADERS_AUTHORIZATION).textValue())
                       .orElseThrow(() -> new IllegalArgumentException(MISSING_HEADER + HEADERS_AUTHORIZATION));
    }

    private PublicationResponse insertPublication(String authorization, String apiUrl, Publication publication)
        throws InterruptedException, InsertPublicationException, IOException, URISyntaxException {
        return publicationPersistenceService.insertPublication(publication, apiUrl, authorization);
    }

    private void writeOutput(OutputStream output, Summary summary) throws IOException {
        objectMapper.writeValue(output,
            new GatewayResponse<>(objectMapper.writeValueAsString(summary), successHeaders(), SC_OK));
    }

    private void writeFailure(OutputStream outputStream, Exception exception) throws IOException {
        if (errorIsKnown(exception)) {
            Status errorStatus = getHttpStatus(exception);
            log(exception.getMessage());
            reportError(outputStream, exception, errorStatus);
        } else {
            reportUnkonwnError(outputStream, exception);
        }
    }

    private void reportUnkonwnError(OutputStream outputStream, Exception exception) throws IOException {
        reportError(outputStream, exception, Status.INTERNAL_SERVER_ERROR);
    }

    private Status getHttpStatus(Exception exception) {
        return EXCEPTIONS_TO_STATUSES.get(exception.getClass().getName());
    }

    private boolean errorIsKnown(Exception exception) {
        return EXCEPTIONS_TO_STATUSES.containsKey(exception.getClass().getName());
    }

    private void reportError(OutputStream outputStream, Exception exception, Status errorStatus) throws IOException {
        objectMapper.writeValue(outputStream,
            new GatewayResponse<>(objectMapper.writeValueAsString(Problem.valueOf(errorStatus, exception.getMessage())),
                failureHeaders(), errorStatus.getStatusCode()));
    }

    private Map<String, String> successHeaders() {
        Map<String, String> headers = new ConcurrentHashMap<>();
        headers.put(ACCESS_CONTROL_ALLOW_ORIGIN, allowedOrigin);
        headers.put(CONTENT_TYPE, APPLICATION_JSON.getMimeType());
        return headers;
    }

    private Map<String, String> failureHeaders() {
        Map<String, String> headers = new ConcurrentHashMap<>();
        headers.put(ACCESS_CONTROL_ALLOW_ORIGIN, allowedOrigin);
        headers.put(CONTENT_TYPE, APPLICATION_PROBLEM_JSON);
        return headers;
    }

    public static Map<String, Status> createExceptionMap() {
        Map<String, Status> exceptionMap = new ConcurrentHashMap<>();
        exceptionMap.put(MetadataNotFoundException.class.getName(), BAD_GATEWAY);
        exceptionMap.put(NoContentLocationFoundException.class.getName(), BAD_GATEWAY);
        exceptionMap.put(TransformFailedException.class.getName(), BAD_GATEWAY);
        exceptionMap.put(InsertPublicationException.class.getName(), BAD_GATEWAY);
        exceptionMap.put(PublicationPersistenceService.class.getName(),BAD_GATEWAY);
        exceptionMap.put(MalformedRequestException.class.getName(), BAD_REQUEST);
        exceptionMap.put(MissingClaimException.class.getName(), BAD_REQUEST);
        return exceptionMap;
    }

    public static void log(String message) {
        System.out.println(message);
    }
}
