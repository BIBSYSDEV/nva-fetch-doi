package no.sikt.nva.scopus;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.S3Event;
import jakarta.xml.bind.JAXB;
import no.scopus.generated.DocTp;
import no.scopus.generated.ItemidTp;
import no.unit.nva.metadata.CreatePublicationRequest;
import no.unit.nva.model.AdditionalIdentifier;
import no.unit.nva.s3.S3Driver;
import nva.commons.core.JacocoGenerated;
import nva.commons.core.paths.UriWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.services.s3.S3Client;

import java.io.StringReader;
import java.net.URI;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static nva.commons.core.attempt.Try.attempt;

public class ScopusHandler implements RequestHandler<S3Event, CreatePublicationRequest> {

    public static final int SINGLE_EXPECTED_RECORD = 0;
    public static final String S3_URI_TEMPLATE = "s3://%s/%s";
    private static final Logger logger = LoggerFactory.getLogger(ScopusHandler.class);
    private final S3Client s3Client;

    @JacocoGenerated
    public ScopusHandler() {
        this(S3Driver.defaultS3Client().build());
    }

    public ScopusHandler(S3Client s3Client) {
        this.s3Client = s3Client;
    }

    @Override
    public CreatePublicationRequest handleRequest(S3Event event, Context context) {
        return attempt(() -> readFile(event))
                .map(this::parseXmlFile)
                .map(this::generateCreatePublicationRequest)
                .orElseThrow(fail -> logErrorAndThrowException(fail.getException()));
    }

    private CreatePublicationRequest generateCreatePublicationRequest(DocTp docTp) {
        CreatePublicationRequest createPublicationRequest = new CreatePublicationRequest();
        createPublicationRequest.setAdditionalIdentifiers(generateAdditionalIdentifiers(docTp));
        return createPublicationRequest;
    }

    private Set<AdditionalIdentifier> generateAdditionalIdentifiers(DocTp docTp) {
        return extractItemIdentifiers(docTp)
                .stream()
                .filter(this::isScopusIdentifier)
                .map(this::toAdditionalIdentifier)
                .collect(Collectors.toSet());
    }

    private List<ItemidTp> extractItemIdentifiers(DocTp docTp) {
        return docTp.getItem()
                .getItem()
                .getBibrecord()
                .getItemInfo()
                .getItemidlist()
                .getItemid();
    }

    private boolean isScopusIdentifier(ItemidTp itemIdTp) {
        return itemIdTp.getIdtype().equalsIgnoreCase(ScopusConstants.SCOPUS_ITEM_IDENTIFIER_SCP_FIELD_NAME);
    }

    private AdditionalIdentifier toAdditionalIdentifier(ItemidTp itemIdTp) {
        return new AdditionalIdentifier(ScopusConstants.ADDITIONAL_IDENTIFIERS_SCOPUS_ID_SOURCE_NAME, itemIdTp.getValue());
    }


    private DocTp parseXmlFile(String file) {
        return JAXB.unmarshal(new StringReader(file), DocTp.class);
    }

    private RuntimeException logErrorAndThrowException(Exception exception) {
        logger.error(exception.getMessage());
        return exception instanceof RuntimeException
                ? (RuntimeException) exception
                : new RuntimeException(exception);
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
