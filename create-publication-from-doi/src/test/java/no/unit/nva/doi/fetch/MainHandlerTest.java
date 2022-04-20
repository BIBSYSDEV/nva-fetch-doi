package no.unit.nva.doi.fetch;

import static com.google.common.net.HttpHeaders.AUTHORIZATION;
import static com.google.common.net.HttpHeaders.CONTENT_TYPE;
import static java.net.HttpURLConnection.HTTP_BAD_GATEWAY;
import static java.net.HttpURLConnection.HTTP_BAD_REQUEST;
import static java.net.HttpURLConnection.HTTP_INTERNAL_ERROR;
import static java.net.HttpURLConnection.HTTP_OK;
import static no.unit.nva.doi.fetch.RestApiConfig.restServiceObjectMapper;
import static nva.commons.apigateway.ApiGatewayHandler.MESSAGE_FOR_RUNTIME_EXCEPTIONS_HIDING_IMPLEMENTATION_DETAILS_TO_API_CLIENTS;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.hamcrest.core.StringContains.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import com.amazonaws.services.lambda.runtime.CognitoIdentity;
import com.amazonaws.services.lambda.runtime.Context;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JavaType;
import com.google.common.net.HttpHeaders;
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
import java.util.Optional;
import java.util.UUID;
import no.unit.nva.doi.CrossRefClient;
import no.unit.nva.doi.DataciteClient;
import no.unit.nva.doi.DoiProxyService;
import no.unit.nva.doi.MetadataAndContentLocation;
import no.unit.nva.doi.fetch.exceptions.MetadataNotFoundException;
import no.unit.nva.doi.fetch.exceptions.UnsupportedDocumentTypeException;
import no.unit.nva.doi.fetch.model.PublicationDate;
import no.unit.nva.doi.fetch.model.RequestBody;
import no.unit.nva.doi.fetch.model.Summary;
import no.unit.nva.doi.fetch.service.PublicationConverter;
import no.unit.nva.doi.fetch.service.PublicationPersistenceService;
import no.unit.nva.doi.transformer.DoiTransformService;
import no.unit.nva.doi.transformer.utils.BareProxyClient;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.metadata.CreatePublicationRequest;
import no.unit.nva.metadata.service.MetadataService;
import no.unit.nva.model.EntityDescription;
import no.unit.nva.model.Organization;
import no.unit.nva.model.Publication;
import no.unit.nva.model.PublicationStatus;
import no.unit.nva.model.exceptions.InvalidIsbnException;
import no.unit.nva.model.exceptions.InvalidIssnException;
import no.unit.nva.testutils.HandlerRequestBuilder;
import no.unit.nva.testutils.TestHeaders;
import nva.commons.apigateway.ApiGatewayHandler;
import nva.commons.apigateway.GatewayResponse;
import nva.commons.core.Environment;
import nva.commons.logutils.LogUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.zalando.problem.Problem;
import org.zalando.problem.Status;

public class MainHandlerTest {

    public static final String VALID_DOI = "https://doi.org/10.1109/5.771073";
    public static final String SAMPLE_CUSTOMER_ID = "http://example.org/publisher/123";
    public static final String AUTHORIZER = "authorizer";
    public static final String CLAIMS = "claims";
    public static final String CUSTOM_FEIDE_ID = "custom:feideId";
    public static final String CUSTOM_CUSTOMER_ID = "custom:customerId";
    public static final String JUNIT = "junit";
    public static final String ALL_ORIGINS = "*";
    public static final String INVALID_HOST_STRING = "https://\\.)_";
    private static final String SOME_ERROR_MESSAGE = "SomeErrorMessage";
    private Environment environment;
    private Context context;
    private ByteArrayOutputStream output;

    /**
     * Set up environment.
     */
    @BeforeEach
    public void setUp() {
        environment = mock(Environment.class);
        context = getMockContext();
        output = new ByteArrayOutputStream();
        when(environment.readEnv(ApiGatewayHandler.ALLOWED_ORIGIN_ENV)).thenReturn(ALL_ORIGINS);
        when(environment.readEnv(MainHandler.PUBLICATION_API_HOST_ENV)).thenReturn("localhost");
    }

