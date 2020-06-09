package no.unit.nva.doi.transformer;

import static java.util.Collections.singletonMap;
import static no.unit.nva.doi.transformer.MainHandler.ACCESS_CONTROL_ALLOW_ORIGIN;
import static no.unit.nva.model.util.OrgNumberMapper.UNIT_ORG_NUMBER;
import static org.apache.http.HttpHeaders.CONTENT_TYPE;
import static org.apache.http.HttpStatus.SC_BAD_REQUEST;
import static org.apache.http.HttpStatus.SC_INTERNAL_SERVER_ERROR;
import static org.apache.http.HttpStatus.SC_OK;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.amazonaws.services.lambda.runtime.Context;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import no.unit.nva.doi.transformer.model.crossrefmodel.CrossRefDocument;
import no.unit.nva.doi.transformer.model.crossrefmodel.CrossrefApiResponse;
import no.unit.nva.doi.transformer.model.internal.external.DataciteResponse;
import no.unit.nva.doi.transformer.utils.TestLambdaLogger;
import no.unit.nva.model.Publication;
import no.unit.nva.model.exceptions.InvalidIssnException;
import no.unit.nva.model.exceptions.InvalidPageTypeException;
import nva.commons.utils.IoUtils;
import org.apache.http.HttpHeaders;
import org.apache.http.entity.ContentType;
import org.junit.Assert;
import org.junit.contrib.java.lang.system.EnvironmentVariables;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

public class MainHandlerTest extends ConversionTest {

    public static final String SAMPLE_CROSSREF_FILE = "crossref.json";
    private static final UUID SOME_UUID = UUID.randomUUID();
    private static final String SOME_OWNER = "SomeOwner";
    public static final String DATACITE_RESPONSE_JSON = "datacite_response.json";

    private ObjectMapper objectMapper = MainHandler.createObjectMapper();
    public EnvironmentVariables environmentVariables;
    private Environment environment;

    /**
     * setup.
     */
    @BeforeEach
    public void setUp() {
        environmentVariables = new EnvironmentVariables();
        environment = Mockito.mock(Environment.class);
        Mockito.when(environment.get("ALLOWED_ORIGIN")).thenReturn(Optional.of("*"));
    }

    @Test
    public void testOkResponse() throws IOException {
        MainHandlerWithAttachedOutputStream mainHandlerWithOutput = new MainHandlerWithAttachedOutputStream();
        OutputStream output = mainHandlerWithOutput.getOutput();
        mainHandlerWithOutput.handleRequest(inputStream());

        GatewayResponse gatewayResponse = objectMapper.readValue(output.toString(), GatewayResponse.class);
        assertEquals(SC_OK, gatewayResponse.getStatusCode());
        Assert.assertTrue(gatewayResponse.getHeaders().keySet().contains(CONTENT_TYPE));
        Assert.assertTrue(gatewayResponse.getHeaders().keySet().contains(ACCESS_CONTROL_ALLOW_ORIGIN));
        Publication publication = objectMapper.readValue(gatewayResponse.getBody().toString(), Publication.class);
        assertEquals(DataciteResponseConverter.DEFAULT_NEW_PUBLICATION_STATUS, publication.getStatus());
    }

    @Test
    public void testBadRequestresponse() throws IOException {
        MainHandlerWithAttachedOutputStream mainHandlerWithOutput = new MainHandlerWithAttachedOutputStream();
        OutputStream output = mainHandlerWithOutput.getOutput();

        mainHandlerWithOutput.handleRequest(new ByteArrayInputStream(new byte[0]));

        GatewayResponse gatewayResponse = objectMapper.readValue(output.toString(), GatewayResponse.class);
        assertEquals(SC_BAD_REQUEST, gatewayResponse.getStatusCode());
    }

    @Test
    public void testInternalServerErrorResponse() throws IOException, URISyntaxException, InvalidPageTypeException,
            InvalidIssnException {
        DataciteResponseConverter dataciteConverter = mock(DataciteResponseConverter.class);
        CrossRefConverter crossRefConverter = new CrossRefConverter();

        when(dataciteConverter
            .toPublication(any(DataciteResponse.class), any(Instant.class), any(UUID.class), anyString(),
                any(URI.class))).thenThrow(new RuntimeException("Fail"));
        Context context = getMockContext();
        MainHandler mainHandler = new MainHandler(objectMapper, dataciteConverter, crossRefConverter, environment);
        OutputStream output = new ByteArrayOutputStream();

        mainHandler.handleRequest(inputStream(), output, context);

        GatewayResponse gatewayResponse = objectMapper.readValue(output.toString(), GatewayResponse.class);
        assertEquals(SC_INTERNAL_SERVER_ERROR, gatewayResponse.getStatusCode());
    }

    @Test
    public void convertInputToPublicationShouldParseCrossrefWhenMetadataLocationIsCrossRef()
            throws IOException, URISyntaxException, InvalidIssnException, InvalidPageTypeException {

        PublicationTransformer publicationTransformer = new PublicationTransformer();
        String jsonString = IoUtils.stringFromResources(Paths.get(SAMPLE_CROSSREF_FILE));
        Instant now = Instant.now();

        Publication actualPublication = publicationTransformer
            .convertInputToPublication(jsonString, MetadataLocation.CROSSREF.getValue(), now, SOME_OWNER, SOME_UUID,
                SOME_PUBLISHER_URI);

        Publication expectedPublication = createPublicationUsingCrossRefConverterDirectly(jsonString, now);
        assertThat(actualPublication, is(equalTo(expectedPublication)));
    }

