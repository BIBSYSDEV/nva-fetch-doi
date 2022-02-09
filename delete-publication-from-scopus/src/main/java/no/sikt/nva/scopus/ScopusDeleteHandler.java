package no.sikt.nva.scopus;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.S3Event;

import java.net.URI;
import java.util.List;
import java.util.stream.Collectors;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.common.collect.Lists;
import no.unit.nva.s3.S3Driver;
import nva.commons.core.JacocoGenerated;
import nva.commons.core.paths.UriWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.services.eventbridge.EventBridgeClient;
import software.amazon.awssdk.services.eventbridge.model.PutEventsRequest;
import software.amazon.awssdk.services.eventbridge.model.PutEventsRequestEntry;
import software.amazon.awssdk.services.s3.S3Client;

import static no.unit.nva.commons.json.JsonUtils.dtoObjectMapper;
import static nva.commons.core.attempt.Try.attempt;

public class ScopusDeleteHandler implements RequestHandler<S3Event, List<String>> {

    public static final int SINGLE_EXPECTED_RECORD = 0;
    public static final int PUT_EVENTS_REQUEST_MAX_ENTRIES = 2;
    public static final String S3_SCHEME = "s3";
    public static final String DELETE_SCOPUS_IDENTIFIER_PREFIX = "DELETE-2-s2.0-";
    public static final String EMPTY_STRING = "";
    private static final Logger logger = LoggerFactory.getLogger(ScopusDeleteHandler.class);
    private final S3Client s3Client;
    private final EventBridgeClient eventBridgeClient;

    @JacocoGenerated
    public ScopusDeleteHandler() {
        this(S3Driver.defaultS3Client().build(), EventBridgeClient.create());
    }

    public ScopusDeleteHandler(S3Client s3Client, EventBridgeClient eventBridgeClient) {
        this.s3Client = s3Client;
        this.eventBridgeClient = eventBridgeClient;
    }

    @Override
    public List<String> handleRequest(S3Event event, Context context) {

        List<String> identifiersToDelete = readIdentifiersToDelete(event);

        emitEventsForIdentifiersToDelete(identifiersToDelete);

        return identifiersToDelete;
    }

    private void emitEventsForIdentifiersToDelete(List<String> identifiersToDelete) {
        Lists.partition(identifiersToDelete, PUT_EVENTS_REQUEST_MAX_ENTRIES).stream()
                .map(this::createPutEventsRequestEntries)
                .collect(Collectors.toList())
                .forEach(this::emitPutEventsRequest);
    }

    private List<String> readIdentifiersToDelete(S3Event event) {
        return attempt(() -> readFile(event))
                    .orElseThrow(fail -> logErrorAndThrowException(fail.getException()))
                    .lines()
                    .map(this::trimDeletePrefix)
                    .collect(Collectors.toList());
    }

    private void emitPutEventsRequest(List<PutEventsRequestEntry> requestEntries) {
        attempt(() -> eventBridgeClient.putEvents(PutEventsRequest.builder().entries(requestEntries).build()))
                .orElseThrow(fail -> logErrorAndThrowException(fail.getException()));
    }

    private List<PutEventsRequestEntry> createPutEventsRequestEntries(List<String> scopusIdentifiersList) {
        return scopusIdentifiersList.stream()
                .map(this::createPutEventsRequestEntry)
                .collect(Collectors.toList());
    }

    private PutEventsRequestEntry createPutEventsRequestEntry(String scopusIdentifier) {
        return PutEventsRequestEntry.builder()
                .detail(createScopusDeleteEventBodyJson(scopusIdentifier))
                .build();
    }

    private String createScopusDeleteEventBodyJson(String scopusIdentifier) {
        try {
            return dtoObjectMapper.writeValueAsString(new ScopusDeleteEventBody(scopusIdentifier));
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    private String trimDeletePrefix(String line) {
        return line.replace(DELETE_SCOPUS_IDENTIFIER_PREFIX, EMPTY_STRING).trim();
    }

    private String readFile(S3Event event) {
        var s3Driver = new S3Driver(s3Client, extractBucketName(event));
        var fileUri = createS3BucketUri(event);
        return s3Driver.getFile(new UriWrapper(fileUri).toS3bucketPath());
    }

    private URI createS3BucketUri(S3Event event) {
        return new UriWrapper(S3_SCHEME,extractBucketName(event)).addChild(extractFilename(event)).getUri();
    }

    private String extractBucketName(S3Event event) {
        return event.getRecords().get(SINGLE_EXPECTED_RECORD).getS3().getBucket().getName();
    }

    private String extractFilename(S3Event event) {
        return event.getRecords().get(SINGLE_EXPECTED_RECORD).getS3().getObject().getKey();
    }

    private RuntimeException logErrorAndThrowException(Exception exception) {
        logger.error(exception.getMessage());
        return exception instanceof RuntimeException
                ? (RuntimeException) exception
                : new RuntimeException(exception);
    }
}