    @Test
    public void testOkResponse()
        throws Exception {
        MainHandler mainHandler = createMainHandler(environment);
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        mainHandler.handleRequest(createSampleRequest(), output, context);
        GatewayResponse<Summary> gatewayResponse = parseSuccessResponse(output.toString());
        assertEquals(HTTP_OK, gatewayResponse.getStatusCode());
        assertThat(gatewayResponse.getHeaders(), hasKey(CONTENT_TYPE));
        assertThat(gatewayResponse.getHeaders(), hasKey(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN));
        Summary summary = gatewayResponse.getBodyObject(Summary.class);
        assertNotNull(summary.getIdentifier());
    }

    @Test
    public void handleRequestReturnsSummaryWithIdentifierWhenUrlIsValidNonDoi()
        throws Exception {
        MainHandler mainHandler = createMainHandler(environment);
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        mainHandler.handleRequest(nonDoiUrlInputStream(), output, context);
        GatewayResponse<Summary> gatewayResponse = parseSuccessResponse(output.toString());
        assertEquals(HTTP_OK, gatewayResponse.getStatusCode());
        assertThat(gatewayResponse.getHeaders(), hasKey(CONTENT_TYPE));
        assertThat(gatewayResponse.getHeaders(), hasKey(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN));
        Summary summary = gatewayResponse.getBodyObject(Summary.class);
        assertNotNull(summary.getIdentifier());
    }

    @Test
    public void testBadGatewayResponseWhenUrlIsInvalidNonDoi() throws Exception {
        PublicationConverter publicationConverter = mock(PublicationConverter.class);

        MainHandler mainHandler = handlerReceivingEmptyResponse(publicationConverter);
        mainHandler.handleRequest(nonDoiUrlInputStream(), output, context);
        GatewayResponse<Problem> gatewayResponse = parseFailureResponse(output);
        assertEquals(HTTP_BAD_GATEWAY, gatewayResponse.getStatusCode());
        assertThat(getProblemDetail(gatewayResponse), containsString(MainHandler.NO_METADATA_FOUND_FOR));
    }

    @Test
    public void shouldReturnInternalErrorWhenUrlToPublicationProxyIsNotValidAndContainInformativeMessage()
        throws IOException, InvalidIssnException, URISyntaxException,
               MetadataNotFoundException, InvalidIsbnException, UnsupportedDocumentTypeException {

        var logger = LogUtils.getTestingAppenderForRootLogger();
        Environment environmentWithInvalidHost = createEnvironmentWithInvalidHost();
        MainHandler mainHandler = createMainHandler(environmentWithInvalidHost);

        mainHandler.handleRequest(createSampleRequest(), output, context);
        var response = GatewayResponse.fromOutputStream(output,Problem.class);
        assertThat(response.getStatusCode(),is(equalTo(HTTP_INTERNAL_ERROR)));
        assertThat(logger.getMessages(),containsString("Missing host for creating URI"));

    }

    @Test
    public void testBadRequestResponse() throws Exception {
        PublicationConverter publicationConverter = mock(PublicationConverter.class);
        DoiTransformService doiTransformService = mockDoiTransformServiceReturningSuccessfulResult();
        DoiProxyService doiProxyService = mock(DoiProxyService.class);
        PublicationPersistenceService publicationPersistenceService = mock(PublicationPersistenceService.class);
        BareProxyClient bareProxyClient = mock(BareProxyClient.class);
        MetadataService metadataService = mock(MetadataService.class);
        MainHandler mainHandler = new MainHandler(publicationConverter, doiTransformService,
                                                  doiProxyService, publicationPersistenceService, bareProxyClient,
                                                  metadataService, environment);
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        mainHandler.handleRequest(malformedInputStream(), output, context);
        GatewayResponse<Problem> gatewayResponse = parseFailureResponse(output);
        assertEquals(HTTP_BAD_REQUEST, gatewayResponse.getStatusCode());
        assertThat(getProblemDetail(gatewayResponse), containsString(MainHandler.NULL_DOI_URL_ERROR));
    }