    @Test
    public void convertInputToPublicationShouldParseDataciteWhenMetadataLocationIsDatacite()
            throws IOException, URISyntaxException, InvalidIssnException, InvalidPageTypeException {

        PublicationTransformer transformer = new PublicationTransformer();
        String jsonString = IoUtils.stringFromResources(Paths.get(DATACITE_RESPONSE_JSON));
        Instant now = Instant.now();

        Publication actualPublication = transformer
            .convertInputToPublication(jsonString, MetadataLocation.DATACITE.getValue(), now, SOME_OWNER, SOME_UUID,
                SOME_PUBLISHER_URI);

        Publication expectedPublication = createPublicationUsingDataciteConverterDirectly(jsonString, now);
        assertThat(actualPublication, is(equalTo(expectedPublication)));
    }

    @Test
    public void hanldeRequestAcceptsHeadersAsLists() throws IOException {

        MainHandlerWithAttachedOutputStream mainHandlerWithOutput = new MainHandlerWithAttachedOutputStream();
        OutputStream output = mainHandlerWithOutput.getOutput();

        mainHandlerWithOutput.handleRequest(inputStreamWithHeadersAsLists());

        GatewayResponse gatewayResponse = objectMapper.readValue(output.toString(), GatewayResponse.class);
        assertEquals(SC_OK, gatewayResponse.getStatusCode());
        Assert.assertTrue(gatewayResponse.getHeaders().keySet().contains(CONTENT_TYPE));
        Assert.assertTrue(gatewayResponse.getHeaders().keySet().contains(ACCESS_CONTROL_ALLOW_ORIGIN));
        Publication publication = objectMapper.readValue(gatewayResponse.getBody().toString(), Publication.class);
        assertEquals(DataciteResponseConverter.DEFAULT_NEW_PUBLICATION_STATUS, publication.getStatus());
    }

    private Publication createPublicationUsingCrossRefConverterDirectly(String jsonString, Instant now)
            throws com.fasterxml.jackson.core.JsonProcessingException, InvalidIssnException, InvalidPageTypeException {
        CrossRefDocument doc = objectMapper.readValue(jsonString, CrossrefApiResponse.class).getMessage();
        return new CrossRefConverter()
                .toPublication(doc, now, SOME_OWNER, SOME_UUID, SOME_PUBLISHER_URI);
    }

    private Publication createPublicationUsingDataciteConverterDirectly(String jsonString, Instant now)
            throws com.fasterxml.jackson.core.JsonProcessingException, URISyntaxException, InvalidPageTypeException,
            InvalidIssnException {
        DataciteResponse doc = objectMapper.readValue(jsonString, DataciteResponse.class);
        return new DataciteResponseConverter().toPublication(doc, now, SOME_UUID, SOME_OWNER, SOME_PUBLISHER_URI);
    }

    private Context getMockContext() {
        Context context = mock(Context.class);
        when(context.getLogger()).thenReturn(new TestLambdaLogger());
        return context;
    }

    private InputStream inputStream() throws IOException {
        Map<String, Object> event = new HashMap<>();
        String body = new String(Files.readAllBytes(Paths.get("src/test/resources/datacite_response2.json")));
        event.put("requestContext", singletonMap("authorizer",
            singletonMap("claims", Map.of("custom:feideId", "junit", "custom:orgNumber", UNIT_ORG_NUMBER))));
        event.put("body", body);

        addHeaders(event);

        return new ByteArrayInputStream(objectMapper.writeValueAsBytes(event));
    }

    private InputStream inputStreamWithHeadersAsLists() throws IOException {
        Map<String, Object> event = new HashMap<>();
        String body = new String(Files.readAllBytes(Paths.get("src/test/resources/datacite_response2.json")));
        event.put("requestContext", singletonMap("authorizer",
            singletonMap("claims", Map.of("custom:feideId", "junit", "custom:orgNumber", UNIT_ORG_NUMBER))));
        event.put("body", body);

        addHeadersAsList(event);

        return new ByteArrayInputStream(objectMapper.writeValueAsBytes(event));
    }

    private void addHeaders(Map<String, Object> event) {
        Map<String, String> headers = new HashMap<>();
        headers.put(CONTENT_TYPE, ContentType.APPLICATION_JSON.getMimeType());
        headers.put(HttpHeaders.CONTENT_LOCATION, MetadataLocation.DATACITE.getValue());
        event.put("headers", headers);
    }

    private void addHeadersAsList(Map<String, Object> event) {
        Map<String, Object> headers = new HashMap<>();
        headers.put(CONTENT_TYPE, Collections.singleton(ContentType.APPLICATION_JSON.getMimeType()));
        headers.put(HttpHeaders.CONTENT_LOCATION, Collections.singletonList(MetadataLocation.DATACITE.getValue()));
        event.put("headers", headers);
    }

    private class MainHandlerWithAttachedOutputStream {

        private Context context;
        private MainHandler mainHandler;
        private OutputStream output;

        public MainHandlerWithAttachedOutputStream() {
            DataciteResponseConverter dataciteConverter = new DataciteResponseConverter();
            CrossRefConverter crossRefConverter = new CrossRefConverter();
            context = getMockContext();
            mainHandler = new MainHandler(objectMapper, dataciteConverter, crossRefConverter, environment);
            output = new ByteArrayOutputStream();
        }

        public Context getContext() {
            return context;
        }

        public MainHandler getMainHandler() {
            return mainHandler;
        }

        public OutputStream getOutput() {
            return output;
        }

        public void handleRequest(InputStream input) throws IOException {
            mainHandler.handleRequest(input, output, context);
        }
    }
}
