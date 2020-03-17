package no.unit.nva.doi.fetch;

import static no.unit.nva.doi.fetch.MainHandler.ALLOWED_ORIGIN_ENV;
import static no.unit.nva.doi.fetch.MainHandler.API_HOST_ENV;
import static no.unit.nva.doi.fetch.MainHandler.API_SCHEME_ENV;
import static org.apache.http.HttpHeaders.AUTHORIZATION;
import static org.apache.http.HttpHeaders.CONTENT_TYPE;
import static org.apache.http.HttpStatus.SC_BAD_GATEWAY;
import static org.apache.http.HttpStatus.SC_BAD_REQUEST;
import static org.apache.http.HttpStatus.SC_INTERNAL_SERVER_ERROR;
import static org.apache.http.HttpStatus.SC_OK;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.amazonaws.services.lambda.runtime.CognitoIdentity;
import com.amazonaws.services.lambda.runtime.Context;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import javax.ws.rs.WebApplicationException;
import no.unit.nva.doi.fetch.model.PublicationDate;
import no.unit.nva.doi.fetch.model.Summary;
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

public class MainHandlerTest {

    private ObjectMapper objectMapper = MainHandler.createObjectMapper();

    private Environment environment;

    /**
     * Set up environment.
     */
    @Before
    public void setUp() {
        environment = Mockito.mock(Environment.class);
        when(environment.get(ALLOWED_ORIGIN_ENV)).thenReturn(Optional.of("*"));
        when(environment.get(API_HOST_ENV)).thenReturn(Optional.of("localhost:3000"));
        when(environment.get(API_SCHEME_ENV)).thenReturn(Optional.of("http"));
    }

    @Rule
    public final EnvironmentVariables environmentVariables
        = new EnvironmentVariables();

    @Test
    public void testDefaultConstructor() {
        environmentVariables.set(ALLOWED_ORIGIN_ENV, "*");
        environmentVariables.set(API_HOST_ENV, "localhost:3000");
        environmentVariables.set(API_SCHEME_ENV, "http");
        MainHandler findChannelFunctionApp = new MainHandler();
        assertNotNull(findChannelFunctionApp);
    }

    @Test
    public void testOkResponse() throws IOException {
        PublicationConverter publicationConverter = mock(PublicationConverter.class);
        when(publicationConverter.toSummary(any())).thenReturn(createSummary());
        DoiTransformService doiTransformService = mock(DoiTransformService.class);
        DoiProxyService doiProxyService = mock(DoiProxyService.class);
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
    public void testBadRequestResponse() throws IOException {
        PublicationConverter publicationConverter = mock(PublicationConverter.class);
        DoiTransformService doiTransformService = mock(DoiTransformService.class);
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
    public void testInternalServerErrorResponse() throws IOException {
        PublicationConverter publicationConverter = mock(PublicationConverter.class);
        when(publicationConverter.toSummary(any())).thenThrow(new RuntimeException());
        DoiTransformService doiTransformService = mock(DoiTransformService.class);
        DoiProxyService doiProxyService = mock(DoiProxyService.class);
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
    public void testBadGatewayErrorResponse() throws IOException {
        PublicationConverter publicationConverter = mock(PublicationConverter.class);
        when(publicationConverter.toSummary(any())).thenThrow(new WebApplicationException());
        DoiTransformService doiTransformService = mock(DoiTransformService.class);
        DoiProxyService doiProxyService = mock(DoiProxyService.class);
        ResourcePersistenceService resourcePersistenceService = mock(ResourcePersistenceService.class);
        Context context = getMockContext();
        MainHandler mainHandler = new MainHandler(objectMapper, publicationConverter, doiTransformService,
                                                  doiProxyService, resourcePersistenceService, environment);
        OutputStream output = new ByteArrayOutputStream();

        mainHandler.handleRequest(inputStream(), output, context);

        GatewayResponse gatewayResponse = objectMapper.readValue(output.toString(), GatewayResponse.class);
        assertEquals(SC_BAD_GATEWAY, gatewayResponse.getStatusCode());
    }

    private Context getMockContext() {
        Context context = mock(Context.class);
        CognitoIdentity cognitoIdentity = mock(CognitoIdentity.class);
        when(context.getIdentity()).thenReturn(cognitoIdentity);
        when(cognitoIdentity.getIdentityPoolId()).thenReturn("junit");
        return context;
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
}
