package no.sikt.nva.scopus;

import static no.sikt.nva.scopus.ScopusConstants.ADDITIONAL_IDENTIFIERS_SCOPUS_ID_SOURCE_NAME;
import static nva.commons.core.attempt.Try.attempt;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.S3Event;
import jakarta.xml.bind.JAXB;
import java.io.StringReader;
import java.net.URI;
import java.time.Instant;
import no.scopus.generated.DocTp;
import no.unit.nva.events.models.EventReference;
import no.unit.nva.metadata.CreatePublicationRequest;
import no.unit.nva.model.AdditionalIdentifier;
import no.unit.nva.metadata.service.MetadataService;
import no.unit.nva.s3.S3Driver;
import nva.commons.core.Environment;
import nva.commons.core.JacocoGenerated;
import nva.commons.core.paths.UnixPath;
import nva.commons.core.paths.UriWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.services.eventbridge.EventBridgeClient;
import software.amazon.awssdk.services.eventbridge.model.PutEventsRequest;
import software.amazon.awssdk.services.eventbridge.model.PutEventsRequestEntry;
import software.amazon.awssdk.services.s3.S3Client;

public class ScopusHandler implements RequestHandler<S3Event, CreatePublicationRequest> {

    public static final int SINGLE_EXPECTED_RECORD = 0;
    public static final String S3_URI_TEMPLATE = "s3://%s/%s";
    private static final Logger logger = LoggerFactory.getLogger(ScopusHandler.class);
    private static final String SCOPUS_EVENTS_FOLDER = "scopus/create";
    //TODO: move to config class.
    private static final String EVENTS_BUCKET = new Environment().readEnv("EVENTS_BUCKET");
    private static final String EVENT_BUS = new Environment().readEnv("EVENTS_BUS");
    public static final String EVENT_TOPIC_IS_SET_IN_EVENT_BODY = "ReferToEventTopic";

    private final S3Client s3Client;
    private final MetadataService metadataService;
    private final EventBridgeClient eventBridgeClient;

    @JacocoGenerated
    public ScopusHandler() {
        this(S3Driver.defaultS3Client().build(), defaultMetadataService(), defaultEventBridgeClient());
    }

    public ScopusHandler(S3Client s3Client, MetadataService metadataService, EventBridgeClient eventBridgeClient) {
        this.metadataService = metadataService;
        this.s3Client = s3Client;
        this.eventBridgeClient = eventBridgeClient;
    }

    @Override
    public CreatePublicationRequest handleRequest(S3Event event, Context context) {
        var request = createPublicationRequest(event);
        emitEventToEventBridge(context, request);
        return request;
    }

    @JacocoGenerated
    private static EventBridgeClient defaultEventBridgeClient() {
        //TODO: setup the client properly
        return EventBridgeClient.create();
    }

    private CreatePublicationRequest createPublicationRequest(S3Event event) {
        return attempt(() -> readFile(event))
            .map(this::parseXmlFile)
            .map(this::generateCreatePublicationRequest)
            .orElseThrow(fail -> logErrorAndThrowException(fail.getException()));
    }

    @JacocoGenerated
    private static MetadataService defaultMetadataService() {
        return new MetadataService();
    }

    private void emitEventToEventBridge(Context context, CreatePublicationRequest request) {
        var fileLocation = writeRequestToS3(request);
        var eventToEmit = new NewScopusEntryEvent(fileLocation);
        eventBridgeClient.putEvents(createPutEventRequest(eventToEmit, context));
    }

    private URI writeRequestToS3(CreatePublicationRequest request) {
        var s3Writer = new S3Driver(s3Client, EVENTS_BUCKET);
        return attempt(() -> constructPathForEventBody(request))
            .map(path -> s3Writer.insertFile(path, request.toJsonString()))
            .orElseThrow(fail -> logErrorAndThrowException(fail.getException()));
    }

    private PutEventsRequest createPutEventRequest(EventReference eventToEmit, Context context) {
        var entry = PutEventsRequestEntry.builder()
            .detailType(EVENT_TOPIC_IS_SET_IN_EVENT_BODY)
            .time(Instant.now())
            .eventBusName(EVENT_BUS)
            .detail(eventToEmit.toJsonString())
            .source(context.getFunctionName())
            .resources(context.getInvokedFunctionArn())
            .build();
        return PutEventsRequest.builder().entries(entry).build();
    }

    private UnixPath constructPathForEventBody(CreatePublicationRequest request) {
        return UnixPath.of(SCOPUS_EVENTS_FOLDER, extractOneOfPossiblyManyScopusIdentifiers(request));
    }

    private String extractOneOfPossiblyManyScopusIdentifiers(CreatePublicationRequest request) {
        return request.getAdditionalIdentifiers()
            .stream()
            .filter(identifier -> ADDITIONAL_IDENTIFIERS_SCOPUS_ID_SOURCE_NAME.equals(identifier.getSource()))
            .map(AdditionalIdentifier::getValue)
            .findFirst()
            .orElseThrow();
    }

    private RuntimeException logErrorAndThrowException(Exception exception) {
        logger.error(exception.getMessage());
        return exception instanceof RuntimeException
                   ? (RuntimeException) exception
                   : new RuntimeException(exception);
    }

    private DocTp parseXmlFile(String file) {
        return JAXB.unmarshal(new StringReader(file), DocTp.class);
    }

    private CreatePublicationRequest generateCreatePublicationRequest(DocTp docTp) {
        var scopusConverter = new ScopusConverter(docTp, metadataService);
        return scopusConverter.generateCreatePublicationRequest();
    }

    private String readFile(S3Event event) {
        var s3Driver = new S3Driver(s3Client, extractBucketName(event));
        var fileUri = createS3BucketUri(event);
        return s3Driver.getFile(new UriWrapper(fileUri).toS3bucketPath());
    }

    private String extractBucketName(S3Event event) {
        return event.getRecords().get(SINGLE_EXPECTED_RECORD).getS3().getBucket().getName();
    }

    private String extractFilename(S3Event event) {
        return event.getRecords().get(SINGLE_EXPECTED_RECORD).getS3().getObject().getKey();
    }

    private URI createS3BucketUri(S3Event s3Event) {
        return URI.create(String.format(S3_URI_TEMPLATE, extractBucketName(s3Event), extractFilename(s3Event)));
    }
}
