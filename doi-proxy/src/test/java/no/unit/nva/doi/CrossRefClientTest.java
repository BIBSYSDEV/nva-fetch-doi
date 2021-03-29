package no.unit.nva.doi;

import static no.unit.nva.doi.CrossRefClient.CROSSREFPLUSAPITOKEN_KEY_ENV;
import static no.unit.nva.doi.CrossRefClient.CROSSREFPLUSAPITOKEN_NAME_ENV;
import static no.unit.nva.doi.CrossRefClient.CROSSREF_API_KEY_SECRET_NOT_FOUND_TEMPLATE;
import static no.unit.nva.doi.CrossRefClient.CROSSREF_SECRETS_NOT_FOUND;
import static no.unit.nva.doi.CrossRefClient.CROSSREF_USER_AGENT;
import static no.unit.nva.doi.CrossRefClient.ILLEGAL_DOI_MESSAGE;
import static no.unit.nva.doi.CrossRefClient.MISSING_CROSSREF_TOKENS_ERROR_MESSAGE;
import static no.unit.nva.doi.CrossRefClient.WORKS;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import com.amazonaws.services.secretsmanager.AWSSecretsManager;
import com.amazonaws.services.secretsmanager.model.GetSecretValueRequest;
import com.amazonaws.services.secretsmanager.model.GetSecretValueResult;
import com.amazonaws.services.secretsmanager.model.ResourceNotFoundException;
import com.fasterxml.jackson.core.JsonProcessingException;
import no.unit.nva.doi.utils.HttpResponseStatus200;
import no.unit.nva.doi.utils.HttpResponseStatus404;
import no.unit.nva.doi.utils.HttpResponseStatus500;
import no.unit.nva.doi.utils.MockHttpClient;
import nva.commons.core.Environment;
import nva.commons.core.JsonUtils;
import nva.commons.core.ioutils.IoUtils;
import nva.commons.logutils.LogUtils;
import nva.commons.logutils.TestAppender;
import nva.commons.secrets.SecretsReader;
import org.apache.http.HttpHeaders;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import org.mockito.invocation.InvocationOnMock;

public class CrossRefClientTest {

    public static final String DOI_STRING = "10.1007/s00115-004-1822-4";
    public static final String DOI_DX_URL_PREFIX = "https://dx.doi.org";
    public static final String DOI_URL_PREFIX = "https://doi.org";

    public static final Path CROSS_REF_SAMPLE_PATH = Paths.get("crossRefSample.json");

    public static final String ERROR_MESSAGE = "404 error message";
    public static final String ILLEGAL_DOI_STRING = "doi:" + DOI_STRING;
    public static final String HTTPS = "https";
    public static final String HTTP_DOI_URI = "http://doi.dx.org/10.000/0001";
    public static final String NAME = "name";
    public static final String KEY = "key";

    @DisplayName("createTargetUrl returns a valid Url for DOI strings that are not DOI URLs")
    @Test
    void createTargetUrlReturnsAValidUrlForDoiStringThatIsNotDoiURL()
            throws URISyntaxException, JsonProcessingException {
        String expected = String.join("/", CrossRefClient.CROSSREF_LINK, WORKS, DOI_STRING);
        String output = getConfiguredCrossrefClient().createUrlToCrossRef(DOI_STRING).toString();
        assertThat(output, is(equalTo(expected)));
    }

    @DisplayName("Requests to Crossref are made politely with https")
    @Test
    void crossRefClientIsConfiguredToUseHttps() throws URISyntaxException, JsonProcessingException {
        var crossRefUri = getConfiguredCrossrefClient().createUrlToCrossRef(HTTP_DOI_URI);
        assertThat(crossRefUri.getScheme(), equalTo(HTTPS));
    }

