package no.sikt.nva.scopus;

import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThan;
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
import java.net.URI;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

class ScopusHandlerTest {

    public static final Context CONTEXT = mock(Context.class);
    public static final RequestParametersEntity EMPTY_REQUEST_PARAMETERS = null;
    public static final ResponseElementsEntity EMPTY_RESPONSE_ELEMENTS = null;
    public static final UserIdentityEntity EMPTY_USER_IDENTITY = null;
    public static final long SOME_FILE_SIZE = 100L;


    @Test
    void fakeEventShouldContainS3BucketNameAndS3ObjectKey(){
        var expectedBucketName = randomString();
        var expectedObjectKey = randomString();
        S3Event s3Event = createS3Event(expectedBucketName,expectedObjectKey);
        assertThat(s3Event.getRecords().size(),is(greaterThan(0)));
        assertThat(s3Event.getRecords().get(0).getS3().getBucket().getName(),is(equalTo(expectedBucketName)));
        assertThat(s3Event.getRecords().get(0).getS3().getObject().getKey(),is(equalTo(expectedObjectKey)));
    }

    @Test
    void shouldReturnS3UriWhenS3EventIsReceived() {
        var expectedBucketName = randomString();
        var expectedObjectKey = randomString();
        S3Event s3Event = createS3Event(expectedBucketName,expectedObjectKey);

        ScopusHandler scopusHandler = new ScopusHandler();
        String actualFileUri = scopusHandler.handleRequest(s3Event, CONTEXT);
        var expectedUri = URI.create(String.format("s3://%s/%s",expectedBucketName,expectedObjectKey));
        assertThat(actualFileUri, is(equalTo(expectedUri.toString())));
    }

    private S3Event createS3Event(String expectedBucketName, String expectedObjectKey) {
        var eventNotification = new S3EventNotificationRecord(randomString(),
                                                              randomString(),
                                                              randomString(),
                                                              randomDate(),
                                                              randomString(),
                                                              EMPTY_REQUEST_PARAMETERS,
                                                              EMPTY_RESPONSE_ELEMENTS,
                                                              createS3Entity(expectedBucketName, expectedObjectKey),
                                                              EMPTY_USER_IDENTITY);
        return new S3Event(List.of(eventNotification));


    }

    private String randomDate() {
        return Instant.now().toString();
    }

    private S3Entity createS3Entity(String expectedBucketName, String expectedObjectKey) {
        S3BucketEntity bucket= new S3BucketEntity(expectedBucketName, EMPTY_USER_IDENTITY,randomString());
        S3ObjectEntity object = new S3ObjectEntity(expectedObjectKey, SOME_FILE_SIZE,randomString(),randomString(),randomString());
        String schemaVersion = randomString();
        return new S3Entity(randomString(), bucket, object, schemaVersion);
    }

}