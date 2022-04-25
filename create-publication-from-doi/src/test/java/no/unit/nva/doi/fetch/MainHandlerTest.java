package no.unit.nva.doi.fetch;

import static com.google.common.net.HttpHeaders.AUTHORIZATION;
import static com.google.common.net.HttpHeaders.CONTENT_TYPE;
import static java.net.HttpURLConnection.HTTP_BAD_REQUEST;
import static java.net.HttpURLConnection.HTTP_OK;
import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static no.unit.nva.testutils.RandomDataGenerator.randomUri;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasKey;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import com.amazonaws.services.lambda.runtime.Context;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.common.net.HttpHeaders;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import no.sikt.nva.doi.fetch.jsonconfig.Json;
import no.unit.nva.commons.json.JsonUtils;
import no.unit.nva.doi.DoiProxyService;
import no.unit.nva.doi.MetadataAndContentLocation;
import no.unit.nva.doi.fetch.exceptions.MetadataNotFoundException;
import no.unit.nva.doi.fetch.exceptions.UnsupportedDocumentTypeException;
import no.unit.nva.doi.fetch.model.Summary;
import no.unit.nva.doi.fetch.service.PublicationConverter;
import no.unit.nva.doi.fetch.service.PublicationPersistenceService;
import no.unit.nva.doi.transformer.DoiTransformService;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.model.EntityDescription;
import no.unit.nva.model.Organization;
import no.unit.nva.model.Publication;
import no.unit.nva.model.PublicationStatus;
import no.unit.nva.model.exceptions.InvalidIsbnException;
import no.unit.nva.model.exceptions.InvalidIssnException;
import no.unit.nva.stubs.FakeContext;
import no.unit.nva.testutils.HandlerRequestBuilder;
import no.unit.nva.testutils.TestHeaders;
import nva.commons.apigateway.GatewayResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.zalando.problem.Problem;

class MainHandlerTest {

    public static final String VALID_DOI = "https://doi.org/10.1109/5.771073";
    public static final String ALL_ORIGINS = "*";
    public static final String INVALID_HOST_STRING = "https://\\.)_";
    private static final String SOME_ERROR_MESSAGE = "SomeErrorMessage";
    private Context context;
    private ByteArrayOutputStream output;

    @BeforeEach
    public void setUp() {

        context = new FakeContext();
        output = new ByteArrayOutputStream();
    }

    @Test
    public void shouldReturnOkWithContentTypeAndAccess()
        throws Exception {
        MainHandler mainHandler = createMainHandler();
        mainHandler.handleRequest(createSampleRequest(), output, context);
        GatewayResponse<Summary> gatewayResponse = GatewayResponse.fromOutputStream(output, Summary.class);
        assertEquals(HTTP_OK, gatewayResponse.getStatusCode());
        assertThat(gatewayResponse.getHeaders(), hasKey(CONTENT_TYPE));
        assertThat(gatewayResponse.getHeaders(), hasKey(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN));
        Summary summary = gatewayResponse.getBodyObject(Summary.class);
        assertNotNull(summary.getIdentifier());
    }

    @Test
    public void handleRequestReturnsSummaryWithIdentifierWhenUrlIsValidNonDoi()
        throws Exception {
        MainHandler mainHandler = createMainHandler();
        mainHandler.handleRequest(nonDoiUrlInputStream(), output, context);
        GatewayResponse<Summary> gatewayResponse = GatewayResponse.fromOutputStream(output, Summary.class);
        Summary summary = gatewayResponse.getBodyObject(Summary.class);
        assertNotNull(summary.getIdentifier());
    }

    //        @Test
    //        public void testBadGatewayResponseWhenUrlIsInvalidNonDoi() throws Exception {
    //            PublicationConverter publicationConverter = mock(PublicationConverter.class);
    //
    //            MainHandler mainHandler = handlerReceivingEmptyResponse(publicationConverter);
    //            mainHandler.handleRequest(nonDoiUrlInputStream(), output, context);
    //            GatewayResponse<Problem> gatewayResponse = GatewayResponse.fromOutputStream(output, Problem.class);
    //            assertEquals(HTTP_BAD_GATEWAY, gatewayResponse.getStatusCode());
    //            Problem problem = gatewayResponse.getBodyObject(Problem.class);
    //            assertThat(problem.getDetail(), containsString(MainHandler.NO_METADATA_FOUND_FOR));
    //        }

