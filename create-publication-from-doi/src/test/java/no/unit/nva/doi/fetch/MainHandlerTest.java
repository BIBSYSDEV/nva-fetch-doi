package no.unit.nva.doi.fetch;

import static no.unit.nva.doi.fetch.MainHandler.ALLOWED_ORIGIN_ENV;
import static no.unit.nva.doi.fetch.MainHandler.NULL_DOI_URL_ERROR;
import static no.unit.nva.doi.fetch.MainHandler.PUBLICATION_API_HOST_ENV;
import static no.unit.nva.doi.fetch.MainHandler.PUBLICATION_API_SCHEME_ENV;
import static nva.commons.utils.JsonUtils.objectMapper;
import static org.apache.http.HttpHeaders.AUTHORIZATION;
import static org.apache.http.HttpHeaders.CONTENT_TYPE;
import static org.apache.http.HttpStatus.SC_BAD_REQUEST;
import static org.apache.http.HttpStatus.SC_INTERNAL_SERVER_ERROR;
import static org.apache.http.HttpStatus.SC_OK;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.hamcrest.core.StringContains.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.amazonaws.services.lambda.runtime.CognitoIdentity;
import com.amazonaws.services.lambda.runtime.Context;
import com.fasterxml.jackson.core.JsonProcessingException;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandler;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import com.fasterxml.jackson.databind.JavaType;
import no.unit.nva.doi.CrossRefClient;
import no.unit.nva.doi.DataciteClient;
import no.unit.nva.doi.DoiProxyService;
import no.unit.nva.doi.MetadataAndContentLocation;
import no.unit.nva.doi.fetch.exceptions.MetadataNotFoundException;
import no.unit.nva.doi.fetch.model.PublicationDate;
import no.unit.nva.doi.fetch.model.RequestBody;
import no.unit.nva.doi.fetch.model.Summary;
import no.unit.nva.doi.fetch.service.PublicationConverter;
import no.unit.nva.doi.fetch.service.PublicationPersistenceService;
import no.unit.nva.doi.transformer.DoiTransformService;
import no.unit.nva.model.EntityDescription;
import no.unit.nva.model.Organization;
import no.unit.nva.model.Publication;
import no.unit.nva.model.PublicationStatus;
import no.unit.nva.model.exceptions.InvalidIssnException;
import no.unit.nva.model.exceptions.InvalidPageRangeException;
import no.unit.nva.model.util.OrgNumberMapper;
import no.unit.nva.testutils.HandlerRequestBuilder;
import no.unit.nva.testutils.TestHeaders;
import nva.commons.handlers.GatewayResponse;
import nva.commons.utils.Environment;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import org.zalando.problem.Problem;
import org.zalando.problem.Status;

public class MainHandlerTest {

    private static final String SOME_ERROR_MESSAGE = "SomeErrorMessage";
    public static final String VALID_DOI = "https://doi.org/10.1109/5.771073";

    public static final String AUTHORIZER = "authorizer";
    public static final String CLAIMS = "claims";
    public static final String CUSTOM_FEIDE_ID = "custom:feideId";
    public static final String CUSTOM_ORG_NUMBER = "custom:orgNumber";
    public static final String JUNIT = "junit";
    public static final String ALL_ORIGINS = "*";
    public static final String INVALID_HOST_STRING = "https://\\.)_";
    public static final String HTTP = "http";

    private Environment environment;

    /**
     * Set up environment.
     */
    @BeforeEach
    public void setUp() {
        environment = mock(Environment.class);
        when(environment.readEnv(ALLOWED_ORIGIN_ENV)).thenReturn(ALL_ORIGINS);
        when(environment.readEnv(PUBLICATION_API_HOST_ENV)).thenReturn("localhost:3000");
        when(environment.readEnv(PUBLICATION_API_SCHEME_ENV)).thenReturn("http");
    }

    @Test
    public void testOkResponse()
            throws Exception {
        MainHandler mainHandler = createMainHandler(environment);
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        mainHandler.handleRequest(mainHandlerInputStream(), output, getMockContext());
        GatewayResponse<Summary> gatewayResponse = parseSuccessResponse(output.toString());
        assertEquals(SC_OK, gatewayResponse.getStatusCode());
        assertTrue(gatewayResponse.getHeaders().containsKey(CONTENT_TYPE));
        assertTrue(gatewayResponse.getHeaders().containsKey(MainHandler.ACCESS_CONTROL_ALLOW_ORIGIN));
        Summary summary = gatewayResponse.getBodyObject(Summary.class);
        assertNotNull(summary.getIdentifier());
    }