    @DisplayName("Requests to Crossref are made politely with user agent")
    @Test
    void crossRefHttpClientIsConfiguredToUseUserAgent() throws URISyntaxException, JsonProcessingException {
        var responseBody = IoUtils.stringFromResources(CROSS_REF_SAMPLE_PATH);
        var httpClient = new MockHttpClient<>(new HttpResponseStatus200<>(responseBody));
        getConfiguredCrossrefClient(httpClient).fetchDataForDoi(DOI_STRING);
        var httpRequest = httpClient.getHttpRequest();
        var actual = httpRequest.headers().map();
        assertTrue(actual.containsKey(HttpHeaders.USER_AGENT));
        assertTrue(actual.get(HttpHeaders.USER_AGENT).contains(CROSSREF_USER_AGENT));
    }

    @DisplayName("createTargetUrl returns a valid Url for DOI strings that are DOI DX URLs")
    @Test
    void createTargetUrlReturnsAValidUrlForDoiStringThatIsDoiDxUrl() throws URISyntaxException,
            JsonProcessingException {
        targetURlReturnsAValidUrlForDoiStrings(DOI_DX_URL_PREFIX);
    }

    @DisplayName("createTargetUrl returns a valid Url for DOI strings that are DOI URLs")
    @Test
    void createTargetUrlReturnsAValidUrlForDoiStringThatIsDoiURL() throws URISyntaxException, JsonProcessingException {
        targetURlReturnsAValidUrlForDoiStrings(DOI_URL_PREFIX);
    }

    @Test
    @DisplayName("fetchDataForDoi returns an Optional with a json object for an existing URL")
    void fetchDataForDoiReturnAnOptionalWithAJsonObjectForAnExistingUrl() throws URISyntaxException,
            JsonProcessingException {
        Optional<String> result = getConfiguredCrossrefClient().fetchDataForDoi(DOI_STRING)
                .map(MetadataAndContentLocation::getJson);
        String expected = IoUtils.stringFromResources(CROSS_REF_SAMPLE_PATH);
        assertThat(result.isPresent(), is(true));
        assertThat(result.get(), is(equalTo(expected)));
    }

    @Test
    @DisplayName("fetchDataForDoi returns an empty Optional for a non existing URL")
    void fetchDataForDoiReturnAnEmptyOptionalForANonExistingUrl() throws URISyntaxException, JsonProcessingException {
        CrossRefClient crossRefClient = crossRefClientReceives404();
        Optional<String> result = crossRefClient.fetchDataForDoi(DOI_STRING).map(MetadataAndContentLocation::getJson);
        assertThat(result.isEmpty(), is(true));
    }

    @Test
    @DisplayName("fetchDataForDoi returns an empty Optional for an unknown error")
    void fetchDataForDoiReturnAnEmptyOptionalForAnUnknownError() throws URISyntaxException, JsonProcessingException {
        CrossRefClient crossRefClient = crossRefClientReceives500();
        Optional<String> result = crossRefClient.fetchDataForDoi(DOI_STRING).map(MetadataAndContentLocation::getJson);
        assertTrue(result.isEmpty());
    }

