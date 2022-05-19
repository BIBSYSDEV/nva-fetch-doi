package no.sikt.nva.scopus.email;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.events.S3Event;
import com.amazonaws.services.lambda.runtime.events.models.s3.S3EventNotification.RequestParametersEntity;
import com.amazonaws.services.lambda.runtime.events.models.s3.S3EventNotification.ResponseElementsEntity;
import com.amazonaws.services.lambda.runtime.events.models.s3.S3EventNotification.S3BucketEntity;
import com.amazonaws.services.lambda.runtime.events.models.s3.S3EventNotification.S3Entity;
import com.amazonaws.services.lambda.runtime.events.models.s3.S3EventNotification.S3EventNotificationRecord;
import com.amazonaws.services.lambda.runtime.events.models.s3.S3EventNotification.S3ObjectEntity;
import com.amazonaws.services.lambda.runtime.events.models.s3.S3EventNotification.UserIdentityEntity;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.http.Fault;
import no.sikt.nva.testing.http.WiremockHttpClient;
import no.unit.nva.s3.S3Driver;
import no.unit.nva.stubs.FakeContext;
import no.unit.nva.stubs.FakeS3Client;
import nva.commons.core.ioutils.IoUtils;
import nva.commons.core.paths.UnixPath;
import nva.commons.core.paths.UriWrapper;
import nva.commons.logutils.LogUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.core.sync.ResponseTransformer;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.http.HttpClient;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.configureFor;
import static com.github.tomakehurst.wiremock.client.WireMock.forbidden;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;
import static com.google.common.net.HttpHeaders.CONTENT_DISPOSITION;
import static com.google.common.net.HttpHeaders.CONTENT_TYPE;
import static no.sikt.nva.scopus.email.ScopusEmailHandler.ERROR_STORING_SCOPUS_ZIP_FILE_FROM_URL;
import static no.sikt.nva.scopus.email.ScopusEmailHandler.SCOPUS_ZIP_FILE_STORED_FROM_URL;
import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.StringContains.containsString;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ScopusEmailHandlerTest {

    public static final RequestParametersEntity EMPTY_REQUEST_PARAMETERS = null;
    public static final ResponseElementsEntity EMPTY_RESPONSE_ELEMENTS = null;
    public static final UserIdentityEntity EMPTY_USER_IDENTITY = null;
    public static final String WIREMOCK_SCOPUS_ZIP_FILE = "scopus.zip";
    public static final long SOME_FILE_SIZE = 100L;
    private static final String TEST_EMAIL_EML = "test_email.eml";
    private static final String TEST_EMAIL_FULLFORMAT_ZIP_DOWNLOAD_URL = "FULLFORMAT_ZIP_DOWNLOAD_URL";

    private final Context context = new FakeContext();
    private FakeS3Client s3Client;
    private S3Driver s3Driver;
    private ScopusEmailHandler scopusEmailHandler;
    private WireMockServer httpServer;
    private URI serverUri;

    @BeforeEach
    public void init() {
        startWiremockServer();
        s3Client = new FakeS3Client();
        s3Driver = new S3Driver(s3Client, "ignoredValue");
        var httpClient = WiremockHttpClient.create();
        scopusEmailHandler = new ScopusEmailHandler(s3Client, httpClient);
    }

    @AfterEach
    public void tearDown() {
        httpServer.stop();
    }

    @Test
    void shouldLogExceptionMessageWhenExceptionOccurs() {
        var s3Event = createS3Event(randomString());
        var expectedMessage = randomString();
        s3Client = new FakeS3ClientThrowingException(expectedMessage);
        scopusEmailHandler = new ScopusEmailHandler(s3Client, HttpClient.newBuilder().build());
        var appender = LogUtils.getTestingAppenderForRootLogger();
        assertThrows(RuntimeException.class, () -> scopusEmailHandler.handleRequest(s3Event, context));
        assertThat(appender.getMessages(), containsString(expectedMessage));
    }

    @Test
    void shouldDownloadFileFromUrlFoundInEmailToS3Bucket() throws IOException {
        var uri = mockedGetRequestThatReturnsFile(WIREMOCK_SCOPUS_ZIP_FILE);
        var emailContents = IoUtils.stringFromResources(Path.of(TEST_EMAIL_EML))
                .replace(TEST_EMAIL_FULLFORMAT_ZIP_DOWNLOAD_URL, uri.toString());
        var s3Event = createNewScopusEmailEvent(emailContents);
        var appender = LogUtils.getTestingAppenderForRootLogger();
        scopusEmailHandler.handleRequest(s3Event, context);
        var expectedLogMessage = createExpectedFileStoredLogMessage(WIREMOCK_SCOPUS_ZIP_FILE, uri);
        assertThat(appender.getMessages(), containsString(expectedLogMessage));
        verify(getRequestedFor(urlEqualTo(UriWrapper.fromUri(uri).getPath().toString())));
    }

    @Test
    void shouldLogErrorWhenNotOkStatusRequestingFile() throws IOException {
        var uri = mockedGetRequestThatReturnsForbidden(WIREMOCK_SCOPUS_ZIP_FILE);
        var emailContents = IoUtils.stringFromResources(Path.of(TEST_EMAIL_EML))
                .replace(TEST_EMAIL_FULLFORMAT_ZIP_DOWNLOAD_URL, uri.toString());
        var s3Event = createNewScopusEmailEvent(emailContents);
        var expectedLogMessage = createExpectedFileStoreErrorLogMessage(WIREMOCK_SCOPUS_ZIP_FILE, uri);
        var appender = LogUtils.getTestingAppenderForRootLogger();
        assertThrows(RuntimeException.class, () -> scopusEmailHandler.handleRequest(s3Event, context));
        assertThat(appender.getMessages(), containsString(expectedLogMessage));
        verify(getRequestedFor(urlEqualTo(UriWrapper.fromUri(uri).getPath().toString())));
    }

    @Test
    void shouldLogErrorWhenExceptionRequestingFile() throws IOException {
        var uri = mockedGetRequestWhereConnectionResetByPeer(WIREMOCK_SCOPUS_ZIP_FILE);
        var emailContents = IoUtils.stringFromResources(Path.of(TEST_EMAIL_EML))
                .replace(TEST_EMAIL_FULLFORMAT_ZIP_DOWNLOAD_URL, uri.toString());
        var s3Event = createNewScopusEmailEvent(emailContents);
        var expectedLogMessage = createExpectedFileStoreErrorLogMessage(WIREMOCK_SCOPUS_ZIP_FILE, uri);
        var appender = LogUtils.getTestingAppenderForRootLogger();
        assertThrows(RuntimeException.class, () -> scopusEmailHandler.handleRequest(s3Event, context));
        assertThat(appender.getMessages(), containsString(expectedLogMessage));
    }

    private String createExpectedFileStoredLogMessage(String filename, URI uri) {
        return String.format(SCOPUS_ZIP_FILE_STORED_FROM_URL, filename, uri.toString());
    }

    private String createExpectedFileStoreErrorLogMessage(String filename, URI uri) {
        return String.format(ERROR_STORING_SCOPUS_ZIP_FILE_FROM_URL, filename, uri.toString());
    }

    private S3Event createNewScopusEmailEvent(String emailContents) throws IOException {
        var uri = s3Driver.insertFile(randomS3Path(), emailContents);
        return createS3Event(uri);
    }

    private UnixPath randomS3Path() {
        return UnixPath.of(randomString());
    }

    private void startWiremockServer() {
        httpServer = new WireMockServer(options().dynamicPort().dynamicHttpsPort().httpDisabled(true));
        httpServer.start();
        serverUri = URI.create(httpServer.baseUrl());
    }

    private S3Event createS3Event(String expectedObjectKey) {
        var eventNotification = new S3EventNotificationRecord(randomString(),
                                                              randomString(),
                                                              randomString(),
                                                              randomDate(),
                                                              randomString(),
                                                              EMPTY_REQUEST_PARAMETERS,
                                                              EMPTY_RESPONSE_ELEMENTS,
                                                              createS3Entity(expectedObjectKey),
                                                              EMPTY_USER_IDENTITY);
        return new S3Event(List.of(eventNotification));
    }

    private S3Event createS3Event(URI uri) {
        return createS3Event(UriWrapper.fromUri(uri).toS3bucketPath().toString());
    }

    private String randomDate() {
        return Instant.now().toString();
    }

    private S3Entity createS3Entity(String expectedObjectKey) {
        var bucket = new S3BucketEntity(randomString(), EMPTY_USER_IDENTITY, randomString());
        var object = new S3ObjectEntity(expectedObjectKey, SOME_FILE_SIZE, randomString(), randomString(),
                                        randomString());
        var schemaVersion = randomString();
        return new S3Entity(randomString(), bucket, object, schemaVersion);
    }

    private static class FakeS3ClientThrowingException extends FakeS3Client {

        private final String expectedErrorMessage;

        public FakeS3ClientThrowingException(String expectedErrorMessage) {
            super();
            this.expectedErrorMessage = expectedErrorMessage;
        }

        @Override
        public <ReturnT> ReturnT getObject(GetObjectRequest getObjectRequest,
                                           ResponseTransformer<GetObjectResponse, ReturnT> responseTransformer) {
            throw new RuntimeException(expectedErrorMessage);
        }
    }

    private URI mockedGetRequestThatReturnsFile(String filename) {
        setupWiremockPorts();
        stubFor(get(urlEqualTo("/file/" + filename))
                .willReturn(aResponse()
                        .withHeader(CONTENT_TYPE, "application/octet-stream")
                        .withHeader(CONTENT_DISPOSITION, "attachment; filename=" + filename)
                        .withStatus(HttpURLConnection.HTTP_OK)
                        .withBodyFile(filename)));
        return UriWrapper.fromUri(serverUri).addChild("file").addChild(filename).getUri();
    }

    private URI mockedGetRequestThatReturnsForbidden(String filename) {
        setupWiremockPorts();
        stubFor(get(urlEqualTo("/file/" + filename)).willReturn(forbidden()));
        return UriWrapper.fromUri(serverUri).addChild("file").addChild(filename).getUri();
    }

    private URI mockedGetRequestWhereConnectionResetByPeer(String filename) {
        setupWiremockPorts();
        stubFor(get(urlEqualTo("/file/" + filename))
                .willReturn(aResponse().withFault(Fault.CONNECTION_RESET_BY_PEER)));
        return UriWrapper.fromUri(serverUri).addChild("file").addChild(filename).getUri();
    }

    private void setupWiremockPorts() {
        configureFor(serverUri.getScheme(), serverUri.getHost(), serverUri.getPort());
    }

}