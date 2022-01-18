package no.sikt.nva.scopus;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.events.S3Event;
import com.amazonaws.services.lambda.runtime.events.models.s3.S3EventNotification.*;
import no.unit.nva.s3.S3Driver;
import no.unit.nva.stubs.FakeS3Client;
import nva.commons.core.ioutils.IoUtils;
import nva.commons.core.paths.UnixPath;
import nva.commons.core.paths.UriWrapper;
import nva.commons.logutils.LogUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.services.s3.S3Client;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;

import static no.sikt.nva.scopus.ScopusHandler.S3_EVENT_WITHOUT_RECORDS_WARNING;
import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.hamcrest.core.StringContains.containsString;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.mock;

class ScopusHandlerTest {

    public static final Context CONTEXT = mock(Context.class);
    public static final RequestParametersEntity EMPTY_REQUEST_PARAMETERS = null;
    public static final ResponseElementsEntity EMPTY_RESPONSE_ELEMENTS = null;
    public static final UserIdentityEntity EMPTY_USER_IDENTITY = null;
    public static final long SOME_FILE_SIZE = 100L;
    public static final String HARD_CODED_DOI_IN_RESOURCE_FILE = "10.1017/S0960428600000743";
    private S3Client s3Client;
    private S3Driver s3Driver;
    private ScopusHandler scopusHandler;

    @BeforeEach
    public void init() {
        s3Client = new FakeS3Client();
        s3Driver = new S3Driver(s3Client, "ignoredValue");
        scopusHandler = new ScopusHandler(s3Client);
    }

    @Test
    void shouldLogWarningWhenEventContainsNoRecords() {
        var appender = LogUtils.getTestingAppenderForRootLogger();
        S3Event s3Event = createS3EventWithoutRecords();
        assertDoesNotThrow(() -> scopusHandler.handleRequest(s3Event, CONTEXT));
        assertThat(appender.getMessages(), containsString(S3_EVENT_WITHOUT_RECORDS_WARNING));
    }

    @Test
    void shouldReturnCorrectDoiWhenEventWithS3UriThatPointsToScopusXmlWithDOi() throws IOException {
        var scopusFile = IoUtils.stringFromResources(Path.of("2-s2.0-0000469852.xml"));
        var uri = s3Driver.insertFile(UnixPath.of(randomString()), scopusFile);
        S3Event s3Event = createS3Event(uri);
        String content = scopusHandler.handleRequest(s3Event, CONTEXT);
        assertThat(content, is(equalTo(HARD_CODED_DOI_IN_RESOURCE_FILE)));
    }

    private S3Event createS3EventWithoutRecords() {
        return new S3Event();
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
        return createS3Event(new UriWrapper(uri).toS3bucketPath().toString());
    }

    private String randomDate() {
        return Instant.now().toString();
    }

    private S3Entity createS3Entity(String expectedObjectKey) {
        S3BucketEntity bucket = new S3BucketEntity(randomString(), EMPTY_USER_IDENTITY, randomString());
        S3ObjectEntity object = new S3ObjectEntity(expectedObjectKey, SOME_FILE_SIZE, randomString(), randomString(),
                randomString());
        String schemaVersion = randomString();
        return new S3Entity(randomString(), bucket, object, schemaVersion);
    }
}