    @Test
    void shouldReturnBadRequestWhenInputDoesNotContainAUri() throws Exception {
        PublicationConverter publicationConverter = mock(PublicationConverter.class);
        DoiTransformService doiTransformService = mockDoiTransformServiceReturningSuccessfulResult();
        DoiProxyService doiProxyService = mock(DoiProxyService.class);
        PublicationPersistenceService publicationPersistenceService = mock(PublicationPersistenceService.class);

        //            MetadataService metadataService = mock(MetadataService.class);
        MainHandler mainHandler = new MainHandler(doiProxyService);
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        mainHandler.handleRequest(malformedRequest(), output, context);
        GatewayResponse<Problem> gatewayResponse = GatewayResponse.fromOutputStream(output, Problem.class);
        var problem = gatewayResponse.getBodyObject(Problem.class);
        assertEquals(HTTP_BAD_REQUEST, gatewayResponse.getStatusCode());
        assertThat(problem.getDetail(), containsString(MainHandler.NULL_DOI_URL_ERROR));
    }

    //
    //    @Test
    //    public void testInternalServerErrorResponse() throws Exception {
    //        PublicationConverter publicationConverter = mock(PublicationConverter.class);
    //        when(publicationConverter.toSummary(any())).thenThrow(new RuntimeException(SOME_ERROR_MESSAGE));
    //        DoiTransformService doiTransformService = mockDoiTransformServiceReturningSuccessfulResult();
    //        DoiProxyService doiProxyService = mockDoiProxyServiceReceivingSuccessfulResult();
    //        PublicationPersistenceService publicationPersistenceService = mock(PublicationPersistenceService.class);
    //        BareProxyClient bareProxyClient = mock(BareProxyClient.class);
    //        MetadataService metadataService = mock(MetadataService.class);
    //
    //        MainHandler mainHandler = new MainHandler(publicationConverter, doiTransformService,
    //                                                  doiProxyService, publicationPersistenceService, bareProxyClient,
    //                                                  metadataService, environment);
    //        ByteArrayOutputStream output = new ByteArrayOutputStream();
    //        mainHandler.handleRequest(createSampleRequest(), output, context);
    //        GatewayResponse<Problem> gatewayResponse = parseFailureResponse(output);
    //        assertEquals(HTTP_INTERNAL_ERROR, gatewayResponse.getStatusCode());
    //        assertThat(getProblemDetail(gatewayResponse), containsString(
    //            MESSAGE_FOR_RUNTIME_EXCEPTIONS_HIDING_IMPLEMENTATION_DETAILS_TO_API_CLIENTS));
    //    }
    //
    //    @Test
    //    @DisplayName("handler returns BadGateway error when DoiProxyService returns failed response")
    //    public void handlerReturnsBadGatewayErrorWhenDoiProxyServiceReturnsFailedResponse()
    //        throws Exception {
    //
    //        PublicationConverter publicationConverter = mockPublicationConverter();
    //        DoiTransformService doiTransformService = mockDoiTransformServiceReturningSuccessfulResult();
    //        DoiProxyService doiProxyService = mockDoiProxyReceivingFailedResult();
    //        PublicationPersistenceService publicationPersistenceService = mock(PublicationPersistenceService.class);
    //        BareProxyClient bareProxyClient = mock(BareProxyClient.class);
    //        MetadataService metadataService = mock(MetadataService.class);
    //
    //        MainHandler handler = new MainHandler(publicationConverter, doiTransformService, doiProxyService,
    //                                              publicationPersistenceService, bareProxyClient, metadataService,
    //                                              environment);
    //        ByteArrayOutputStream outputStream = outputStream();
    //        handler.handleRequest(createSampleRequest(), outputStream, context);
    //        GatewayResponse<Problem> gatewayResponse = parseFailureResponse(outputStream);
    //        assertThat(gatewayResponse.getStatusCode(), is(equalTo(Status.BAD_GATEWAY.getStatusCode())));
    //        assertThat(getProblemDetail(gatewayResponse), containsString(DoiProxyService.ERROR_READING_METADATA));
    //    }
    //
    //    @Test
    //    @DisplayName("handler returns BadGateway when ResourcePersistenceService returns failed response")
    //    public void handlerReturnsBadGatewayErrorWhenResourcePersistenceServiceReturnsFailedResponse()
    //        throws Exception {
    //
    //        PublicationConverter publicationConverter = mockPublicationConverter();
    //        DoiProxyService doiProxyService = mockDoiProxyServiceReceivingSuccessfulResult();
    //        DoiTransformService doiTransformService = mockDoiTransformServiceReturningSuccessfulResult();
    //        BareProxyClient bareProxyClient = mock(BareProxyClient.class);
    //        MetadataService metadataService = mock(MetadataService.class);
    //
    //        PublicationPersistenceService publicationPersistenceService =
    //            mockResourcePersistenceServiceReceivingFailedResult();
    //
    //        MainHandler handler = new MainHandler(publicationConverter, doiTransformService, doiProxyService,
    //                                              publicationPersistenceService, bareProxyClient, metadataService,
    //                                              environment);
    //        ByteArrayOutputStream outputStream = outputStream();
    //        handler.handleRequest(createSampleRequest(), outputStream, context);
    //        GatewayResponse<Problem> gatewayResponse = parseFailureResponse(outputStream);
    //        assertThat(gatewayResponse.getStatusCode(), is(equalTo(Status.BAD_GATEWAY.getStatusCode())));
    //        assertThat(getProblemDetail(gatewayResponse), containsString(PublicationPersistenceService
    //        .WARNING_MESSAGE));
    //    }
    //
    private MainHandler handlerReceivingEmptyResponse(PublicationConverter publicationConverter) {
        DoiTransformService doiTransformService = mock(DoiTransformService.class);
        DoiProxyService doiProxyService = mock(DoiProxyService.class);
        PublicationPersistenceService publicationPersistenceService = mock(PublicationPersistenceService.class);

        //        MetadataService metadataService = mock(MetadataService.class);
        //        when(metadataService.generateCreatePublicationRequest(any())).thenReturn(Optional.empty());

        return new MainHandler(doiProxyService);
    }