    @Test
    public void processInputThrowsIllegalStateExceptionWithInternalCauseWhenUrlToPublicationProxyIsNotValid()
            throws IOException, InvalidPageRangeException, InvalidIssnException, URISyntaxException,
            MetadataNotFoundException {
        Environment environmentWithInvalidHost = createEnvironmentWithInvalidHost();
        MainHandler mainHandler = createMainHandler(environmentWithInvalidHost);
        RequestBody requestBody = createSampleRequest();
        Executable action = () -> mainHandler.processInput(requestBody, null, getMockContext());
        IllegalStateException exception = assertThrows(IllegalStateException.class, action);
        assertThat(exception.getCause().getClass(), is(equalTo(URISyntaxException.class)));
    }

    @Test
    public void testBadRequestResponse() throws Exception {
        PublicationConverter publicationConverter = mock(PublicationConverter.class);
        DoiTransformService doiTransformService = mockDoiTransformServiceReturningSuccessfulResult();
        DoiProxyService doiProxyService = mock(DoiProxyService.class);
        PublicationPersistenceService publicationPersistenceService = mock(PublicationPersistenceService.class);
        Context context = getMockContext();
        MainHandler mainHandler = new MainHandler(objectMapper, publicationConverter, doiTransformService,
            doiProxyService, publicationPersistenceService, environment);
        OutputStream output = new ByteArrayOutputStream();

        mainHandler.handleRequest(malformedInputStream(), output, context);

        GatewayResponse<Problem> gatewayResponse = parseFailureResponse(output);
        assertEquals(SC_BAD_REQUEST, gatewayResponse.getStatusCode());
        assertThat(getProblemDetail(gatewayResponse), containsString(NULL_DOI_URL_ERROR));
    }

    private String getProblemDetail(GatewayResponse<Problem> gatewayResponse) throws JsonProcessingException {
        return gatewayResponse.getBodyObject(Problem.class).getDetail();
    }

    @Test
    public void testInternalServerErrorResponse() throws Exception {
        PublicationConverter publicationConverter = mock(PublicationConverter.class);
        when(publicationConverter.toSummary(any())).thenThrow(new RuntimeException(SOME_ERROR_MESSAGE));
        DoiTransformService doiTransformService = mockDoiTransformServiceReturningSuccessfulResult();
        DoiProxyService doiProxyService = mockDoiProxyServiceReceivingSuccessfulResult();
        PublicationPersistenceService publicationPersistenceService = mock(PublicationPersistenceService.class);
        Context context = getMockContext();
        MainHandler mainHandler = new MainHandler(objectMapper, publicationConverter, doiTransformService,
            doiProxyService, publicationPersistenceService, environment);
        OutputStream output = new ByteArrayOutputStream();

        mainHandler.handleRequest(mainHandlerInputStream(), output, context);

        GatewayResponse<Problem> gatewayResponse = parseFailureResponse(output);
        assertEquals(SC_INTERNAL_SERVER_ERROR, gatewayResponse.getStatusCode());
        assertThat(getProblemDetail(gatewayResponse), containsString(SOME_ERROR_MESSAGE));
    }

    @Test
    @DisplayName("handler returns BadGateway error when DoiProxyService returns failed response")
    public void handlerReturnsBadGatewayErrorWhenDoiProxyServiceReturnsFailedResponse()
        throws Exception {

        PublicationConverter publicationConverter = mockPublicationConverter();
        DoiTransformService doiTransformService = mockDoiTransformServiceReturningSuccessfulResult();
        DoiProxyService doiProxyService = mockDoiProxyReceivingFailedResult();
        PublicationPersistenceService publicationPersistenceService = mock(PublicationPersistenceService.class);

        MainHandler handler = new MainHandler(objectMapper, publicationConverter, doiTransformService, doiProxyService,
            publicationPersistenceService, environment);
        OutputStream outputStream = outputStream();
        handler.handleRequest(mainHandlerInputStream(), outputStream, getMockContext());
        GatewayResponse<Problem> gatewayResponse = parseFailureResponse(outputStream);
        assertThat(gatewayResponse.getStatusCode(), is(equalTo(Status.BAD_GATEWAY.getStatusCode())));
        assertThat(getProblemDetail(gatewayResponse), containsString(DoiProxyService.ERROR_READING_METADATA));
    }

