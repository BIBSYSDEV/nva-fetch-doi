package no.unit.nva.doi;

import static no.unit.nva.doi.CrossRefClient.CROSSREFPLUSAPITOKEN_KEY_ENV;
import static no.unit.nva.doi.CrossRefClient.CROSSREFPLUSAPITOKEN_NAME_ENV;
import static no.unit.nva.doi.CrossRefClient.CROSSREF_API_KEY_SECRET_NOT_FOUND_TEMPLATE;
import static no.unit.nva.doi.CrossRefClient.CROSSREF_USER_AGENT;
import static no.unit.nva.doi.CrossRefClient.ILLEGAL_DOI_MESSAGE;
import static no.unit.nva.doi.CrossRefClient.WORKS;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.common.net.HttpHeaders;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import no.sikt.nva.doi.fetch.jsonconfig.Json;
import no.unit.nva.doi.utils.HttpResponseStatus200;
import no.unit.nva.doi.utils.HttpResponseStatus404;
import no.unit.nva.doi.utils.HttpResponseStatus500;
import no.unit.nva.doi.utils.MockHttpClient;
import nva.commons.core.Environment;
import nva.commons.core.ioutils.IoUtils;
import nva.commons.logutils.LogUtils;
import nva.commons.secrets.SecretsReader;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import org.mockito.invocation.InvocationOnMock;
import software.amazon.awssdk.services.secretsmanager.SecretsManagerClient;
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueRequest;
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueResponse;

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
    public static final String PATH_DELIMITER = "/";

    @DisplayName("createTargetUrl returns a valid Url for DOI strings that are not DOI URLs")
    @Test
    void createTargetUrlReturnsAValidUrlForDoiStringThatIsNotDoiURL()
        throws URISyntaxException, JsonProcessingException {
        var expected = String.join(PATH_DELIMITER, CrossRefClient.CROSSREF_LINK, WORKS, DOI_STRING);
        var output = getConfiguredCrossrefClient().createUrlToCrossRef(DOI_STRING).toString();
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
        var result = getConfiguredCrossrefClient().fetchDataForDoi(DOI_STRING)
            .map(MetadataAndContentLocation::getJson);
        var expected = IoUtils.stringFromResources(CROSS_REF_SAMPLE_PATH);
        assertThat(result.isPresent(), is(true));
        assertThat(result.get(), is(equalTo(expected)));
    }

    @Test
    @DisplayName("fetchDataForDoi returns an empty Optional for a non existing URL")
    void fetchDataForDoiReturnAnEmptyOptionalForANonExistingUrl() throws URISyntaxException, JsonProcessingException {
        var crossRefClient = crossRefClientReceives404();
        var result = crossRefClient.fetchDataForDoi(DOI_STRING).map(MetadataAndContentLocation::getJson);
        assertThat(result.isEmpty(), is(true));
    }

    @Test
    @DisplayName("fetchDataForDoi returns an empty Optional for an unknown error")
    void fetchDataForDoiReturnAnEmptyOptionalForAnUnknownError() throws URISyntaxException, JsonProcessingException {
        var crossRefClient = crossRefClientReceives500();
        var result = crossRefClient.fetchDataForDoi(DOI_STRING).map(MetadataAndContentLocation::getJson);
        assertTrue(result.isEmpty());
    }

    @Test
    void fetchDataForDoiThrowsExceptionWhenDoiHasInvalidFormat() {
        Executable executable = () -> getConfiguredCrossrefClient().fetchDataForDoi(ILLEGAL_DOI_STRING);
        var exception = assertThrows(IllegalArgumentException.class, executable);
        var expected = String.format(ILLEGAL_DOI_MESSAGE, ILLEGAL_DOI_STRING);
        assertThat(exception.getMessage(), equalTo(expected));
    }

    @Test
    void crossrefClientThrowsRuntimeExceptionIfEnvironmentVariableCrossrefApiTokenNameIsMissing() {
        Executable executable = () -> getConfiguredCrossrefClient(mock(HttpClient.class), false, true, true);
        var exception = assertThrows(RuntimeException.class, executable);
        var actual = exception.getMessage();
        assertThat(actual, containsString(CROSSREFPLUSAPITOKEN_NAME_ENV));
    }

    @Test
    void crossrefClientLogsErrorIfEnvironmentVariableCrossrefApiTokenNameIsMissing() {
        var log = LogUtils.getTestingAppenderForRootLogger();
        Executable executable = () -> getConfiguredCrossrefClient(mock(HttpClient.class), false, true, true);
        assertThrows(RuntimeException.class, executable);
        var actual = log.getMessages();
        assertThat(actual, containsString(CROSSREFPLUSAPITOKEN_NAME_ENV));
    }

    @Test
    void crossrefClientThrowsRuntimeExceptionIfEnvironmentVariableCrossrefApiTokenKeyIsMissing() {
        Executable executable = () -> getConfiguredCrossrefClient(mock(HttpClient.class), true, false, true);
        var exception = assertThrows(RuntimeException.class, executable);
        var actual = exception.getMessage();
        assertThat(actual, containsString(CROSSREFPLUSAPITOKEN_KEY_ENV));
    }

    @Test
    void crossrefClientLogsErrorIfEnvironmentVariableCrossrefApiTokenKeyIsMissing() {
        var log = LogUtils.getTestingAppenderForRootLogger();
        Executable executable = () -> getConfiguredCrossrefClient(mock(HttpClient.class), true, false, true);
        RuntimeException exception = assertThrows(RuntimeException.class, executable);
        assertThat(exception.getMessage(), containsString(CROSSREFPLUSAPITOKEN_KEY_ENV));
        var actual = log.getMessages();
        assertThat(actual, containsString(CROSSREFPLUSAPITOKEN_KEY_ENV));
    }

    @Test
    void crossrefClientLogsErrorIfEnvironmentVariableCrossrefApiTokensNameIsMissing() {
        var log = LogUtils.getTestingAppenderForRootLogger();
        Executable executable = () -> getConfiguredCrossrefClient(mock(HttpClient.class), false, false, true);
        RuntimeException exception = assertThrows(RuntimeException.class, executable);
        var actual = log.getMessages();
        assertThat(actual, containsString(CROSSREFPLUSAPITOKEN_NAME_ENV));
    }

    @Test
    void crossrefClientThrowsRuntimeExceptionWhenSecretsManagerLacksSecret() {
        Executable executable = () -> getConfiguredCrossrefClient(mock(HttpClient.class), true, true, false)
            .fetchDataForDoi(DOI_STRING);
        RuntimeException exception = assertThrows(RuntimeException.class, executable);
        Throwable cause = exception.getCause();
        // assert that no information about secrets is being leaked.
        assertThat(exception.getMessage(), is(equalTo(cause.getClass().getCanonicalName())));
        assertThat(cause.getMessage(), is(nullValue()));
    }

    @Test
    void crossrefClientLogsMissingSecretsInSecretsManager() {
        var log = LogUtils.getTestingAppenderForRootLogger();
        Executable executable = () -> getConfiguredCrossrefClient(mock(HttpClient.class), true, true, false)
            .fetchDataForDoi(DOI_STRING);
        assertThrows(RuntimeException.class, executable);
        var actual = log.getMessages();
        var expected = String.format(CROSSREF_API_KEY_SECRET_NOT_FOUND_TEMPLATE.replace("{}", "%s"), NAME, KEY);
        assertThat(actual, containsString(expected));
    }

    @SuppressWarnings("unchecked")
    @Test
    void fetchDataForDoiReturnsNotFoundWhenInputDoiDoesNotDereference() throws URISyntaxException,
                                                                               JsonProcessingException {
        var httpClient = mock(HttpClient.class);
        var httpResponse = new HttpResponseStatus404<>("Not found");
        var completableFuture = CompletableFuture.supplyAsync(() -> httpResponse);
        when(httpClient.sendAsync(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
            .thenReturn(completableFuture);
        var actual = getConfiguredCrossrefClient(httpClient).fetchDataForDoi(DOI_STRING);
        assertThat(actual, equalTo(Optional.empty()));
    }

    private CrossRefClient getConfiguredCrossrefClient() throws JsonProcessingException {
        var httpClient = mockHttpClientWithNonEmptyResponse();
        return getConfiguredCrossrefClient(httpClient);
    }

    private CrossRefClient getConfiguredCrossrefClient(HttpClient httpClient) throws JsonProcessingException {
        return getConfiguredCrossrefClient(httpClient, true, true, true);
    }

    private CrossRefClient getConfiguredCrossrefClient(HttpClient httpClient,
                                                       boolean withApiTokenName,
                                                       boolean withApiTokenKey,
                                                       boolean withApiSecret) throws JsonProcessingException {
        Environment spiedEnvironment = new Environment();
        var environment = spy(spiedEnvironment);
        if (withApiTokenName) {
            when(environment.readEnvOpt(CROSSREFPLUSAPITOKEN_NAME_ENV)).thenReturn(Optional.of(NAME));
        }
        if (withApiTokenKey) {
            when(environment.readEnvOpt(CROSSREFPLUSAPITOKEN_KEY_ENV)).thenReturn(Optional.of(KEY));
        }

        var secretsManager = mock(SecretsManagerClient.class);
        var secretsReader = new SecretsReader(secretsManager);
        if (withApiSecret) {
            var secretString = Json.writeValueAsString(Map.of(KEY, "irrelevant"));
            var secretValue = GetSecretValueResponse.builder().name(NAME)
                .secretString(secretString)
                .build();
            when(secretsManager.getSecretValue(any(GetSecretValueRequest.class))).thenReturn(secretValue);
        } else {
            when(secretsManager.getSecretValue(any(GetSecretValueRequest.class)))
                .thenAnswer(this::secretValueProvider);
        }
        return new CrossRefClient(httpClient, environment, secretsReader);
    }

    private GetSecretValueResponse secretValueProvider(InvocationOnMock invocation) {
        throw new RuntimeException("irrelevant");
    }

    private void targetURlReturnsAValidUrlForDoiStrings(String doiPrefix) throws URISyntaxException,
                                                                                 JsonProcessingException {
        var doiURL = String.join(PATH_DELIMITER, doiPrefix, DOI_STRING);
        var expected = String.join(PATH_DELIMITER, CrossRefClient.CROSSREF_LINK, WORKS, DOI_STRING);
        var output = getConfiguredCrossrefClient().createUrlToCrossRef(doiURL).toString();
        assertThat(output, is(equalTo(expected)));
    }

    private HttpClient mockHttpClientWithNonEmptyResponse() {
        var responseBody = IoUtils.stringFromResources(CROSS_REF_SAMPLE_PATH);
        var response = new HttpResponseStatus200<>(responseBody);
        return new MockHttpClient<>(response);
    }

    private CrossRefClient crossRefClientReceives404() throws JsonProcessingException {
        var errorResponse = new HttpResponseStatus404<>(
            ERROR_MESSAGE);
        var mockHttpClient = new MockHttpClient<>(errorResponse);
        return getConfiguredCrossrefClient(mockHttpClient);
    }

    private CrossRefClient crossRefClientReceives500() throws JsonProcessingException {
        var errorResponse = new HttpResponseStatus500<>(
            ERROR_MESSAGE);
        var mockHttpClient = new MockHttpClient<>(errorResponse);
        return getConfiguredCrossrefClient(mockHttpClient);
    }
}