    @Test
    void fetchDataForDoiThrowsExceptionWhenDoiHasInvalidFormat() {
        Executable executable = () -> getConfiguredCrossrefClient().fetchDataForDoi(ILLEGAL_DOI_STRING);
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, executable);
        String expected = String.format(ILLEGAL_DOI_MESSAGE, ILLEGAL_DOI_STRING);
        assertThat(exception.getMessage(), equalTo(expected));
    }

    @Test
    void crossrefClientThrowsRuntimeExceptionIfEnvironmentVariableCrossrefApiTokenNameIsMissing() {
        Executable executable = () -> getConfiguredCrossrefClient(mock(HttpClient.class), false, true, true);
        Exception exception = assertThrows(RuntimeException.class, executable);
        String actual = exception.getMessage();
        assertThat(actual, equalTo(MISSING_CROSSREF_TOKENS_ERROR_MESSAGE));
    }

    @Test
    void crossrefClientLogsErrorIfEnvironmentVariableCrossrefApiTokenNameIsMissing() {
        TestAppender log = LogUtils.getTestingAppender(CrossRefClient.class);
        Executable executable = () -> getConfiguredCrossrefClient(mock(HttpClient.class), false, true, true);
        assertThrows(RuntimeException.class, executable);
        String actual = log.getMessages();
        assertThat(actual, containsString("Missing environment variable for Crossref API CROSSREFPLUSAPITOKEN_NAME"));
    }

    @Test
    void crossrefClientThrowsRuntimeExceptionIfEnvironmentVariableCrossrefApiTokenKeyIsMissing() {
        Executable executable = () -> getConfiguredCrossrefClient(mock(HttpClient.class), true, false, true);
        Exception exception = assertThrows(RuntimeException.class, executable);
        String actual = exception.getMessage();
        assertThat(actual, containsString(MISSING_CROSSREF_TOKENS_ERROR_MESSAGE));
    }

    @Test
    void crossrefClientLogsErrorIfEnvironmentVariableCrossrefApiTokenKeyIsMissing() {
        TestAppender log = LogUtils.getTestingAppender(CrossRefClient.class);
        Executable executable = () -> getConfiguredCrossrefClient(mock(HttpClient.class), true, false, true);
        assertThrows(RuntimeException.class, executable);
        String actual = log.getMessages();
        assertThat(actual, containsString("Missing environment variable for Crossref API CROSSREFPLUSAPITOKEN_KEY"));
    }

    @Test
    void crossrefClientThrowsRuntimeExceptionIfEnvironmentVariableCrossrefApiTokensAreMissing() {
        Executable executable = () -> getConfiguredCrossrefClient(mock(HttpClient.class), false, false, true);
        Exception exception = assertThrows(RuntimeException.class, executable);
        String actual = exception.getMessage();
        assertThat(actual, containsString(MISSING_CROSSREF_TOKENS_ERROR_MESSAGE));
    }

    @Test
    void crossrefClientLogsErrorIfEnvironmentVariableCrossrefApiTokensAreMissing() {
        TestAppender log = LogUtils.getTestingAppender(CrossRefClient.class);
        Executable executable = () -> getConfiguredCrossrefClient(mock(HttpClient.class), false, false, true);
        assertThrows(RuntimeException.class, executable);
        String actual = log.getMessages();
        assertThat(actual, containsString("Missing environment variable for Crossref API CROSSREFPLUSAPITOKEN_NAME"));
        assertThat(actual, containsString("Missing environment variable for Crossref API CROSSREFPLUSAPITOKEN_KEY"));
    }

    @Test
    void crossrefClientThrowsRuntimeExceptionWhenSecretsManagerLacksSecret() {
        Executable executable = () -> getConfiguredCrossrefClient(mock(HttpClient.class), true, true, false)
                .fetchDataForDoi(DOI_STRING);
        Exception exception = assertThrows(RuntimeException.class, executable);
        String actual = exception.getMessage();
        assertThat(actual, containsString(CROSSREF_SECRETS_NOT_FOUND));
    }

    @Test
    void crossrefClientLogsMissingSecretsInSecretsManager() {
        TestAppender log = LogUtils.getTestingAppender(CrossRefClient.class);
        Executable executable = () -> getConfiguredCrossrefClient(mock(HttpClient.class), true, true, false)
                .fetchDataForDoi(DOI_STRING);
        assertThrows(RuntimeException.class, executable);
        String actual = log.getMessages();
        String expected = String.format(CROSSREF_API_KEY_SECRET_NOT_FOUND_TEMPLATE.replace("{}", "%s"), NAME, KEY);
        assertThat(actual, containsString(expected));
    }

    @SuppressWarnings("unchecked")
    @Test
    void fetchDataForDoiReturnsNotFoundWhenInputDoiDoesNotDereference() throws URISyntaxException,
            JsonProcessingException {
        HttpClient httpClient = mock(HttpClient.class);
        var httpResponse = new HttpResponseStatus404<>("Not found");
        CompletableFuture<HttpResponse<String>> completableFuture = CompletableFuture.supplyAsync(() -> httpResponse);
        when(httpClient.sendAsync(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenReturn(completableFuture);
        Optional<MetadataAndContentLocation> actual = getConfiguredCrossrefClient(httpClient)
                .fetchDataForDoi(DOI_STRING);
        assertThat(actual, equalTo(Optional.empty()));
    }

    private CrossRefClient getConfiguredCrossrefClient() throws JsonProcessingException {
        final HttpClient httpClient = mockHttpClientWithNonEmptyResponse();
        return getConfiguredCrossrefClient(httpClient);
    }

    private CrossRefClient getConfiguredCrossrefClient(HttpClient httpClient) throws JsonProcessingException {
        return getConfiguredCrossrefClient(httpClient, true, true, true);
    }

    private CrossRefClient getConfiguredCrossrefClient(HttpClient httpClient,
                                                       boolean withApiTokenName,
                                                       boolean withApiTokenKey,
                                                       boolean withApiSecret) throws JsonProcessingException {
        Environment environment = mock(Environment.class);
        if (withApiTokenName) {
            when(environment.readEnvOpt(CROSSREFPLUSAPITOKEN_NAME_ENV)).thenReturn(Optional.of(NAME));
        }
        if (withApiTokenKey) {
            when(environment.readEnvOpt(CROSSREFPLUSAPITOKEN_KEY_ENV)).thenReturn(Optional.of(KEY));
        }

        AWSSecretsManager secretsManager = mock(AWSSecretsManager.class);
        SecretsReader secretsReader = new SecretsReader(secretsManager);
        if (withApiSecret) {
            String secretString = JsonUtils.objectMapper.writeValueAsString(
                    Map.of(KEY, "irrelevant"));
            GetSecretValueResult secretValue = new GetSecretValueResult().withName(NAME)
                    .withSecretString(secretString);
            when(secretsManager.getSecretValue(any(GetSecretValueRequest.class))).thenReturn(secretValue);
        } else {
            when(secretsManager.getSecretValue(any(GetSecretValueRequest.class)))
                    .thenAnswer(this::secretValueProvider);
        }
        return new CrossRefClient(httpClient, environment, secretsReader);
    }

    private GetSecretValueResult secretValueProvider(InvocationOnMock invocation) {
        throw new ResourceNotFoundException("irrelevant");
    }

    private void targetURlReturnsAValidUrlForDoiStrings(String doiPrefix) throws URISyntaxException,
            JsonProcessingException {
        String doiURL = String.join("/", doiPrefix, DOI_STRING);
        String expected = String.join("/", CrossRefClient.CROSSREF_LINK, WORKS, DOI_STRING);

        String output = getConfiguredCrossrefClient().createUrlToCrossRef(doiURL).toString();
        assertThat(output, is(equalTo(expected)));
    }

    private HttpClient mockHttpClientWithNonEmptyResponse() {
        String responseBody = IoUtils.stringFromResources(CROSS_REF_SAMPLE_PATH);
        HttpResponseStatus200<String> response = new HttpResponseStatus200<>(responseBody);
        return new MockHttpClient<>(response);
    }

    private CrossRefClient crossRefClientReceives404() throws JsonProcessingException {
        HttpResponseStatus404<String> errorResponse = new HttpResponseStatus404<>(
                ERROR_MESSAGE);
        MockHttpClient<String> mockHttpClient = new MockHttpClient<>(errorResponse);
        return getConfiguredCrossrefClient(mockHttpClient);
    }

    private CrossRefClient crossRefClientReceives500() throws JsonProcessingException {
        HttpResponseStatus500<String> errorResponse = new HttpResponseStatus500<>(
                ERROR_MESSAGE);
        MockHttpClient<String> mockHttpClient = new MockHttpClient<>(errorResponse);
        return getConfiguredCrossrefClient(mockHttpClient);
    }
}
