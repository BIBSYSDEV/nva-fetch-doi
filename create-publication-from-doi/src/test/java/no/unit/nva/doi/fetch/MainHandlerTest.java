package no.unit.nva.doi.fetch;

class MainHandlerTest {

//    public static final String VALID_DOI = "https://doi.org/10.1109/5.771073";
//    public static final String ALL_ORIGINS = "*";
//    public static final String INVALID_HOST_STRING = "https://\\.)_";
//    private static final String SOME_ERROR_MESSAGE = "SomeErrorMessage";
//    private Environment environment;
//    private Context context;
//    private ByteArrayOutputStream output;
//
//    @BeforeEach
//    public void setUp() {
//        environment = mock(Environment.class);
//        context = getMockContext();
//        output = new ByteArrayOutputStream();
//        when(environment.readEnv(ApiGatewayHandler.ALLOWED_ORIGIN_ENV)).thenReturn(ALL_ORIGINS);
//        when(environment.readEnv(MainHandler.PUBLICATION_API_HOST_ENV)).thenReturn("localhost");
//    }
//
//    @Test
//    void testLogging()
//        throws MetadataNotFoundException, InvalidIssnException, URISyntaxException, IOException, InvalidIsbnException,
//               UnsupportedDocumentTypeException {
//        var logger = LogUtils.getTestingAppender(MainHandler.class);
//        MainHandler mainHandler = createMainHandler(environment);
//        ByteArrayOutputStream output = new ByteArrayOutputStream();
//        mainHandler.handleRequest(createSampleRequest(), output, context);
//        assertThat(logger.getMessages(), containsString("world"));
//    }
//
//    @Test
//    public void testOkResponse()
//        throws Exception {
//        MainHandler mainHandler = createMainHandler(environment);
//        ByteArrayOutputStream output = new ByteArrayOutputStream();
//        mainHandler.handleRequest(createSampleRequest(), output, context);
//        GatewayResponse<Summary> gatewayResponse = parseSuccessResponse(output.toString());
//        assertEquals(HTTP_OK, gatewayResponse.getStatusCode());
//        assertThat(gatewayResponse.getHeaders(), hasKey(CONTENT_TYPE));
//        assertThat(gatewayResponse.getHeaders(), hasKey(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN));
//        Summary summary = gatewayResponse.getBodyObject(Summary.class);
//        assertNotNull(summary.getIdentifier());
//    }
//
//    @Test
//    public void handleRequestReturnsSummaryWithIdentifierWhenUrlIsValidNonDoi()
//        throws Exception {
//        MainHandler mainHandler = createMainHandler(environment);
//        ByteArrayOutputStream output = new ByteArrayOutputStream();
//        mainHandler.handleRequest(nonDoiUrlInputStream(), output, context);
//        GatewayResponse<Summary> gatewayResponse = parseSuccessResponse(output.toString());
//        assertEquals(HTTP_OK, gatewayResponse.getStatusCode());
//        assertThat(gatewayResponse.getHeaders(), hasKey(CONTENT_TYPE));
//        assertThat(gatewayResponse.getHeaders(), hasKey(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN));
//        Summary summary = gatewayResponse.getBodyObject(Summary.class);
//        assertNotNull(summary.getIdentifier());
//    }
//
//    @Test
//    public void testBadGatewayResponseWhenUrlIsInvalidNonDoi() throws Exception {
//        PublicationConverter publicationConverter = mock(PublicationConverter.class);
//
//        MainHandler mainHandler = handlerReceivingEmptyResponse(publicationConverter);
//        mainHandler.handleRequest(nonDoiUrlInputStream(), output, context);
//        GatewayResponse<Problem> gatewayResponse = parseFailureResponse(output);
//        assertEquals(HTTP_BAD_GATEWAY, gatewayResponse.getStatusCode());
//        assertThat(getProblemDetail(gatewayResponse), containsString(MainHandler.NO_METADATA_FOUND_FOR));
//    }
//
//    @Test
//    public void shouldReturnInternalErrorWhenUrlToPublicationProxyIsNotValidAndContainInformativeMessage()
//        throws IOException, InvalidIssnException, URISyntaxException,
//               MetadataNotFoundException, InvalidIsbnException, UnsupportedDocumentTypeException {
//
//        var logger = LogUtils.getTestingAppenderForRootLogger();
//        Environment environmentWithInvalidHost = createEnvironmentWithInvalidHost();
//        MainHandler mainHandler = createMainHandler(environmentWithInvalidHost);
//
//        mainHandler.handleRequest(createSampleRequest(), output, context);
//        var response = GatewayResponse.fromOutputStream(output, Problem.class);
//        assertThat(response.getStatusCode(), is(equalTo(HTTP_INTERNAL_ERROR)));
//        assertThat(logger.getMessages(), containsString("Missing host for creating URI"));
//    }
//
//    @Test
//    public void testBadRequestResponse() throws Exception {
//        PublicationConverter publicationConverter = mock(PublicationConverter.class);
//        DoiTransformService doiTransformService = mockDoiTransformServiceReturningSuccessfulResult();
//        DoiProxyService doiProxyService = mock(DoiProxyService.class);
//        PublicationPersistenceService publicationPersistenceService = mock(PublicationPersistenceService.class);
//        BareProxyClient bareProxyClient = mock(BareProxyClient.class);
//        MetadataService metadataService = mock(MetadataService.class);
//        MainHandler mainHandler = new MainHandler(publicationConverter, doiTransformService,
//                                                  doiProxyService, publicationPersistenceService, bareProxyClient,
//                                                  metadataService, environment);
//        ByteArrayOutputStream output = new ByteArrayOutputStream();
//        mainHandler.handleRequest(malformedInputStream(), output, context);
//        GatewayResponse<Problem> gatewayResponse = parseFailureResponse(output);
//        assertEquals(HTTP_BAD_REQUEST, gatewayResponse.getStatusCode());
//        assertThat(getProblemDetail(gatewayResponse), containsString(MainHandler.NULL_DOI_URL_ERROR));
//    }
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
//        assertThat(getProblemDetail(gatewayResponse), containsString(PublicationPersistenceService.WARNING_MESSAGE));
//    }
//
//    private MainHandler handlerReceivingEmptyResponse(PublicationConverter publicationConverter) {
//        DoiTransformService doiTransformService = mock(DoiTransformService.class);
//        DoiProxyService doiProxyService = mock(DoiProxyService.class);
//        PublicationPersistenceService publicationPersistenceService = mock(PublicationPersistenceService.class);
//        BareProxyClient bareProxyClient = mock(BareProxyClient.class);
//        MetadataService metadataService = mock(MetadataService.class);
//        when(metadataService.generateCreatePublicationRequest(any())).thenReturn(Optional.empty());
//
//        return new MainHandler(publicationConverter, doiTransformService,
//                               doiProxyService, publicationPersistenceService, bareProxyClient, metadataService,
//                               environment);
//    }
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
//    private MainHandler createMainHandler(Environment environment)
//        throws URISyntaxException, IOException, InvalidIssnException,
//               MetadataNotFoundException, InvalidIsbnException, UnsupportedDocumentTypeException {
//        PublicationConverter publicationConverter = mockPublicationConverter();
//        DoiTransformService doiTransformService = mockDoiTransformServiceReturningSuccessfulResult();
//        DoiProxyService doiProxyService = mockDoiProxyServiceReceivingSuccessfulResult();
//        PublicationPersistenceService publicationPersistenceService = mock(PublicationPersistenceService.class);
//        BareProxyClient bareProxyClient = mock(BareProxyClient.class);
//        MetadataService metadataService = mockMetadataServiceReturningSuccessfulResult();
//
//        return new MainHandler(publicationConverter, doiTransformService,
//                               doiProxyService, publicationPersistenceService, bareProxyClient, metadataService,
//                               environment);
//    }
//
//    private PublicationConverter mockPublicationConverter() {
//        PublicationConverter publicationConverter = mock(PublicationConverter.class);
//        when(publicationConverter.toSummary(any())).thenReturn(createSummary());
//        return publicationConverter;
//    }
//
//    private DoiTransformService mockDoiTransformServiceReturningSuccessfulResult()
//        throws URISyntaxException, IOException, InvalidIssnException,
//               InvalidIsbnException, UnsupportedDocumentTypeException {
//        DoiTransformService service = mock(DoiTransformService.class);
//        when(service.transformPublication(anyString(), anyString(), anyString(), any()))
//            .thenReturn(getPublication());
//        return service;
//    }
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
//    private Publication getPublication() {
//        return new Publication.Builder()
//            .withIdentifier(new SortableIdentifier(UUID.randomUUID().toString()))
//            .withCreatedDate(Instant.now())
//            .withModifiedDate(Instant.now())
//            .withStatus(PublicationStatus.DRAFT)
//            .withPublisher(new Organization.Builder().withId(URI.create("http://example.org/123")).build())
//            .withEntityDescription(new EntityDescription.Builder().withMainTitle("Main title").build())
//            .withOwner("Owner")
//            .build();
//    }
//
//    private DoiProxyService mockDoiProxyServiceReceivingSuccessfulResult()
//        throws MetadataNotFoundException, IOException, URISyntaxException {
//        DoiProxyService doiProxyService = mock(DoiProxyService.class);
//        when(doiProxyService.lookupDoiMetadata(anyString(), any())).thenReturn(metadataAndContentLocation());
//        return doiProxyService;
//    }
//
//    private MetadataAndContentLocation metadataAndContentLocation() throws JsonProcessingException {
//        return new MetadataAndContentLocation("datacite",
//                                              restServiceObjectMapper.writeValueAsString(getPublication()));
//    }
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
//    private InputStream createSampleRequest(URL url) throws JsonProcessingException {
//
//        RequestBody requestBody = createSampleRequestBody(url);
//
//        Map<String, String> requestHeaders = new HashMap<>();
//        requestHeaders.put(AUTHORIZATION, "some api key");
//        requestHeaders.putAll(TestHeaders.getRequestHeaders());
//
//        return new HandlerRequestBuilder<RequestBody>(restServiceObjectMapper)
//            .withBody(requestBody)
//            .withHeaders(requestHeaders)
//            .withNvaUsername(randomString())
//            .withCustomerId(randomUri().toString())
//            .build();
//    }
//
//    private InputStream createSampleRequest() throws MalformedURLException, JsonProcessingException {
//        return createSampleRequest(new URL(VALID_DOI));
//    }
//
//    private InputStream nonDoiUrlInputStream() throws MalformedURLException, JsonProcessingException {
//        return createSampleRequest(new URL("http://example.org/metadata"));
//    }
//
//    private InputStream malformedInputStream() throws JsonProcessingException {
//
//        Map<String, String> requestHeaders = new HashMap<>();
//        requestHeaders.put(AUTHORIZATION, "some api key");
//        requestHeaders.putAll(TestHeaders.getRequestHeaders());
//
//        return new HandlerRequestBuilder<RequestBody>(restServiceObjectMapper)
//            .withHeaders(requestHeaders)
//            .withNvaUsername(randomString())
//            .withCustomerId(randomUri().toString())
//            .build();
//    }
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
//    private RequestBody createSampleRequestBody(URL url) {
//        RequestBody requestBody = new RequestBody();
//        requestBody.setDoiUrl(url);
//        return requestBody;
//    }
//
//    private Environment createEnvironmentWithInvalidHost() {
//        Environment environment = mock(Environment.class);
//        when(environment.readEnv(ApiGatewayHandler.ALLOWED_ORIGIN_ENV)).thenReturn(ALL_ORIGINS);
//        when(environment.readEnv(MainHandler.PUBLICATION_API_HOST_ENV)).thenReturn(INVALID_HOST_STRING);
//        return environment;
//    }
}
