package no.sikt.nva.scopus;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.S3Event;
import jakarta.xml.bind.JAXB;
import java.util.ArrayList;
import no.scopus.generated.AuthorGroupTp;
import no.scopus.generated.AuthorTp;
import no.scopus.generated.CollaborationTp;
import no.scopus.generated.DocTp;
import no.scopus.generated.ItemidTp;
import no.unit.nva.metadata.CreatePublicationRequest;
import no.unit.nva.model.AdditionalIdentifier;
import no.unit.nva.model.Contributor;
import no.unit.nva.model.EntityDescription;
import no.unit.nva.model.Identity;
import no.unit.nva.model.Reference;
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

import static no.sikt.nva.scopus.ScopusConstants.DOI_OPEN_URL_FORMAT;
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
        createPublicationRequest.setEntityDescription(generateEntityDescription(docTp));
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

    private EntityDescription generateEntityDescription(DocTp docTp) {
        EntityDescription entityDescription = new EntityDescription();
        entityDescription.setReference(generateReference(docTp));
        entityDescription.setContributors(generateContributors(docTp));
        return entityDescription;
    }

    private Reference generateReference(DocTp docTp) {
        Reference reference = new Reference();
        reference.setDoi(extractDOI(docTp));
        return reference;
    }

    private List<Contributor> generateContributors(DocTp docTp) {
        List<Contributor> contributors = new ArrayList<>();
        extractAuthorGroup(docTp).forEach(authorGroupTp -> contributors.addAll(generateContributorsFromAuthorGroup(authorGroupTp)));
        return contributors;
    }

    private List<Contributor> generateContributorsFromAuthorGroup(AuthorGroupTp authorGroupTp){
        List<Contributor> contributors = new ArrayList<>();
        authorGroupTp
            .getAuthorOrCollaboration()
            .forEach(authorOrCollaboration ->
                         contributors.add(generateContributorFromAuthorOrCollaboration(authorOrCollaboration)));
        return contributors;
    }

    private List<AuthorGroupTp> extractAuthorGroup(DocTp docTp) {
        return docTp.getItem().getItem().getBibrecord().getHead().getAuthorGroup();
    }

    private Contributor generateContributorFromAuthorOrCollaboration(Object authorOrCollaboration) {
        return authorOrCollaboration instanceof AuthorTp
                   ? generateContributorFromAuthorTp((AuthorTp) authorOrCollaboration)
                   : generateContributorFromCollaborationTp(
                       (CollaborationTp) authorOrCollaboration);
    }

    private Contributor generateContributorFromAuthorTp(AuthorTp author) {
        var sequence = Integer.parseInt(author.getSeq());
        var identity = new Identity();
        identity.setName(determineContributorName(author));
        return new Contributor(identity, null, null, sequence, false);
    }

    private Contributor generateContributorFromCollaborationTp(CollaborationTp collaboration) {
        var sequence = Integer.parseInt(collaboration.getSeq());
        var identity = new Identity();
        identity.setName(determineContributorName(collaboration));
        return new Contributor(identity, null, null, sequence, false);
    }

    private String determineContributorName(AuthorTp author) {
        return author.getPreferredName().getIndexedName();
    }

    private String determineContributorName(CollaborationTp collaborationTp) {
        return collaborationTp.getIndexedName();
    }

    private URI extractDOI(DocTp docTp) {
        return new UriWrapper(DOI_OPEN_URL_FORMAT).addChild(docTp.getMeta().getDoi()).getUri();
    }

    private boolean isScopusIdentifier(ItemidTp itemIdTp) {
        return itemIdTp.getIdtype().equalsIgnoreCase(ScopusConstants.SCOPUS_ITEM_IDENTIFIER_SCP_FIELD_NAME);
    }

    private AdditionalIdentifier toAdditionalIdentifier(ItemidTp itemIdTp) {
        return new AdditionalIdentifier(ScopusConstants.ADDITIONAL_IDENTIFIERS_SCOPUS_ID_SOURCE_NAME,
                                        itemIdTp.getValue());
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
