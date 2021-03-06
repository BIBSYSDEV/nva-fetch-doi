package no.unit.nva.doi.transformer.utils;

import nva.commons.core.Environment;
import nva.commons.core.ioutils.IoUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;

import static no.unit.nva.doi.transformer.utils.BareProxyClient.BARE_PROXY_API_URI_ENV_KEY;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class BareProxyClientTest {

    public static final String ERROR_MESSAGE = "404 error message";
    private static final String BARE_PROXY_API_URI = "https://api.dev.nva.aws.unit.no";
    private static final Path BARE_PROXY_SAMPLE_PATH = Paths.get("bareproxysample.json");
    private static final Path BARE_PROXY_ERRONEOUS_JSON_SAMPLE_PATH = Paths.get("bareproxyerroneoussample.json");
    private static final String SAMPLE_ARPID = "https://api.dev.nva.aws.unit.no/person/1600776277420";
    private static final String SAMPLE_ORCID = "https://sandbox.orcid.org/0000-0002-8617-3281";
    private static final String ILLEGAL_ORCID = "hptts:??sandbox.orkid.org\"42";
    private BareProxyClient bareProxyClient;
    private Environment environment;

    /**
     * Set up environment.
     */
    @BeforeEach
    void before() throws IOException {
        HttpClient httpClient = mockHttpClientWithNonEmptyResponse();
        environment = mock(Environment.class);
        when(environment.readEnv(BARE_PROXY_API_URI_ENV_KEY)).thenReturn(BARE_PROXY_API_URI);

        bareProxyClient = new BareProxyClient(httpClient, environment);
    }

    private HttpClient mockHttpClientWithNonEmptyResponse() throws IOException {
        String responseBody = IoUtils.stringFromResources(BARE_PROXY_SAMPLE_PATH);
        HttpResponseStatus200<String> response = new HttpResponseStatus200<>(responseBody);
        return new MockHttpClient<>(response);
    }

    private HttpClient mockHttpClientWithNonEmptyResponseWithJsonErrors() throws IOException {
        String responseBody = IoUtils.stringFromResources(BARE_PROXY_ERRONEOUS_JSON_SAMPLE_PATH);
        HttpResponseStatus200<String> response = new HttpResponseStatus200<>(responseBody);
        return new MockHttpClient<>(response);
    }


    @Test
    @DisplayName("fetchAuthorityDataForOrcid returns an Optional apr identifier for an existing Orcid (URL)")
    public void fetchAuthorityDataForOrcidReturnAnOptionalIdentifierForAnExistingOrcid() throws URISyntaxException {
        Optional<String> result = bareProxyClient.lookupArpidForOrcid(SAMPLE_ORCID);
        assertThat(result.isPresent(), is(true));
        assertThat(result.get(), is(equalTo(SAMPLE_ARPID)));
    }

    @Test
    @DisplayName("fetchDataForDoi returns an empty Optional for a non existing URL")
    public void fetchDataForDoiReturnAnEmptyOptionalForANonExistingUrl() throws URISyntaxException {

        BareProxyClient bareProxyClient = bareProxyClientReceives404();

        Optional<String> result = bareProxyClient.lookupArpidForOrcid(SAMPLE_ORCID);
        assertThat(result.isEmpty(), is(true));
    }

    @Test
    @DisplayName("fetchDataForDoi returns an empty Optional for an unknown error")
    public void fetchDataForDoiReturnAnEmptyOptionalForAnUnknownError() throws URISyntaxException {
        BareProxyClient bareProxyClient = bareProxyClientReceives500();
        Optional<String> result = bareProxyClient.lookupArpidForOrcid(SAMPLE_ORCID);
        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("fetchDataForDoi returns an empty Optional when bare response has errors")
    public void fetchDataForDoiReturnAnEmptyOptionalForAnBareJsonSyntaxError() throws URISyntaxException, IOException {
        HttpClient httpClient = mockHttpClientWithNonEmptyResponseWithJsonErrors();
        bareProxyClient = new BareProxyClient(httpClient, environment);
        Optional<String> result = bareProxyClient.lookupArpidForOrcid(SAMPLE_ORCID);
        assertTrue(result.isEmpty());
    }

    @Test
    public void fetchDataForDoiThrowsExceptionWhenDoiHasInvalidFormat() {
        assertThrows(IllegalArgumentException.class,
            () -> bareProxyClient.lookupArpidForOrcid(ILLEGAL_ORCID));
    }

    private BareProxyClient bareProxyClientReceives404() {
        HttpResponseStatus404<String> errorResponse = new HttpResponseStatus404<>(ERROR_MESSAGE);
        MockHttpClient<String> mockHttpClient = new MockHttpClient<>(errorResponse);
        return new BareProxyClient(mockHttpClient, environment);
    }

    private BareProxyClient bareProxyClientReceives500() {
        HttpResponseStatus500<String> errorResponse = new HttpResponseStatus500<>(ERROR_MESSAGE);
        MockHttpClient<String> mockHttpClient = new MockHttpClient<>(errorResponse);
        return new BareProxyClient(mockHttpClient, environment);
    }

}