    //
    //    private String getProblemDetail(GatewayResponse<Problem> gatewayResponse) throws JsonProcessingException {
    //        return gatewayResponse.getBodyObject(Problem.class).getDetail();
    //    }
    //
    //    private DoiProxyService mockDoiProxyReceivingFailedResult() {
    //        DataciteClient dataciteClient = mock(DataciteClient.class);
    //        CrossRefClient crossRefClient = mock(CrossRefClient.class);
    //        return new DoiProxyService(crossRefClient, dataciteClient);
    //    }
    //
    private MainHandler createMainHandler() throws MetadataNotFoundException, IOException, URISyntaxException {
        //        PublicationConverter publicationConverter = mockPublicationConverter();
        //        DoiTransformService doiTransformService = mockDoiTransformServiceReturningSuccessfulResult();
        var doiProxyService = mockDoiProxyServiceReceivingSuccessfulResult();
        //        PublicationPersistenceService publicationPersistenceService = mock(PublicationPersistenceService
        //        .class);
        //        BareProxyClient bareProxyClient = mock(BareProxyClient.class);
        //        MetadataService metadataService = mockMetadataServiceReturningSuccessfulResult();

        return new MainHandler(doiProxyService);
    }

    //
    //    private PublicationConverter mockPublicationConverter() {
    //        PublicationConverter publicationConverter = mock(PublicationConverter.class);
    //        when(publicationConverter.toSummary(any())).thenReturn(createSummary());
    //        return publicationConverter;
    //    }
    //
    private DoiTransformService mockDoiTransformServiceReturningSuccessfulResult()
        throws URISyntaxException, IOException, InvalidIssnException,
               InvalidIsbnException, UnsupportedDocumentTypeException {
        DoiTransformService service = mock(DoiTransformService.class);
        when(service.transformPublication(anyString(), anyString(), anyString(), any()))
            .thenReturn(getPublication());
        return service;
    }

    //
    //    private MetadataService mockMetadataServiceReturningSuccessfulResult() {
    //        MetadataService service = mock(MetadataService.class);
    //
    //        EntityDescription entityDescription = new EntityDescription();
    //        entityDescription.setMainTitle("Main title");
    //        CreatePublicationRequest request = new CreatePublicationRequest();
    //        request.setEntityDescription(entityDescription);
    //
    //        when(service.generateCreatePublicationRequest(any()))
    //            .thenReturn(Optional.of(request));
    //        return service;
    //    }
    //
    private Publication getPublication() {
        return new Publication.Builder()
            .withIdentifier(SortableIdentifier.next())
            .withCreatedDate(Instant.now())
            .withModifiedDate(Instant.now())
            .withStatus(PublicationStatus.DRAFT)
            .withPublisher(new Organization.Builder().withId(URI.create("http://example.org/123")).build())
            .withEntityDescription(new EntityDescription.Builder().withMainTitle("Main title").build())
            .withOwner("Owner")
            .build();
    }