    @Test
    @DisplayName("handler returns BadGateway when ResourcePersistenceService returns failed response")
    public void handlerReturnsBadGatewayErrorWhenResourcePersistenceServiceReturnsFailedResponse()
        throws Exception {

        PublicationConverter publicationConverter = mockPublicationConverter();
        DoiProxyService doiProxyService = mockDoiProxyServiceReceivingSuccessfulResult();
        DoiTransformService doiTransformService = mockDoiTransformServiceReturningSuccessfulResult();

        PublicationPersistenceService publicationPersistenceService =
            mockResourcePersistenceServiceReceivingFailedResult();

        MainHandler handler = new MainHandler(objectMapper, publicationConverter, doiTransformService, doiProxyService,
            publicationPersistenceService, environment);
        OutputStream outputStream = outputStream();
        handler.handleRequest(mainHandlerInputStream(), outputStream, getMockContext());
        GatewayResponse<Problem> gatewayResponse = parseFailureResponse(outputStream);
        assertThat(gatewayResponse.getStatusCode(), is(equalTo(Status.BAD_GATEWAY.getStatusCode())));
        assertThat(getProblemDetail(gatewayResponse), containsString(PublicationPersistenceService.WARNING_MESSAGE));
    }

    private DoiProxyService mockDoiProxyReceivingFailedResult() {
        DataciteClient dataciteClient = mock(DataciteClient.class);
        CrossRefClient crossRefClient = mock(CrossRefClient.class);
        return new DoiProxyService(crossRefClient, dataciteClient);
    }

    private MainHandler createMainHandler(Environment environment)
        throws URISyntaxException, IOException, InvalidPageRangeException, InvalidIssnException,
               MetadataNotFoundException {
        PublicationConverter publicationConverter = mockPublicationConverter();
        DoiTransformService doiTransformService = mockDoiTransformServiceReturningSuccessfulResult();
        DoiProxyService doiProxyService = mockDoiProxyServiceReceivingSuccessfulResult();
        PublicationPersistenceService publicationPersistenceService = mock(PublicationPersistenceService.class);
        return new MainHandler(objectMapper, publicationConverter, doiTransformService,
            doiProxyService, publicationPersistenceService, environment);
    }

    private PublicationConverter mockPublicationConverter() {
        PublicationConverter publicationConverter = mock(PublicationConverter.class);
        when(publicationConverter.toSummary(any())).thenReturn(createSummary());
        return publicationConverter;
    }

    private DoiTransformService mockDoiTransformServiceReturningSuccessfulResult()
        throws URISyntaxException, IOException,
               InvalidPageRangeException, InvalidIssnException {
        DoiTransformService service = mock(DoiTransformService.class);
        when(service.transformPublication(anyString(), anyString(), anyString(), anyString()))
            .thenReturn(getPublication());
        return service;
    }

    private Publication getPublication() {
        return new Publication.Builder()
            .withIdentifier(UUID.randomUUID())
            .withCreatedDate(Instant.now())
            .withModifiedDate(Instant.now())
            .withStatus(PublicationStatus.DRAFT)
            .withPublisher(new Organization.Builder().withId(URI.create("http://example.org/123")).build())
            .withEntityDescription(new EntityDescription.Builder().withMainTitle("Main title").build())
            .withOwner("Owner")
            .build();
    }

    private DoiProxyService mockDoiProxyServiceReceivingSuccessfulResult()
        throws MetadataNotFoundException, IOException, URISyntaxException {
        DoiProxyService doiProxyService = mock(DoiProxyService.class);
        when(doiProxyService.lookupDoiMetadata(anyString(), any())).thenReturn(metadataAndContentLocation());
        return doiProxyService;
    }

    private MetadataAndContentLocation metadataAndContentLocation() throws JsonProcessingException {
        return new MetadataAndContentLocation("datacite",
            objectMapper.writeValueAsString(getPublication()));
    }