    @Test
    public void testInternalServerErrorResponse() throws Exception {
        PublicationConverter publicationConverter = mock(PublicationConverter.class);
        when(publicationConverter.toSummary(any())).thenThrow(new RuntimeException(SOME_ERROR_MESSAGE));
        DoiTransformService doiTransformService = mockDoiTransformServiceReturningSuccessfulResult();
        DoiProxyService doiProxyService = mockDoiProxyServiceReceivingSuccessfulResult();
        PublicationPersistenceService publicationPersistenceService = mock(PublicationPersistenceService.class);
        BareProxyClient bareProxyClient = mock(BareProxyClient.class);
        MetadataService metadataService = mock(MetadataService.class);

        MainHandler mainHandler = new MainHandler(publicationConverter, doiTransformService,
                                                  doiProxyService, publicationPersistenceService, bareProxyClient,
                                                  metadataService, environment);
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        mainHandler.handleRequest(createSampleRequest(), output, context);
        GatewayResponse<Problem> gatewayResponse = parseFailureResponse(output);
        assertEquals(HTTP_INTERNAL_ERROR, gatewayResponse.getStatusCode());
        assertThat(getProblemDetail(gatewayResponse), containsString(
            MESSAGE_FOR_RUNTIME_EXCEPTIONS_HIDING_IMPLEMENTATION_DETAILS_TO_API_CLIENTS));
    }

    @Test
    @DisplayName("handler returns BadGateway error when DoiProxyService returns failed response")
    public void handlerReturnsBadGatewayErrorWhenDoiProxyServiceReturnsFailedResponse()
        throws Exception {

        PublicationConverter publicationConverter = mockPublicationConverter();
        DoiTransformService doiTransformService = mockDoiTransformServiceReturningSuccessfulResult();
        DoiProxyService doiProxyService = mockDoiProxyReceivingFailedResult();
        PublicationPersistenceService publicationPersistenceService = mock(PublicationPersistenceService.class);
        BareProxyClient bareProxyClient = mock(BareProxyClient.class);
        MetadataService metadataService = mock(MetadataService.class);

        MainHandler handler = new MainHandler(publicationConverter, doiTransformService, doiProxyService,
                                              publicationPersistenceService, bareProxyClient, metadataService,
                                              environment);
        ByteArrayOutputStream outputStream = outputStream();
        handler.handleRequest(createSampleRequest(), outputStream, context);
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
        BareProxyClient bareProxyClient = mock(BareProxyClient.class);
        MetadataService metadataService = mock(MetadataService.class);

        PublicationPersistenceService publicationPersistenceService =
            mockResourcePersistenceServiceReceivingFailedResult();

        MainHandler handler = new MainHandler(publicationConverter, doiTransformService, doiProxyService,
                                              publicationPersistenceService, bareProxyClient, metadataService,
                                              environment);
        ByteArrayOutputStream outputStream = outputStream();
        handler.handleRequest(createSampleRequest(), outputStream, context);
        GatewayResponse<Problem> gatewayResponse = parseFailureResponse(outputStream);
        assertThat(gatewayResponse.getStatusCode(), is(equalTo(Status.BAD_GATEWAY.getStatusCode())));
        assertThat(getProblemDetail(gatewayResponse), containsString(PublicationPersistenceService.WARNING_MESSAGE));
    }

    private MainHandler handlerReceivingEmptyResponse(PublicationConverter publicationConverter) {
        DoiTransformService doiTransformService = mock(DoiTransformService.class);
        DoiProxyService doiProxyService = mock(DoiProxyService.class);
        PublicationPersistenceService publicationPersistenceService = mock(PublicationPersistenceService.class);
        BareProxyClient bareProxyClient = mock(BareProxyClient.class);
        MetadataService metadataService = mock(MetadataService.class);
        when(metadataService.generateCreatePublicationRequest(any())).thenReturn(Optional.empty());

        return new MainHandler(publicationConverter, doiTransformService,
                               doiProxyService, publicationPersistenceService, bareProxyClient, metadataService,
                               environment);
    }

    private String getProblemDetail(GatewayResponse<Problem> gatewayResponse) throws JsonProcessingException {
        return gatewayResponse.getBodyObject(Problem.class).getDetail();
    }

    private DoiProxyService mockDoiProxyReceivingFailedResult() {
        DataciteClient dataciteClient = mock(DataciteClient.class);
        CrossRefClient crossRefClient = mock(CrossRefClient.class);
        return new DoiProxyService(crossRefClient, dataciteClient);
    }

    private MainHandler createMainHandler(Environment environment)
        throws URISyntaxException, IOException, InvalidIssnException,
               MetadataNotFoundException, InvalidIsbnException, UnsupportedDocumentTypeException {
        PublicationConverter publicationConverter = mockPublicationConverter();
        DoiTransformService doiTransformService = mockDoiTransformServiceReturningSuccessfulResult();
        DoiProxyService doiProxyService = mockDoiProxyServiceReceivingSuccessfulResult();
        PublicationPersistenceService publicationPersistenceService = mock(PublicationPersistenceService.class);
        BareProxyClient bareProxyClient = mock(BareProxyClient.class);
        MetadataService metadataService = mockMetadataServiceReturningSuccessfulResult();

        return new MainHandler(publicationConverter, doiTransformService,
                               doiProxyService, publicationPersistenceService, bareProxyClient, metadataService,
                               environment);
    }

