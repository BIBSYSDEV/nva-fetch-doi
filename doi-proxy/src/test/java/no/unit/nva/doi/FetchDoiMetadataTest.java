package no.unit.nva.doi;

import static no.unit.nva.doi.DataciteContentType.CITEPROC_JSON;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import no.bibsys.aws.tools.IoUtils;
import no.unit.nva.doi.fetch.ObjectMapperConfig;
import no.unit.nva.doi.utils.AbstractLambdaTest;
import no.unit.nva.doi.utils.HttpResponseStatus200;
import no.unit.nva.doi.utils.MockHttpClient;
import org.apache.http.HttpStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

public class FetchDoiMetadataTest extends AbstractLambdaTest {

    public static final String VALID_DOI = "https://doi.org/10.1093/afraf/ady029";
    public static final String MOCK_ERROR_MESSAGE = "The test told me to fail";
    public static final String INVALID_DOI = "https://doi.org/lets^Go^Wild";
    public static final String ERROR_JSON = "{\"error\":\"error\"}";
    public static final String ERROR = "error";
    public static final String ERROR_KEY = "error";
    public static final String DATACITE_RESPONSE = "";

    public static final String VALID_DOI_WITH_DOI_PREFIX = "doi:10.123.4.5/124";
    public static final String VALID_DOI_WITHOUT_DOI_PREFIX = "10.123.4.5/124";
    public static final ObjectMapper objectMapper = ObjectMapperConfig.createObjectMapper();
    private static final CrossRefClient crossRefClient = createCrossRefClient();

