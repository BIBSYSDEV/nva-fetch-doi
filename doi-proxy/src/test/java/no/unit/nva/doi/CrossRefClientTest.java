package no.unit.nva.doi;

import static no.unit.nva.doi.CrossRefClient.CROSSREFPLUSAPITOKEN_KEY_ENV;
import static no.unit.nva.doi.CrossRefClient.CROSSREFPLUSAPITOKEN_NAME_ENV;
import static no.unit.nva.doi.CrossRefClient.CROSSREF_SECRETS_NOT_FOUND;
import static no.unit.nva.doi.CrossRefClient.CROSSREF_USER_AGENT;
import static no.unit.nva.doi.CrossRefClient.MISSING_CROSSREF_TOKENS_ERROR_MESSAGE;
import static no.unit.nva.doi.CrossRefClient.WORKS;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import no.unit.nva.doi.utils.HttpResponseStatus200;
import no.unit.nva.doi.utils.HttpResponseStatus404;
import no.unit.nva.doi.utils.HttpResponseStatus500;
import no.unit.nva.doi.utils.MockHttpClient;
import nva.commons.core.Environment;
import nva.commons.core.ioutils.IoUtils;
import nva.commons.secrets.ErrorReadingSecretException;
import nva.commons.secrets.SecretsReader;
import org.apache.http.HttpHeaders;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

public class CrossRefClientTest {

    public static final String DOI_STRING = "10.1007/s00115-004-1822-4";
    public static final String DOI_DX_URL_PREFIX = "https://dx.doi.org";
    public static final String DOI_URL_PREFIX = "https://doi.org";

    public static final Path CROSS_REF_SAMPLE_PATH = Paths.get("crossRefSample.json");

    public static final String ERROR_MESSAGE = "404 error message";
    public static final String ILLEGAL_DOI_STRING = "doi:" + DOI_STRING;
    public static final String HTTPS = "https";
    public static final String HTTP_DOI_URI = "http://doi.dx.org/10.000/0001";

    private CrossRefClient crossRefClient;
    private Environment environment;
    private SecretsReader secretsReader;

    @BeforeEach
    void before() throws ErrorReadingSecretException {
        final HttpClient httpClient = mockHttpClientWithNonEmptyResponse();
        secretsReader = mock(SecretsReader.class);
        environment = mock(Environment.class);
        setUpPassingSecretsManager();
        crossRefClient = new CrossRefClient(httpClient, environment, secretsReader);
    }

    @DisplayName("createTargetUrl returns a valid Url for DOI strings that are not DOI URLs")
    @Test
    void createTargetUrlReturnsAValidUrlForDoiStringThatIsNotDoiURL()
            throws URISyntaxException {
        String expected = String.join("/", CrossRefClient.CROSSREF_LINK, WORKS, DOI_STRING);
        String output = crossRefClient.createUrlToCrossRef(DOI_STRING).toString();
        assertThat(output, is(equalTo(expected)));
    }

    @DisplayName("Requests to Crossref are made politely with https")
    @Test
    void crossRefClientIsConfiguredToUseHttps() throws URISyntaxException {
        var crossRefUri = crossRefClient.createUrlToCrossRef(HTTP_DOI_URI);
        assertThat(crossRefUri.getScheme(), equalTo(HTTPS));
    }

    @DisplayName("Requests to Crossref are made politely with user agent")
    @Test
    void crossRefHttpClientIsConfiguredToUseUserAgent() throws URISyntaxException {
        var responseBody = IoUtils.stringFromResources(CROSS_REF_SAMPLE_PATH);
        var httpClient = new MockHttpClient<>(new HttpResponseStatus200<>(responseBody));
        var crossRefClient = new CrossRefClient(httpClient, environment, secretsReader);
        crossRefClient.fetchDataForDoi(DOI_STRING);
        var httpRequest = httpClient.getHttpRequest();
        var headers = httpRequest.headers().map();
        assertTrue(headers.containsKey(HttpHeaders.USER_AGENT));
        assertTrue(headers.get(HttpHeaders.USER_AGENT).contains(CROSSREF_USER_AGENT));
    }

