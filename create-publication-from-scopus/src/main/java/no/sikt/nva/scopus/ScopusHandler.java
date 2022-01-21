package no.sikt.nva.scopus;

import static no.sikt.nva.scopus.ScopusSourceType.JOURNAL;
import static nva.commons.core.attempt.Try.attempt;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.S3Event;
import jakarta.xml.bind.JAXB;
import java.io.StringReader;
import java.net.URI;
import java.util.Objects;

import no.scopus.generated.DocTp;
import no.scopus.generated.IssnTp;
import no.scopus.generated.SourceTp;
import no.unit.nva.metadata.CreatePublicationRequest;
import no.unit.nva.model.EntityDescription;
import no.unit.nva.model.Reference;
import no.unit.nva.model.contexttypes.PublicationContext;
import no.unit.nva.model.contexttypes.UnconfirmedJournal;
import no.unit.nva.model.exceptions.InvalidIssnException;
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
    public static final String EMPTY_STRING = "";
    public static final String ISSN_TYPE_ELECTRONIC = "electronic";
    public static final String ISSN_TYPE_PRINT = "print";
    public static final String DASH = "-";
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
        CreatePublicationRequest request = attempt(() -> readFile(event))
                .map(this::parseXmlFile)
                .map(this::extractMetadata)
                .orElseThrow(fail -> logErrorAndThrowException(fail.getException()));
        return request;
    }

    private CreatePublicationRequest extractMetadata(DocTp docTp) {
        CreatePublicationRequest request = new CreatePublicationRequest();
        EntityDescription entityDescription = new EntityDescription();
        request.setEntityDescription(entityDescription);
        Reference reference = new Reference();
        entityDescription.setReference(reference);
        reference.setPublicationContext(getPublicationContext(docTp));
        return request;
    }

    private PublicationContext getPublicationContext(DocTp docTp) {
        PublicationContext publicationContext = null;
        ScopusSourceType scopusSourceType = ScopusSourceType.valueOfCode(docTp.getMeta().getSrctype());
        if (Objects.requireNonNull(scopusSourceType) == JOURNAL) {
            StringBuilder publisherName = new StringBuilder();
            SourceTp sourceTp = docTp.getItem().getItem().getBibrecord().getHead().getSource();
            sourceTp.getSourcetitle().getContent().forEach(publisherName::append);
            try {
                String electronicIssn = EMPTY_STRING;
                String printedIssn = EMPTY_STRING;
                for (IssnTp issnTp : sourceTp.getIssn()) {
                    String issn = issnTp.getContent();
                    issn = issn.substring(0, 4) + DASH + issn.substring(4);
                    if (ISSN_TYPE_ELECTRONIC.equals(issnTp.getType())) {
                        electronicIssn = issn;
                    } else if (ISSN_TYPE_PRINT.equals(issnTp.getType())) {
                        printedIssn = issn;
                    }
                }
                publicationContext = new UnconfirmedJournal(publisherName.toString(), printedIssn,
                        electronicIssn);
            } catch (InvalidIssnException e) {
                logger.error(e.getMessage());
            }
        }
        return publicationContext;
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