    //
    private DoiProxyService mockDoiProxyServiceReceivingSuccessfulResult()
        throws MetadataNotFoundException, IOException, URISyntaxException {
        DoiProxyService doiProxyService = mock(DoiProxyService.class);
        when(doiProxyService.lookupDoiMetadata(anyString(), any())).thenReturn(metadataAndContentLocation());
        return doiProxyService;
    }

    private MetadataAndContentLocation metadataAndContentLocation() throws JsonProcessingException {
        return new MetadataAndContentLocation("datacite",
                                              Json.writeValueAsString(getPublication()));
    }
    //
    //    private Summary createSummary() {
    //        return new Summary.Builder().withIdentifier(SortableIdentifier.next())
    //            .withTitle("Title on publication")
    //            .withCreatorName("Name, Creator")
    //            .withDate(new PublicationDate.Builder().withYear("2020").build()).build();
    //    }
    //
    //    private PublicationPersistenceService mockResourcePersistenceServiceReceivingFailedResult()
    //        throws IOException, InterruptedException {
    //        return new PublicationPersistenceService(mockHttpClientReceivingFailure());
    //    }
    //
    //    @SuppressWarnings("unchecked")
    //    private HttpClient mockHttpClientReceivingFailure() throws IOException, InterruptedException {
    //        HttpClient client = mock(HttpClient.class);
    //        HttpResponse<Object> failedResponse = mockFailedHttpResponse();
    //        when(client.send(any(HttpRequest.class), any(BodyHandler.class))).thenReturn(failedResponse);
    //        return client;
    //    }
    //
    //    @SuppressWarnings("unchecked")
    //    private HttpResponse<Object> mockFailedHttpResponse() {
    //        HttpResponse<Object> response = mock(HttpResponse.class);
    //        when(response.statusCode()).thenReturn(Status.BAD_REQUEST.getStatusCode());
    //        return response;
    //    }
    //
    //    private Context getMockContext() {
    //        CognitoIdentity cognitoIdentity = mock(CognitoIdentity.class);
    //        when(cognitoIdentity.getIdentityPoolId()).thenReturn("junit");
    //        return new FakeContext() {
    //            @Override
    //            public CognitoIdentity getIdentity() {
    //                return cognitoIdentity;
    //            }
    //        };
    //    }
    //

    private InputStream nonDoiUrlInputStream() throws JsonProcessingException {
        return createSampleRequest(randomUri());
    }

    private InputStream malformedRequest() throws JsonProcessingException {

        Map<String, String> requestHeaders = new HashMap<>();
        requestHeaders.put(AUTHORIZATION, "some api key");
        requestHeaders.putAll(TestHeaders.getRequestHeaders());

        return new HandlerRequestBuilder<RequestBody>(JsonUtils.dtoObjectMapper)
            .withHeaders(requestHeaders)
            .withNvaUsername(randomString())
            .withCustomerId(randomUri().toString())
            .build();
    }

    //
    //    private ByteArrayOutputStream outputStream() {
    //        return new ByteArrayOutputStream();
    //    }
    //
    //    private GatewayResponse<Summary> parseSuccessResponse(String output) throws JsonProcessingException {
    //        return parseGatewayResponse(output, Summary.class);
    //    }
    //
    //    private GatewayResponse<Problem> parseFailureResponse(OutputStream output) throws JsonProcessingException {
    //        return parseGatewayResponse(output.toString(), Problem.class);
    //    }
    //
    //    private <T> GatewayResponse<T> parseGatewayResponse(String output, Class<T> responseObjectClass)
    //        throws JsonProcessingException {
    //        JavaType typeRef = restServiceObjectMapper.getTypeFactory()
    //            .constructParametricType(GatewayResponse.class, responseObjectClass);
    //        return restServiceObjectMapper.readValue(output, typeRef);
    //    }
    //
    private InputStream createSampleRequest() throws JsonProcessingException {
        return createSampleRequest(URI.create(VALID_DOI));
    }

    private InputStream createSampleRequest(URI uri) throws JsonProcessingException {

        RequestBody requestBody = createSampleRequestBody(uri);

        Map<String, String> requestHeaders = new HashMap<>();
        requestHeaders.put(AUTHORIZATION, "some api key");
        requestHeaders.putAll(TestHeaders.getRequestHeaders());

        return new HandlerRequestBuilder<RequestBody>(JsonUtils.dtoObjectMapper)
            .withBody(requestBody)
            .withHeaders(requestHeaders)
            .withNvaUsername(randomString())
            .withCustomerId(randomUri().toString())
            .build();
    }

    private RequestBody createSampleRequestBody(URI url) {
        RequestBody requestBody = new RequestBody();
        requestBody.setDoiUrl(url);
        return requestBody;
    }
}