    private static final CrossRefClient createCrossRefClient() {
        CrossRefClient client = mock(CrossRefClient.class);
        try {
            when(client.fetchDataForDoi(anyString())).thenReturn(Optional.empty());
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
        return client;
    }

    @Test
    public void successfulResponse() throws Exception {
        DataciteClient dataciteClient = dataciteClientWithSuccessfulResponse();
        Map<String, Object> event = createEvent(VALID_DOI, CITEPROC_JSON);

        FetchDoiMetadata fetchDoiMetadata = new FetchDoiMetadata(dataciteClient, crossRefClient);
        GatewayResponse result = fetchDoiMetadata.handleRequest(event, mockLambdaContext);

        assertEquals(Response.Status.OK.getStatusCode(), result.getStatusCode());
        assertEquals(result.getHeaders().get(HttpHeaders.CONTENT_TYPE),
                     CITEPROC_JSON.getContentType());
        String content = result.getBody();
        assertNotNull(content);
    }

    @Test
    public void testMissingAcceptHeader() throws IOException {
        DataciteClient dataciteClient = dataciteClientWithSuccessfulResponse();
        Map<String, Object> event = new HashMap<>();
        event.put("headers", Collections.emptyMap());

        FetchDoiMetadata fetchDoiMetadata = new FetchDoiMetadata(dataciteClient, crossRefClient);
        GatewayResponse result = fetchDoiMetadata.handleRequest(event, mockLambdaContext);

        assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), result.getStatusCode());
        assertEquals(result.getHeaders().get(HttpHeaders.CONTENT_TYPE), MediaType.APPLICATION_JSON);
        String content = result.getBody();
        assertNotNull(content);
        assertEquals(getErrorAsJson(FetchDoiMetadata.MISSING_ACCEPT_HEADER), content);
    }

    @Test
    public void testInvalidDoiUrl()
        throws IOException {
        DataciteClient dataciteClient = dataciteClientWithSuccessfulResponse();
        Map<String, Object> event = createEvent(INVALID_DOI, CITEPROC_JSON);

        FetchDoiMetadata fetchDoiMetadata = new FetchDoiMetadata(dataciteClient, crossRefClient);
        GatewayResponse result = fetchDoiMetadata.handleRequest(event, mockLambdaContext);

        assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), result.getStatusCode());
        assertEquals(result.getHeaders().get(HttpHeaders.CONTENT_TYPE), MediaType.APPLICATION_JSON);
        String content = result.getBody();
        assertNotNull(content);
        assertEquals(getErrorAsJson(FetchDoiMetadata.INVALID_DOI_URL), content);
    }

    @Test
    public void testCommunicationIssuesOnCallingHandler() throws Exception {
        DataciteClient dataciteClient = Mockito.mock(DataciteClient.class);
        when(dataciteClient.fetchMetadata(anyString(), any(DataciteContentType.class)))
            .thenThrow(new IOException(MOCK_ERROR_MESSAGE));
        Map<String, Object> event = createEvent(VALID_DOI, CITEPROC_JSON);

        FetchDoiMetadata fetchDoiMetadata = new FetchDoiMetadata(dataciteClient, crossRefClient);
        GatewayResponse result = fetchDoiMetadata.handleRequest(event, mockLambdaContext);

        assertEquals(Response.Status.SERVICE_UNAVAILABLE.getStatusCode(), result.getStatusCode());
        assertEquals(result.getHeaders().get(HttpHeaders.CONTENT_TYPE), MediaType.APPLICATION_JSON);
        String content = result.getBody();
        assertNotNull(content);
        assertEquals(getErrorAsJson(MOCK_ERROR_MESSAGE), content);
    }

    @Test
    public void testUnexpectedException() throws Exception {
        DataciteClient dataciteClient = Mockito.mock(DataciteClient.class);
        when(dataciteClient.fetchMetadata(anyString(), any(DataciteContentType.class)))
            .thenThrow(new NullPointerException());
        Map<String, Object> event = createEvent(VALID_DOI, CITEPROC_JSON);

        FetchDoiMetadata fetchDoiMetadata = new FetchDoiMetadata(dataciteClient, crossRefClient);
        GatewayResponse result = fetchDoiMetadata.handleRequest(event, mockLambdaContext);

        assertEquals(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), result.getStatusCode());
        assertEquals(result.getHeaders().get(HttpHeaders.CONTENT_TYPE), MediaType.APPLICATION_JSON);
        String content = result.getBody();
        assertNotNull(content);
        assertEquals(getErrorAsJson(null), content);
    }

    @Test
    public void testErrorResponse() throws JsonProcessingException {
        String errorJson = getErrorAsJson(ERROR);
        assertEquals(ERROR_JSON, errorJson);
    }

    @Test
    @DisplayName("DOI_STRING_PATTERN matches doi string starting with doi prefix")
    public void doiStringPatternMatchesDoiStringStartingWithDoiPrefix() {
        Pattern regex = FetchDoiMetadata.DOI_STRING_PATTERN;
        boolean matchingResult = regex.matcher(VALID_DOI_WITH_DOI_PREFIX).matches();
        assertThat(matchingResult, is(true));
    }

    @Test
    @DisplayName("DOI_STRING_PATTERN matches doi string starting without doi prefix")
    public void doiStringPatternMatchesDoiStringStartingWithoutDoiPrefix() {
        Pattern regex = FetchDoiMetadata.DOI_STRING_PATTERN;
        boolean matchingResult = regex.matcher(VALID_DOI_WITHOUT_DOI_PREFIX).matches();
        assertThat(matchingResult, is(true));
    }

    @Test
    @DisplayName("FetchDoiMetadata returns ContentLocation header pointing to CrossRef when reading from CrossRef")
    public void fetchDoiMetadataReturnsContentLocationHeaderPointingToCrossRefWhenReadingFromCrossRef()
        throws IOException {
        DataciteClient dataciteClient = mock(DataciteClient.class);
        CrossRefClient crossRefClient = setUpCrossRefClient();
        FetchDoiMetadata fetch = new FetchDoiMetadata(dataciteClient, crossRefClient);
        GatewayResponse response = fetch.handleRequest(createCrossRefRequest(VALID_DOI), mockLambdaContext);
        assertThat(response.getStatusCode(), is(HttpStatus.SC_OK));
        assertThat(response.getHeaders(), hasEntry(HttpHeaders.CONTENT_LOCATION, CrossRefClient.CROSSREF_LINK));
    }

    @Test
    @DisplayName("FetchDoiMetadata Returns Ok For Non Url DoiString")
    public void fetchDoiMetadataReturnsOkForNonUrlDoiString()
        throws IOException {
        DataciteClient dataciteClient = mock(DataciteClient.class);
        CrossRefClient crossRefClient = setUpCrossRefClient();
        FetchDoiMetadata fetch = new FetchDoiMetadata(dataciteClient, crossRefClient);
        GatewayResponse response = fetch
            .handleRequest(createCrossRefRequest(AbstractLambdaTest.DOI_STRING), mockLambdaContext);
        assertThat(response.getStatusCode(), is(HttpStatus.SC_OK));
    }

    @Test
    @DisplayName("FetchDoiMetadata returns ContentLocation header pointing to DataCite when reading from DataCite")
    public void fetchDoiMetadataReturnsContentLocationHeaderPointingToDataCiteWhenReadingFromDataCite()
        throws IOException {
        DataciteClient dataciteClient = dataciteClientWithSuccessfulResponse();
        CrossRefClient crossRefClient = crossRefClientReceives404();
        FetchDoiMetadata fetch = new FetchDoiMetadata(dataciteClient, crossRefClient);
        GatewayResponse response = fetch.handleRequest(createCrossRefRequest(VALID_DOI), mockLambdaContext);
        assertThat(response.getStatusCode(), is(HttpStatus.SC_OK));
        assertThat(response.getHeaders(),
            hasEntry(HttpHeaders.CONTENT_LOCATION, DataciteClient.DATACITE_BASE_URL_STRING));
    }

    @Test
    @DisplayName("FetchDoiMetadata should have a contructor without parameters")
    public void fetchDoiMetadataShouldHaveAConsturctorWithoutParameters() {
        new FetchDoiMetadata();
    }

    private CrossRefClient setUpCrossRefClient() throws IOException {
        String responseBody = IoUtils.resourceAsString(AbstractLambdaTest.CrossRefSamplePath);
        HttpClient httpClient = new MockHttpClient<String>(new HttpResponseStatus200<>(responseBody));
        return new CrossRefClient(httpClient);
    }

    private Map<String, Object> createCrossRefRequest(String doi) throws JsonProcessingException {
        Map<String, Object> event = new HashMap<>();
        Map<String, String> headers = createHeaders();
        String bodyString = createBodyString(doi);

        event.put(FetchDoiMetadata.BODY, bodyString);
        event.put(FetchDoiMetadata.HEADERS, headers);
        return event;
    }

    private Map<String, String> createHeaders() {
        Map<String, String> headers = new HashMap<>();
        headers.put(HttpHeaders.ACCEPT, CITEPROC_JSON.getContentType());
        return headers;
    }

    private String createBodyString(String doi) throws JsonProcessingException {
        DoiLookup doiLookup = new DoiLookup();
        doiLookup.setDoi(doi);
        return objectMapper.writeValueAsString(doiLookup);
    }

    private Map<String, Object> createEvent(String doiUrlString,
                                            DataciteContentType dataciteContentType) throws JsonProcessingException {
        Map<String, Object> event = new HashMap<>();
        DoiLookup doiLookup = new DoiLookup();
        doiLookup.setDoi(doiUrlString);
        event.put("body", objectMapper.writeValueAsString(doiLookup));
        event.put("headers", Collections
            .singletonMap(HttpHeaders.ACCEPT, dataciteContentType.getContentType()));
        return event;
    }

    /**
     * Get error message as a json string.
     *
     * @param message message from exception
     * @return String containing an error message as json
     */
    private String getErrorAsJson(String message) throws JsonProcessingException {
        Map errorMap = new HashMap();
        errorMap.put(ERROR_KEY, message);
        return objectMapper.writeValueAsString(errorMap);
    }

    private DataciteClient dataciteClientWithSuccessfulResponse() throws IOException {
        DataciteClient dataciteClient = Mockito.mock(DataciteClient.class);
        when(dataciteClient.fetchMetadata(anyString(), any(DataciteContentType.class)))
            .thenReturn(new MetadataAndContentLocation(DataciteClient.DATACITE_BASE_URL_STRING, DATACITE_RESPONSE));
        return dataciteClient;
    }
}
