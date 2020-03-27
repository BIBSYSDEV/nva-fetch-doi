package no.unit.nva.doi.fetch;

import static no.bibsys.aws.tools.JsonUtils.jsonParser;
import static no.unit.nva.doi.fetch.MainHandler.ALLOWED_ORIGIN_ENV;
import static no.unit.nva.doi.fetch.MainHandler.API_HOST_ENV;
import static no.unit.nva.doi.fetch.MainHandler.API_SCHEME_ENV;
import static org.apache.http.HttpHeaders.AUTHORIZATION;
import static org.apache.http.HttpHeaders.CONTENT_TYPE;
import static org.apache.http.HttpStatus.SC_BAD_REQUEST;
import static org.apache.http.HttpStatus.SC_INTERNAL_SERVER_ERROR;
import static org.apache.http.HttpStatus.SC_OK;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.hamcrest.core.StringContains.containsString;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.amazonaws.services.lambda.runtime.CognitoIdentity;
import com.amazonaws.services.lambda.runtime.Context;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.sun.tools.javadoc.Main;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.http.HttpHeaders;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiPredicate;
import no.unit.nva.doi.fetch.exceptions.NoContentLocationFoundException;
import no.unit.nva.doi.fetch.exceptions.TransformFailedException;
import no.unit.nva.doi.fetch.model.PublicationDate;
import no.unit.nva.doi.fetch.model.RequestBody;
import no.unit.nva.doi.fetch.model.Summary;
import no.unit.nva.doi.fetch.service.DoiProxyResponse;
import no.unit.nva.doi.fetch.service.DoiProxyService;
import no.unit.nva.doi.fetch.service.DoiTransformService;
import no.unit.nva.doi.fetch.service.PublicationConverter;
import no.unit.nva.doi.fetch.service.ResourcePersistenceService;
import org.apache.http.entity.ContentType;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.contrib.java.lang.system.EnvironmentVariables;
import org.mockito.Mockito;
import org.zalando.problem.Status;

public class MainHandlerTest {

    public static final String SOME_METADATA_SOURCE = "SomeMetadataSource";
    public static final BiPredicate<String, String> INCLUDE_ALL = (l, r) -> true;
    @Rule
    public final EnvironmentVariables environmentVariables = new EnvironmentVariables();
    private ObjectMapper objectMapper = MainHandler.createObjectMapper();

    private Environment environment;

    /**
     * Set up environment.
     */
    @Before
    public void setUp() {
        environment = Mockito.mock(Environment.class);
        when(environment.get(ALLOWED_ORIGIN_ENV)).thenReturn("*");
        when(environment.get(API_HOST_ENV)).thenReturn("localhost:3000");
        when(environment.get(API_SCHEME_ENV)).thenReturn("http");
    }

    @Test
    public void testDefaultConstructor() {
        environmentVariables.set(ALLOWED_ORIGIN_ENV, "*");
        environmentVariables.set(API_HOST_ENV, "localhost:3000");
        environmentVariables.set(API_SCHEME_ENV, "http");
        MainHandler findChannelFunctionApp = new MainHandler();
        assertNotNull(findChannelFunctionApp);
    }

    @Test
    public void testOkResponse()
        throws IOException, NoContentLocationFoundException, InterruptedException, TransformFailedException, URISyntaxException {
        PublicationConverter publicationConverter = mockPublicationConverter();
        DoiTransformService doiTransformService = mockDoiTransformService();
        DoiProxyService doiProxyService = mockDoiProxyService();
        ResourcePersistenceService resourcePersistenceService = mock(ResourcePersistenceService.class);
        Context context = getMockContext();
        MainHandler mainHandler = new MainHandler(objectMapper, publicationConverter, doiTransformService,
            doiProxyService, resourcePersistenceService, environment);
        OutputStream output = new ByteArrayOutputStream();

        mainHandler.handleRequest(inputStream(), output, context);

        GatewayResponse gatewayResponse = objectMapper.readValue(output.toString(), GatewayResponse.class);
        assertEquals(SC_OK, gatewayResponse.getStatusCode());
        Assert.assertTrue(gatewayResponse.getHeaders().keySet().contains(CONTENT_TYPE));
        Assert.assertTrue(gatewayResponse.getHeaders().keySet().contains(MainHandler.ACCESS_CONTROL_ALLOW_ORIGIN));
        Summary summary = objectMapper.readValue(gatewayResponse.getBody().toString(),
            Summary.class);
        assertNotNull(summary.getIdentifier());
    }

