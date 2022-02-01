package no.sikt.nva.scopus;

import static no.sikt.nva.scopus.ScopusSourceType.JOURNAL;
import static nva.commons.core.attempt.Try.attempt;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.S3Event;
import jakarta.xml.bind.JAXB;

import java.io.StringReader;
import java.net.URI;

import java.io.StringWriter;
import java.util.Optional;
import java.util.Collection;
import no.scopus.generated.AuthorGroupTp;
import no.scopus.generated.AuthorTp;
import no.scopus.generated.CollaborationTp;
import no.scopus.generated.DocTp;
import no.scopus.generated.ItemidTp;
import no.scopus.generated.TitletextTp;
import no.scopus.generated.YesnoAtt;
import no.unit.nva.metadata.CreatePublicationRequest;
import no.unit.nva.model.AdditionalIdentifier;
import no.unit.nva.model.Contributor;
import no.scopus.generated.IssnTp;
import no.scopus.generated.SourceTp;
import no.unit.nva.model.EntityDescription;
import no.unit.nva.model.Identity;
import no.unit.nva.model.Reference;
import no.unit.nva.model.contexttypes.PublicationContext;
import no.unit.nva.model.contexttypes.UnconfirmedJournal;
import no.unit.nva.model.exceptions.InvalidIssnException;
import no.unit.nva.s3.S3Driver;
import nva.commons.core.JacocoGenerated;
import nva.commons.core.SingletonCollector;
import nva.commons.core.paths.UriWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.services.s3.S3Client;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static no.sikt.nva.scopus.ScopusConstants.DOI_OPEN_URL_FORMAT;

public class ScopusHandler implements RequestHandler<S3Event, CreatePublicationRequest> {

    public static final int SINGLE_EXPECTED_RECORD = 0;
    public static final String S3_URI_TEMPLATE = "s3://%s/%s";
    private static final Logger logger = LoggerFactory.getLogger(ScopusHandler.class);
    public static final String ISSN_TYPE_ELECTRONIC = "electronic";
    public static final String ISSN_TYPE_PRINT = "print";
    public static final String DASH = "-";
    public static final PublicationContext EMPTY_PUBLICATION_CONTEXT = null;
    public static final String ERROR_MSG_ISSN_NOT_FOUND = "Could not find issn";
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
        entityDescription.setMainTitle(extractMainTitle(docTp));
        entityDescription.setContributors(generateContributors(docTp));
        return entityDescription;
    }

    private Reference generateReference(DocTp docTp) {
        Reference reference = new Reference();
        reference.setDoi(extractDOI(docTp));
        reference.setPublicationContext(getPublicationContext(docTp));
        return reference;
    }

    private List<TitletextTp> getTitleText(DocTp docTp) {
        return docTp.getItem().getItem().getBibrecord().getHead().getCitationTitle().getTitletext();
    }

    private String extractMainTitle(DocTp docTp) {
        return getMainTitleTextTp(docTp)
            .map(this::marshallMainTitleToXmlPreservingUnderlyingStructure)
            .orElse(null);
    }

    private Optional<TitletextTp> getMainTitleTextTp(DocTp docTp) {
        return getTitleText(docTp)
            .stream()
            .filter(this::isTitleOriginal)
            .findFirst();
    }

    private boolean isTitleOriginal(TitletextTp titletextTp) {
        return titletextTp.getOriginal().equals(YesnoAtt.Y);
    }

    private String marshallMainTitleToXmlPreservingUnderlyingStructure(TitletextTp contents) {
        StringWriter sw = new StringWriter();
        JAXB.marshal(contents, sw);
        return sw.toString();
    }

    private List<Contributor> generateContributors(DocTp docTp) {
        return extractAuthorGroup(docTp)
            .stream()
            .map(this::generateContributorsFromAuthorGroup)
            .flatMap(Collection::stream)
            .collect(Collectors.toList());
    }

    private List<Contributor> generateContributorsFromAuthorGroup(AuthorGroupTp authorGroupTp) {
        return authorGroupTp.getAuthorOrCollaboration()
            .stream()
            .map(this::generateContributorFromAuthorOrCollaboration)
            .collect(Collectors.toList());
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
        var identity = new Identity();
        identity.setName(determineContributorName(author));
        return new Contributor(identity, null, null, getSequenceNumber(author), false);
    }

    private Contributor generateContributorFromCollaborationTp(CollaborationTp collaboration) {
        var identity = new Identity();
        identity.setName(determineContributorName(collaboration));
        return new Contributor(identity, null, null, getSequenceNumber(collaboration), false);
    }

    private int getSequenceNumber(AuthorTp authorTp) {
        return Integer.parseInt(authorTp.getSeq());
    }

    private int getSequenceNumber(CollaborationTp collaborationTp) {
        return Integer.parseInt(collaborationTp.getSeq());
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

    private PublicationContext getPublicationContext(DocTp docTp) {
        if (isJournal(docTp)) {
            return attempt(() -> createUnconfirmedJournal(docTp))
                    .orElseThrow(fail -> logErrorAndThrowException(fail.getException()));
        }
        return EMPTY_PUBLICATION_CONTEXT;
    }

    private UnconfirmedJournal createUnconfirmedJournal(DocTp docTp) throws InvalidIssnException {
        var source = getSource(docTp);
        var sourceTitle = extractSourceTitle(source);
        var issnTpList = source.getIssn();
        var printIssn = findPrintIssn(issnTpList).orElse(null);
        var electronicIssn = findElectronicIssn(issnTpList).orElse(null);
        return new UnconfirmedJournal(sourceTitle, printIssn, electronicIssn);
    }

    private boolean isJournal(DocTp docTp) {
        return ScopusSourceType.valueOfCode(docTp.getMeta().getSrctype()) == JOURNAL;
    }

    private SourceTp getSource(DocTp docTp) {
        return docTp.getItem().getItem().getBibrecord().getHead().getSource();
    }

    private String extractSourceTitle(SourceTp sourceTp) {
        StringBuilder sourceTitle = new StringBuilder();
        sourceTp.getSourcetitle().getContent().forEach(sourceTitle::append);
        return sourceTitle.toString();
    }

    private Optional<String> findElectronicIssn(List<IssnTp> issnTpList) {
        return findIssn(issnTpList, ISSN_TYPE_ELECTRONIC);
    }

    private Optional<String> findPrintIssn(List<IssnTp> issnTpList) {
        return findIssn(issnTpList, ISSN_TYPE_PRINT);
    }

    private Optional<String> findIssn(List<IssnTp> issnTpList, String issnType) {
        return Optional.ofNullable(issnTpList.stream()
                .filter(issn -> issnType.equals(issn.getType()))
                .map(IssnTp::getContent)
                .map(this::addDashToIssn)
                .collect(SingletonCollector.collectOrElse(null)));
    }

    private String addDashToIssn(String issn) {
        return issn.contains(DASH) ? issn : issn.substring(0, 4) + DASH + issn.substring(4);
    }

    private DocTp parseXmlFile(String file) {
        return JAXB.unmarshal(new StringReader(file), DocTp.class);
    }

    @JacocoGenerated
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
