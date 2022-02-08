package no.sikt.nva.scopus;

import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.mockito.Mockito.mock;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.events.S3Event;
import com.amazonaws.services.lambda.runtime.events.models.s3.S3EventNotification.RequestParametersEntity;
import com.amazonaws.services.lambda.runtime.events.models.s3.S3EventNotification.ResponseElementsEntity;
import com.amazonaws.services.lambda.runtime.events.models.s3.S3EventNotification.S3BucketEntity;
import com.amazonaws.services.lambda.runtime.events.models.s3.S3EventNotification.S3Entity;
import com.amazonaws.services.lambda.runtime.events.models.s3.S3EventNotification.S3EventNotificationRecord;
import com.amazonaws.services.lambda.runtime.events.models.s3.S3EventNotification.S3ObjectEntity;
import com.amazonaws.services.lambda.runtime.events.models.s3.S3EventNotification.UserIdentityEntity;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;

import no.unit.nva.commons.json.JsonUtils;
import no.unit.nva.s3.S3Driver;
import no.unit.nva.stubs.FakeEventBridgeClient;
import no.unit.nva.stubs.FakeS3Client;
import nva.commons.core.ioutils.IoUtils;
import nva.commons.core.paths.UnixPath;
import nva.commons.core.paths.UriWrapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ScopusDeleteHandlerTest {

    public static final Context CONTEXT = mock(Context.class);
    public static final RequestParametersEntity EMPTY_REQUEST_PARAMETERS = null;
    public static final ResponseElementsEntity EMPTY_RESPONSE_ELEMENTS = null;
    public static final UserIdentityEntity EMPTY_USER_IDENTITY = null;
    public static final long SOME_FILE_SIZE = 100L;

    public static final String DELETE_MULTIPLE_IDENTIFIERS_FILE = "deleteMultipleIdentifiers.txt";
    public static final String DELETE_SINGLE_IDENTIFIER_FILE = "deleteSingleIdentifier.txt";
    public static final String HARDCODED_SCOPUS_IDENTIFIER_IN_DELETE_SINGLE_FILE = "0000687314";
    public static final List<String> HARDCODED_SCOPUS_IDENTIFIERS_IN_DELETE_MULTIPLE_FILE =
            Arrays.asList("0000687314", "0017752991", "33947568076", "85084744636");

    private FakeS3Client s3Client;
    private S3Driver s3Driver;
    private ScopusDeleteHandler scopusDeleteHandler;
    private FakeEventBridgeClient fakeEventBridgeClient;


    @BeforeEach
    public void init() {
        s3Client = new FakeS3Client();
        s3Driver = new S3Driver(s3Client, "ignoredValue");
        fakeEventBridgeClient = new FakeEventBridgeClient();
        scopusDeleteHandler = new ScopusDeleteHandler(s3Client, fakeEventBridgeClient);
    }

    @Test
    void shouldEmitEventWithSingleScopusIdentifierWhenInputFileContainsSingleScopusIdentifier() throws IOException {
        var scopusFile = IoUtils.stringFromResources(Path.of(DELETE_SINGLE_IDENTIFIER_FILE));
        var uri = s3Driver.insertFile(UnixPath.of(randomString()), scopusFile);
        var s3Event = createS3Event(uri);
        scopusDeleteHandler.handleRequest(s3Event, CONTEXT);
        var requestEntries = fakeEventBridgeClient.getRequestEntries();
        assertThat(requestEntries.size(), is(equalTo(1)));
        var eventBodyJson = requestEntries.get(0).detail();
        var actualRequestEntryBody =
                JsonUtils.dtoObjectMapper.readValue(eventBodyJson, ScopusDeleteEventBody.class);
        var expectedRequestEntryBody = new ScopusDeleteEventBody(HARDCODED_SCOPUS_IDENTIFIER_IN_DELETE_SINGLE_FILE);
        assertThat(actualRequestEntryBody, is(equalTo(expectedRequestEntryBody)));
    }

    @Test
    void shouldEmitEventsWithScopusIdentifiersWhenInputFileContainsMultipleScopusIdentifiers() throws IOException {
        var scopusFile = IoUtils.stringFromResources(Path.of(DELETE_MULTIPLE_IDENTIFIERS_FILE));
        var uri = s3Driver.insertFile(UnixPath.of(randomString()), scopusFile);
        var s3Event = createS3Event(uri);
        scopusDeleteHandler.handleRequest(s3Event, CONTEXT);
        var requestEntries = fakeEventBridgeClient.getRequestEntries();
        assertThat(requestEntries.size(), is(equalTo(HARDCODED_SCOPUS_IDENTIFIERS_IN_DELETE_MULTIPLE_FILE.size())));
        var eventBodyJson = requestEntries.get(0).detail();
        var actualRequestEntryBody =
                JsonUtils.dtoObjectMapper.readValue(eventBodyJson, ScopusDeleteEventBody.class);
        var expectedRequestEntryBody = new ScopusDeleteEventBody(HARDCODED_SCOPUS_IDENTIFIER_IN_DELETE_SINGLE_FILE);
        assertThat(actualRequestEntryBody, is(equalTo(expectedRequestEntryBody)));
    }

    @Test
    void shouldReturnCorrectIdentifiersFromFileContentWhenEventWithS3UriThatPointsToScopusDeleteFile()
            throws IOException {
        var scopusFile = IoUtils.stringFromResources(Path.of(DELETE_MULTIPLE_IDENTIFIERS_FILE));
        var uri = s3Driver.insertFile(UnixPath.of(randomString()), scopusFile);
        var s3Event = createS3Event(uri);
        var identifiersToDelete = scopusDeleteHandler.handleRequest(s3Event, CONTEXT);
        assertThat(identifiersToDelete, is(equalTo(HARDCODED_SCOPUS_IDENTIFIERS_IN_DELETE_MULTIPLE_FILE)));
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