    private PublicationConverter mockPublicationConverter() {
        PublicationConverter publicationConverter = mock(PublicationConverter.class);
        when(publicationConverter.toSummary(any())).thenReturn(createSummary());
        return publicationConverter;
    }

    private DoiTransformService mockDoiTransformService()
        throws URISyntaxException, TransformFailedException, InterruptedException, IOException {
        DoiTransformService service = mock(DoiTransformService.class);
        ObjectNode sampleNode = jsonParser.createObjectNode();
        when(service.transform(any(), anyString(), anyString())).thenReturn(sampleNode);
        return service;
    }

    private DoiProxyService mockDoiProxyService()
        throws NoContentLocationFoundException, InterruptedException, IOException, URISyntaxException {
        DoiProxyService doiProxyService = mock(DoiProxyService.class);
        when(doiProxyService.lookup(any(), anyString(), anyString()))
            .thenReturn(mockDoiProxyResponse());
        return doiProxyService;
    }

    private Optional<DoiProxyResponse> mockDoiProxyResponse() {
        JsonNode sampleNode = jsonParser.createObjectNode();
        DoiProxyResponse doiProxyResponse = new DoiProxyResponse(sampleNode, SOME_METADATA_SOURCE);
        return Optional.of(doiProxyResponse);
    }

    private Summary createSummary() {
        return new Summary.Builder()
            .withIdentifier(UUID.randomUUID())
            .withTitle("Title on publication")
            .withCreatorName("Name, Creator")
            .withDate(new PublicationDate.Builder()
                .withYear("2020")
                .build())
            .build();
    }

    @Test
    public void testBadRequestResponse()
        throws IOException, InterruptedException, TransformFailedException, URISyntaxException {
        PublicationConverter publicationConverter = mock(PublicationConverter.class);
        DoiTransformService doiTransformService = mockDoiTransformService();
        DoiProxyService doiProxyService = mock(DoiProxyService.class);
        ResourcePersistenceService resourcePersistenceService = mock(ResourcePersistenceService.class);
        Context context = getMockContext();
        MainHandler mainHandler = new MainHandler(objectMapper, publicationConverter, doiTransformService,
            doiProxyService, resourcePersistenceService, environment);
        OutputStream output = new ByteArrayOutputStream();

        mainHandler.handleRequest(new ByteArrayInputStream(new byte[0]), output, context);

        GatewayResponse gatewayResponse = objectMapper.readValue(output.toString(), GatewayResponse.class);
        assertEquals(SC_BAD_REQUEST, gatewayResponse.getStatusCode());
    }

    @Test
    public void testInternalServerErrorResponse()
        throws IOException, NoContentLocationFoundException, InterruptedException, TransformFailedException, URISyntaxException {
        PublicationConverter publicationConverter = mock(PublicationConverter.class);
        when(publicationConverter.toSummary(any())).thenThrow(new RuntimeException());
        DoiTransformService doiTransformService = mockDoiTransformService();
        DoiProxyService doiProxyService = mockDoiProxyService();
        ResourcePersistenceService resourcePersistenceService = mock(ResourcePersistenceService.class);
        Context context = getMockContext();
        MainHandler mainHandler = new MainHandler(objectMapper, publicationConverter, doiTransformService,
            doiProxyService, resourcePersistenceService, environment);
        OutputStream output = new ByteArrayOutputStream();

        mainHandler.handleRequest(inputStream(), output, context);

        GatewayResponse gatewayResponse = objectMapper.readValue(output.toString(), GatewayResponse.class);
        assertEquals(SC_INTERNAL_SERVER_ERROR, gatewayResponse.getStatusCode());
    }