    @DisplayName("createTargetUrl returns a valid Url for DOI strings that are DOI DX URLs")
    @Test
    void createTargetUrlReturnsAValidUrlForDoiStringThatIsDoiDxUrl()
            throws URISyntaxException {
        targetURlReturnsAValidUrlForDoiStrings(DOI_DX_URL_PREFIX);
    }

    @DisplayName("createTargetUrl returns a valid Url for DOI strings that are DOI URLs")
    @Test
    void createTargetUrlReturnsAValidUrlForDoiStringThatIsDoiURL()
            throws URISyntaxException {
        targetURlReturnsAValidUrlForDoiStrings(DOI_URL_PREFIX);
    }

    @Test
    @DisplayName("fetchDataForDoi returns an Optional with a json object for an existing URL")
    void fetchDataForDoiReturnAnOptionalWithAJsonObjectForAnExistingUrl()
            throws URISyntaxException {
        Optional<String> result = crossRefClient.fetchDataForDoi(DOI_STRING).map(MetadataAndContentLocation::getJson);
        String expected = IoUtils.stringFromResources(CROSS_REF_SAMPLE_PATH);
        assertThat(result.isPresent(), is(true));
        assertThat(result.get(), is(equalTo(expected)));
    }

    @Test
    @DisplayName("fetchDataForDoi returns an empty Optional for a non existing URL")
    void fetchDataForDoiReturnAnEmptyOptionalForANonExistingUrl()
            throws URISyntaxException {

        CrossRefClient crossRefClient = crossRefClientReceives404();

        Optional<String> result = crossRefClient.fetchDataForDoi(DOI_STRING).map(MetadataAndContentLocation::getJson);
        assertThat(result.isEmpty(), is(true));
    }

    @Test
    @DisplayName("fetchDataForDoi returns an empty Optional for an unknown error")
    void fetchDataForDoiReturnAnEmptyOptionalForAnUnknownError()
            throws URISyntaxException {
        CrossRefClient crossRefClient = crossRefClientReceives500();
        Optional<String> result = crossRefClient.fetchDataForDoi(DOI_STRING).map(MetadataAndContentLocation::getJson);
        assertTrue(result.isEmpty());
    }

    @Test
    void fetchDataForDoiThrowsExceptionWhenDoiHasInvalidFormat() {
        assertThrows(IllegalArgumentException.class, () -> crossRefClient.fetchDataForDoi(ILLEGAL_DOI_STRING));
    }

    @Test
    void crossrefClientThrowsRuntimeExceptionIfEnvironmentVariableCrossrefApiTokenNameIsMissing()
            throws ErrorReadingSecretException {
        Environment mockEnvironment = mock(Environment.class);
        when(mockEnvironment.readEnvOpt(CROSSREFPLUSAPITOKEN_KEY_ENV)).thenReturn(Optional.of("anything"));
        SecretsReader secretsReader = mock(SecretsReader.class);
        when(secretsReader.fetchSecret(anyString(), anyString())).thenReturn("happy", "feet");
        Executable executable = () -> new CrossRefClient(HttpClient.newHttpClient(), mockEnvironment,
                secretsReader);
        Exception exception = assertThrows(RuntimeException.class, executable);
        String actual = exception.getMessage();
        assertThat(actual, equalTo(MISSING_CROSSREF_TOKENS_ERROR_MESSAGE));
    }

    @Test
    void crossrefClientThrowsRuntimeExceptionIfEnvironmentVariableCrossrefApiTokenKeyIsMissing()
            throws ErrorReadingSecretException {
        Environment mockEnvironment = mock(Environment.class);
        when(mockEnvironment.readEnvOpt(CROSSREFPLUSAPITOKEN_NAME_ENV)).thenReturn(Optional.of("anything"));
        SecretsReader secretsReader = mock(SecretsReader.class);
        when(secretsReader.fetchSecret(anyString(), anyString())).thenReturn("happy", "feet");
        Executable executable = () -> new CrossRefClient(HttpClient.newHttpClient(), mockEnvironment,
                secretsReader);
        Exception exception = assertThrows(RuntimeException.class, executable);
        String actual = exception.getMessage();
        assertThat(actual, containsString(MISSING_CROSSREF_TOKENS_ERROR_MESSAGE));
    }

