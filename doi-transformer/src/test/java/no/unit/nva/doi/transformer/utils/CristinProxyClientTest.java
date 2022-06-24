package no.unit.nva.doi.transformer.utils;

import static no.unit.nva.doi.transformer.utils.CristinProxyClient.API_HOST_ENV_KEY;
import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;
import java.net.URI;
import java.net.http.HttpClient;
import java.nio.file.Path;
import java.nio.file.Paths;
import nva.commons.core.Environment;
import nva.commons.core.ioutils.IoUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class CristinProxyClientTest {

    private static final String API_HOST_VALUE = "api.dev.nva.aws.unit.no";
    private static final String SAMPLE_ORCID = "1234-1234-1234-1234";
    private static final URI SAMPLE_URI = URI.create("https://" + API_HOST_VALUE + "/cristin/person/" + SAMPLE_ORCID);
    private static final URI SAMPLE_IDENTIFIER = URI.create("https://api.dev.nva.aws.unit.no/cristin/person/738");
    private static final Path CRISTIN_PROXY_VALID_JSON = Paths.get("cristin_proxy_person_sample.json");
    private static final int HTTP_STATUS_CODE_BELOW_OK = 100;

    private CristinProxyClient cristinProxyClient;
    private Environment environment;

    @BeforeEach
    void before() {
        HttpClient httpClient = mockHttpClientWithNonEmptyResponse();
        environment = mock(Environment.class);
        when(environment.readEnv(API_HOST_ENV_KEY)).thenReturn(API_HOST_VALUE);

        cristinProxyClient = new CristinProxyClient(httpClient, environment);
    }

    @Test
    void shouldReturnIdentifierWhenIdentifierMatchingOrcidWasFoundInUpstream() {
        var result = cristinProxyClient.lookupIdentifierFromOrcid(SAMPLE_ORCID);

        assertThat(result.isPresent(), equalTo(true));
        assertThat(result.get(), equalTo(SAMPLE_IDENTIFIER));
    }

    @Test
    void shouldReturnEmptyOptionalInsteadOfIdentifierWhenUpstreamHasNoMatchOnGivenOrcid() {
        var cristinProxyClient = cristinProxyClientReceives404();
        var result = cristinProxyClient.lookupIdentifierFromOrcid(SAMPLE_ORCID);

        assertThat(result.isEmpty(), equalTo(true));
    }

    @Test
    void shouldReturnEmptyOptionalInsteadOfIdentifierWhenUpstreamReturnsError() {
        var cristinProxyClient = cristinProxyClientReceives500();
        var result = cristinProxyClient.lookupIdentifierFromOrcid(SAMPLE_ORCID);

        assertThat(result.isEmpty(), equalTo(true));
    }

    @Test
    void shouldReturnEmptyOptionalInsteadOfIdentifierWhenUpstreamReturnsStatusCodeBelowOkRange() {
        var cristinProxyClient = cristinProxyClientReceives100();
        var result = cristinProxyClient.lookupIdentifierFromOrcid(SAMPLE_ORCID);

        assertThat(result.isEmpty(), equalTo(true));
    }

    @Test
    void shouldReturnEmptyOptionalInsteadOfIdentifierWhenResponseFromUpstreamHasInvalidJson() {
        var httpClient = mockHttpClientWithErrorInResponseJson();
        var cristinProxyClient = new CristinProxyClient(httpClient, environment);
        var result = cristinProxyClient.lookupIdentifierFromOrcid(SAMPLE_ORCID);

        assertThat(result.isEmpty(), equalTo(true));
    }

    @Test
    void shouldReturnEmptyOptionalInsteadOfIdentifierWhenResponseFromUpstreamIsMissingIdentifier() {
        var httpClient = mockHttpClientWithMissingIdentifierField();
        var cristinProxyClient = new CristinProxyClient(httpClient, environment);
        var result = cristinProxyClient.lookupIdentifierFromOrcid(SAMPLE_ORCID);

        assertThat(result.isEmpty(), equalTo(true));
    }

    @Test
    void shouldThrowIllegalArgumentWhenOrcidHasInvalidFormat() {
        assertThrows(IllegalArgumentException.class,
                     () -> cristinProxyClient.lookupIdentifierFromOrcid(randomString()));
    }

    @Test
    void shouldProduceCorrectUrlToCristinProxyFromSampleOrcid() {
        assertEquals(SAMPLE_URI, cristinProxyClient.createUrlToCristinProxy(SAMPLE_ORCID));
    }

    @Test
    void shouldThrowIllegalArgumentWhenInputIsNull() {
        assertThrows(IllegalArgumentException.class, () -> cristinProxyClient.lookupIdentifierFromOrcid(null));
    }

    @Test
    void shouldNotProduceExceptionWhenExtractingFromInvalidJsonIdentifier() {
        cristinProxyClient.extractIdentifierFromResponse("\"id\": \"Hello\"");
        cristinProxyClient.extractIdentifierFromResponse(null);
    }

    private HttpClient mockHttpClientWithNonEmptyResponse() {
        var responseBody = IoUtils.stringFromResources(CRISTIN_PROXY_VALID_JSON);
        var response = new HttpResponseStatus200<>(responseBody);
        return new MockHttpClient<>(response);
    }

    private CristinProxyClient cristinProxyClientReceives404() {
        var errorResponse = new HttpResponseStatus404<>(randomString());
        var mockHttpClient = new MockHttpClient<>(errorResponse);
        return new CristinProxyClient(mockHttpClient, environment);
    }

    private CristinProxyClient cristinProxyClientReceives500() {
        var errorResponse = new HttpResponseStatus500<>(randomString());
        var mockHttpClient = new MockHttpClient<>(errorResponse);
        return new CristinProxyClient(mockHttpClient, environment);
    }

    private CristinProxyClient cristinProxyClientReceives100() {
        var errorResponse = new HttpResponseStatus500<>(randomString());
        errorResponse = spy(errorResponse);
        when(errorResponse.statusCode()).thenReturn(HTTP_STATUS_CODE_BELOW_OK);
        var mockHttpClient = new MockHttpClient<>(errorResponse);
        return new CristinProxyClient(mockHttpClient, environment);
    }

    private HttpClient mockHttpClientWithErrorInResponseJson() {
        var responseBody = IoUtils.stringFromResources(CRISTIN_PROXY_VALID_JSON)
                               .replaceAll(",", ".");
        var response = new HttpResponseStatus200<>(responseBody);
        return new MockHttpClient<>(response);
    }

    private HttpClient mockHttpClientWithMissingIdentifierField() {
        var responseBody = IoUtils.stringFromResources(CRISTIN_PROXY_VALID_JSON)
                               .replaceAll("id", "someField");
        var response = new HttpResponseStatus200<>(responseBody);
        return new MockHttpClient<>(response);
    }

}