    @Test
    public void handlerShouldReturnBadGatewayErrorWhenDoiProxyServiceReturnsEmptyResult()
        throws InterruptedException, URISyntaxException, NoContentLocationFoundException, IOException, TransformFailedException {
        PublicationConverter publicationConverter = mockPublicationConverter();
        DoiTransformService doiTransforService = mockDoiTransformService();
        DoiProxyService doiProxyService = mockDoiProxyServiceReturningEmptyResult();
        ResourcePersistenceService resourcePersistenceService = mock(ResourcePersistenceService.class);

        MainHandler handler = new MainHandler(jsonParser, publicationConverter, doiTransforService, doiProxyService,
            resourcePersistenceService, environment);
        OutputStream outputStream = outputStream();
        handler.handleRequest(mainHandlerInputStream(), outputStream, getMockContext());
        GatewayResponse<String> response = gatewayResponse(outputStream);
        assertThat(response.getStatusCode(),is(equalTo(Status.BAD_GATEWAY.getStatusCode())));
        assertThat(response.getBody(),containsString(MainHandler.ERROR_READING_METADATA));
    }

    private DoiProxyService mockDoiProxyServiceReturningEmptyResult()
        throws InterruptedException, IOException, NoContentLocationFoundException, URISyntaxException {
        DoiProxyService service = mock(DoiProxyService.class);
        when(service.lookup(any(), anyString(), anyString())).thenReturn(Optional.empty());
        return service;
    }

//    @Test
//    public void testBadGatewayErrorResponse()
//        throws IOException, InterruptedException, TransformFailedException, URISyntaxException {
//        PublicationConverter publicationConverter = mock(PublicationConverter.class);
//        when(publicationConverter.toSummary(any())).thenThrow(new MetadataNotFoundException("message"));
//        DoiTransformService doiTransformService = mockDoiTransformService();
//        DoiProxyService doiProxyService = mock(DoiProxyService.class);
//        ResourcePersistenceService resourcePersistenceService = mock(ResourcePersistenceService.class);
//        Context context = getMockContext();
//        MainHandler mainHandler = new MainHandler(objectMapper, publicationConverter, doiTransformService,
//            doiProxyService, resourcePersistenceService, environment);
//        ;
//
//        mainHandler.handleRequest(inputStream(), output, context);
//
//        GatewayResponse gatewayResponse =
//            assertEquals(SC_BAD_GATEWAY, gatewayResponse.getStatusCode());
//    }

    private Context getMockContext() {
        Context context = mock(Context.class);
        CognitoIdentity cognitoIdentity = mock(CognitoIdentity.class);
        when(context.getIdentity()).thenReturn(cognitoIdentity);
        when(cognitoIdentity.getIdentityPoolId()).thenReturn("junit");
        return context;
    }

    private InputStream mainHandlerInputStream() throws MalformedURLException, JsonProcessingException {
        Map<String,Object> event = new ConcurrentHashMap<>();
        RequestBody requestBody = new RequestBody();
        requestBody.setDoiUrl(new URL("https://somedoi.org"));
        event.put("body",objectMapper.writeValueAsString(requestBody));
        event.put("headers",mainHandlerRequestHeaders());
        return new ByteArrayInputStream(objectMapper.writeValueAsBytes(event));
    }

    private InputStream inputStream() throws IOException {
        Map<String, Object> event = new ConcurrentHashMap<>();
        String body = new String(Files.readAllBytes(Paths.get("src/test/resources/example_publication.json")));
        event.put("body", body);
        Map<String, String> headers = new ConcurrentHashMap<>();
        headers.put(AUTHORIZATION, "some api key");
        headers.put(CONTENT_TYPE, ContentType.APPLICATION_JSON.getMimeType());
        event.put("headers", headers);
        return new ByteArrayInputStream(objectMapper.writeValueAsBytes(event));
    }

    private Map<String,String> mainHandlerRequestHeaders() {
        Map<String, String> headers = new ConcurrentHashMap<>();
        headers.put(AUTHORIZATION,"some api key");
        headers.put(CONTENT_TYPE, ContentType.APPLICATION_JSON.getMimeType());
        return headers;
    }

    private OutputStream outputStream() {
        return new ByteArrayOutputStream();
    }

    private GatewayResponse gatewayResponse(OutputStream outputStream) throws JsonProcessingException {
        return objectMapper.readValue(outputStream.toString(), GatewayResponse.class);
    }
}