    @Test
    void crossrefClientThrowsRuntimeExceptionIfEnvironmentVariableCrossrefApiTokensAreMissing()
            throws ErrorReadingSecretException {
        Environment mockEnvironment = mock(Environment.class);
        SecretsReader secretsReader = mock(SecretsReader.class);
        when(secretsReader.fetchSecret(anyString(), anyString())).thenReturn("happy", "feet");
        Executable executable = () -> new CrossRefClient(HttpClient.newHttpClient(), mockEnvironment,
                secretsReader);
        Exception exception = assertThrows(RuntimeException.class, executable);
        String actual = exception.getMessage();
        assertThat(actual, containsString(MISSING_CROSSREF_TOKENS_ERROR_MESSAGE));
    }

    @Test
    void crossrefClientThrowsRuntimeExceptionWhenSecretsManagerLacksSecret() throws ErrorReadingSecretException {
        SecretsReader secretsReader = mock(SecretsReader.class);
        when(environment.readEnvOpt(CROSSREFPLUSAPITOKEN_NAME_ENV)).thenReturn(Optional.of("name"));
        when(environment.readEnvOpt(CROSSREFPLUSAPITOKEN_KEY_ENV)).thenReturn(Optional.of("key"));
        when(secretsReader.fetchSecret("name", "key")).thenThrow(ErrorReadingSecretException.class);

        Executable executable = () -> new CrossRefClient(mockHttpClientWithNonEmptyResponse(),
                environment, secretsReader).fetchDataForDoi(DOI_STRING);
        Exception exception = assertThrows(RuntimeException.class, executable);
        String actual = exception.getMessage();
        assertThat(actual, containsString(CROSSREF_SECRETS_NOT_FOUND));
    }

    @SuppressWarnings("unchecked")
    @Test
    void fetchDataForDoiReturnsNotFoundWhenInputDoiDoesNotDereference() throws URISyntaxException {
        HttpClient httpClient = mock(HttpClient.class);
        var httpResponse = new HttpResponseStatus404<>("Not found");
        CompletableFuture<HttpResponse<String>> completableFuture = CompletableFuture.supplyAsync(() -> httpResponse);
        when(httpClient.sendAsync(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenReturn(completableFuture);
        Optional<MetadataAndContentLocation> actual = new CrossRefClient(httpClient, environment, secretsReader)
                .fetchDataForDoi(DOI_STRING);
        assertThat(actual, equalTo(Optional.empty()));
    }

    private void setUpPassingSecretsManager() throws ErrorReadingSecretException {
        when(environment.readEnvOpt(any())).thenReturn(Optional.of("ok"));
        when(secretsReader.fetchSecret(anyString(), anyString())).thenReturn("ok");
    }

    private void targetURlReturnsAValidUrlForDoiStrings(String doiPrefix)
            throws URISyntaxException {
        String doiURL = String.join("/", doiPrefix, DOI_STRING);
        String expected = String.join("/", CrossRefClient.CROSSREF_LINK, WORKS, DOI_STRING);

        String output = crossRefClient.createUrlToCrossRef(doiURL).toString();
        assertThat(output, is(equalTo(expected)));
    }

    private HttpClient mockHttpClientWithNonEmptyResponse() {
        String responseBody = IoUtils.stringFromResources(CROSS_REF_SAMPLE_PATH);
        HttpResponseStatus200<String> response = new HttpResponseStatus200<>(responseBody);
        return new MockHttpClient<>(response);
    }

    private CrossRefClient crossRefClientReceives404() {
        HttpResponseStatus404<String> errorResponse = new HttpResponseStatus404<>(
                ERROR_MESSAGE);
        MockHttpClient<String> mockHttpClient = new MockHttpClient<>(errorResponse);
        return new CrossRefClient(mockHttpClient, environment, secretsReader);
    }

    private CrossRefClient crossRefClientReceives500() {
        HttpResponseStatus500<String> errorResponse = new HttpResponseStatus500<>(
                ERROR_MESSAGE);
        MockHttpClient<String> mockHttpClient = new MockHttpClient<>(errorResponse);
        return new CrossRefClient(mockHttpClient, environment, secretsReader);
    }
}