    private PublicationConverter mockPublicationConverter() {
        PublicationConverter publicationConverter = mock(PublicationConverter.class);
        when(publicationConverter.toSummary(any())).thenReturn(createSummary());
        return publicationConverter;
    }

    private DoiTransformService mockDoiTransformServiceReturningSuccessfulResult()
        throws URISyntaxException, IOException, InvalidIssnException,
               InvalidIsbnException, UnsupportedDocumentTypeException {
        DoiTransformService service = mock(DoiTransformService.class);
        when(service.transformPublication(anyString(), anyString(), anyString(), any()))
            .thenReturn(getPublication());
        return service;
    }

    private MetadataService mockMetadataServiceReturningSuccessfulResult() {
        MetadataService service = mock(MetadataService.class);

        EntityDescription entityDescription = new EntityDescription();
        entityDescription.setMainTitle("Main title");
        CreatePublicationRequest request = new CreatePublicationRequest();
        request.setEntityDescription(entityDescription);

        when(service.generateCreatePublicationRequest(any()))
            .thenReturn(Optional.of(request));
        return service;
    }

    private Publication getPublication() {
        return new Publication.Builder()
            .withIdentifier(new SortableIdentifier(UUID.randomUUID().toString()))
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
                                              restServiceObjectMapper.writeValueAsString(getPublication()));
    }

    private Summary createSummary() {
        return new Summary.Builder().withIdentifier(SortableIdentifier.next())
            .withTitle("Title on publication")
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

    private InputStream createSampleRequest(URL url) throws JsonProcessingException {

        RequestBody requestBody = createSampleRequestBody(url);

        Map<String, String> requestHeaders = new HashMap<>();
        requestHeaders.put(AUTHORIZATION, "some api key");
        requestHeaders.putAll(TestHeaders.getRequestHeaders());

        return new HandlerRequestBuilder<RequestBody>(restServiceObjectMapper)
            .withBody(requestBody)
            .withHeaders(requestHeaders)
            .withRequestContext(getRequestContext())
            .build();
    }

    private InputStream createSampleRequest() throws MalformedURLException, JsonProcessingException {
        return createSampleRequest(new URL(VALID_DOI));
    }

    private InputStream nonDoiUrlInputStream() throws MalformedURLException, JsonProcessingException {
        return createSampleRequest(new URL("http://example.org/metadata"));
    }

    private InputStream malformedInputStream() throws JsonProcessingException {

        Map<String, String> requestHeaders = new HashMap<>();
        requestHeaders.put(AUTHORIZATION, "some api key");
        requestHeaders.putAll(TestHeaders.getRequestHeaders());

        return new HandlerRequestBuilder<RequestBody>(restServiceObjectMapper)
            .withHeaders(requestHeaders)
            .withRequestContext(getRequestContext())
            .build();
    }

    private ByteArrayOutputStream outputStream() {
        return new ByteArrayOutputStream();
    }

    private Map<String, Object> getRequestContext() {
        return Map.of(AUTHORIZER, Map.of(
            CLAIMS, Map.of(
                CUSTOM_FEIDE_ID, JUNIT,
                CUSTOM_CUSTOMER_ID, SAMPLE_CUSTOMER_ID
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
        JavaType typeRef = restServiceObjectMapper.getTypeFactory()
            .constructParametricType(GatewayResponse.class, responseObjectClass);
        return restServiceObjectMapper.readValue(output, typeRef);
    }

    private RequestBody createSampleRequestBody(URL url) {
        RequestBody requestBody = new RequestBody();
        requestBody.setDoiUrl(url);
        return requestBody;
    }

    private Environment createEnvironmentWithInvalidHost() {
        Environment environment = mock(Environment.class);
        when(environment.readEnv(ApiGatewayHandler.ALLOWED_ORIGIN_ENV)).thenReturn(ALL_ORIGINS);
        when(environment.readEnv(MainHandler.PUBLICATION_API_HOST_ENV)).thenReturn(INVALID_HOST_STRING);
        return environment;
    }
}
