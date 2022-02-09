package no.sikt.nva.scopus;

import static nva.commons.core.attempt.Try.attempt;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.S3Event;
import jakarta.xml.bind.JAXB;
import java.io.StringReader;
import java.net.URI;
import no.scopus.generated.DocTp;
import no.unit.nva.metadata.CreatePublicationRequest;
import no.unit.nva.metadata.service.MetadataService;
import no.unit.nva.s3.S3Driver;
import nva.commons.core.JacocoGenerated;
import nva.commons.core.paths.UriWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.services.s3.S3Client;

public class ScopusHandler implements RequestHandler<S3Event, CreatePublicationRequest> {

    public static final int SINGLE_EXPECTED_RECORD = 0;
    public static final String S3_URI_TEMPLATE = "s3://%s/%s";
    private static final Logger logger = LoggerFactory.getLogger(ScopusHandler.class);
    private final S3Client s3Client;
    private final MetadataService metadataService;

    @JacocoGenerated
    public ScopusHandler() {
        this(S3Driver.defaultS3Client().build(), defaultMetadataService());
    }

    public ScopusHandler(S3Client s3Client, MetadataService metadataService) {
        this.metadataService = metadataService;
        this.s3Client = s3Client;
    }

    @Override
    public CreatePublicationRequest handleRequest(S3Event event, Context context) {
        return attempt(() -> readFile(event))
            .map(this::parseXmlFile)
            .map(this::generateCreatePublicationRequest)
            .orElseThrow(fail -> logErrorAndThrowException(fail.getException()));
    }

    @JacocoGenerated
    private static MetadataService defaultMetadataService() {
        return new MetadataService();
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