    private Summary createSummary() {
        return new Summary.Builder().withIdentifier(UUID.randomUUID()).withTitle("Title on publication")
            .withCreatorName("Name, Creator")
            .withDate(new PublicationDate.Builder().withYear("2020").build()).build();
    }

    private PublicationPersistenceService mockResourcePersistenceServiceReceivingFailedResult()
        throws IOException, InterruptedException {
        return new PublicationPersistenceService(mockHttpClientReceivingFailure());
    }

    @SuppressWarnings("unchecked")
    private HttpClient mockHttpClientReceivingFailure() throws IOException, InterruptedException {
        HttpClient client = mock(HttpClient.class);
        HttpResponse<Object> failedResponse = mockFailedHttpResponse();
        when(client.send(any(HttpRequest.class), any(BodyHandler.class))).thenReturn(failedResponse);
        return client;
    }

    @SuppressWarnings("unchecked")
    private HttpResponse<Object> mockFailedHttpResponse() {
        HttpResponse<Object> response = mock(HttpResponse.class);
        when(response.statusCode()).thenReturn(Status.BAD_REQUEST.getStatusCode());
        return response;
    }

    private Context getMockContext() {
        Context context = mock(Context.class);
        CognitoIdentity cognitoIdentity = mock(CognitoIdentity.class);
        when(context.getIdentity()).thenReturn(cognitoIdentity);
        when(cognitoIdentity.getIdentityPoolId()).thenReturn("junit");
        return context;
    }

    private InputStream mainHandlerInputStream() throws MalformedURLException, JsonProcessingException {

        RequestBody requestBody = new RequestBody();
        requestBody.setDoiUrl(new URL(VALID_DOI));

        Map<String, String> requestHeaders = new HashMap<>();
        requestHeaders.put(AUTHORIZATION, "some api key");
        requestHeaders.putAll(TestHeaders.getRequestHeaders());

        return new HandlerRequestBuilder<RequestBody>(objectMapper)
            .withBody(requestBody)
            .withHeaders(requestHeaders)
            .withRequestContext(getRequestContext())
            .build();
    }

    private InputStream malformedInputStream() throws JsonProcessingException {

        Map<String, String> requestHeaders = new HashMap<>();
        requestHeaders.put(AUTHORIZATION, "some api key");
        requestHeaders.putAll(TestHeaders.getRequestHeaders());

        return new HandlerRequestBuilder<RequestBody>(objectMapper)
            .withHeaders(requestHeaders)
            .withRequestContext(getRequestContext())
            .build();
    }

    private OutputStream outputStream() {
        return new ByteArrayOutputStream();
    }

    private Map<String, Object> getRequestContext() {
        return Map.of(AUTHORIZER, Map.of(
            CLAIMS, Map.of(
                CUSTOM_FEIDE_ID, JUNIT,
                CUSTOM_ORG_NUMBER, OrgNumberMapper.UNIT_ORG_NUMBER
            ))
        );
    }

    private GatewayResponse<Summary> parseSuccessResponse(String output) throws JsonProcessingException {
        return parseGatewayResponse(output, Summary.class);
    }

    private GatewayResponse<Problem> parseFailureResponse(OutputStream output) throws JsonProcessingException {
        return parseGatewayResponse(output.toString(), Problem.class);
    }

    private <T> GatewayResponse<T> parseGatewayResponse(String output, Class<T> responseObjectClass)
            throws JsonProcessingException {
        JavaType typeRef = objectMapper.getTypeFactory()
                .constructParametricType(GatewayResponse.class, responseObjectClass);
        return objectMapper.readValue(output, typeRef);
    }

    private RequestBody createSampleRequest() throws MalformedURLException {
        RequestBody requestBody = new RequestBody();
        requestBody.setDoiUrl(new URL(VALID_DOI));
        return requestBody;
    }

    private Environment createEnvironmentWithInvalidHost() {
        Environment environment = mock(Environment.class);
        when(environment.readEnv(ALLOWED_ORIGIN_ENV)).thenReturn(ALL_ORIGINS);
        when(environment.readEnv(PUBLICATION_API_HOST_ENV)).thenReturn(INVALID_HOST_STRING);
        when(environment.readEnv(PUBLICATION_API_SCHEME_ENV)).thenReturn(HTTP);
        return environment;
    }
}
