package no.sikt.nva.scopus;

import static java.util.Collections.emptyList;
import static java.util.Objects.nonNull;
import static no.sikt.nva.scopus.ScopusConstants.DOI_OPEN_URL_FORMAT;
import static no.sikt.nva.scopus.ScopusSourceType.JOURNAL;
import static nva.commons.core.attempt.Try.attempt;

import jakarta.xml.bind.JAXB;
import jakarta.xml.bind.JAXBElement;
import java.io.StringWriter;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import no.scopus.generated.AbstractTp;
import no.scopus.generated.AuthorGroupTp;
import no.scopus.generated.AuthorKeywordsTp;
import no.scopus.generated.AuthorTp;
import no.scopus.generated.CitationTypeTp;
import no.scopus.generated.CitationtypeAtt;
import no.scopus.generated.CollaborationTp;
import no.scopus.generated.DocTp;
import no.scopus.generated.InfTp;
import no.scopus.generated.ItemidTp;
import no.scopus.generated.SupTp;
import no.scopus.generated.IssnTp;
import no.scopus.generated.MetaTp;
import no.scopus.generated.SourceTp;
import no.scopus.generated.TitletextTp;
import no.scopus.generated.YesnoAtt;
import no.sikt.nva.scopus.exception.UnsupportedXmlElementException;
import no.unit.nva.metadata.CreatePublicationRequest;
import no.unit.nva.model.AdditionalIdentifier;
import no.unit.nva.model.Contributor;
import no.unit.nva.model.EntityDescription;
import no.unit.nva.model.Identity;
import no.unit.nva.model.Reference;
import no.unit.nva.model.contexttypes.PublicationContext;
import no.unit.nva.model.contexttypes.UnconfirmedJournal;
import no.unit.nva.model.exceptions.InvalidIssnException;
import no.unit.nva.model.instancetypes.PublicationInstance;
import no.unit.nva.model.instancetypes.journal.JournalArticle;
import no.unit.nva.model.pages.Pages;
import nva.commons.core.SingletonCollector;
import nva.commons.core.paths.UriWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SuppressWarnings("PMD.GodClass")
class ScopusConverter {

    private static final String MALFORMED_CONTENT_MESSAGE = "Malformed content, cannot parse: %s";
    public static final String DASH = "-";
    private final DocTp docTp;
    private static final Logger logger = LoggerFactory.getLogger(ScopusConverter.class);

    protected ScopusConverter(DocTp docTp) {
        this.docTp = docTp;
    }

    public CreatePublicationRequest generateCreatePublicationRequest() {
        CreatePublicationRequest createPublicationRequest = new CreatePublicationRequest();
        createPublicationRequest.setAdditionalIdentifiers(generateAdditionalIdentifiers());
        createPublicationRequest.setEntityDescription(generateEntityDescription());
        createPublicationRequest.setAuthorKeywordsXmlFormat(generateAuthorKeyWordsXml());
        return createPublicationRequest;
    }

    private String generateAuthorKeyWordsXml() {
        var authorKeywords = extractAuthorKeyWords();
        return nonNull(authorKeywords) ? marshallAuthorKeywords(authorKeywords) : null;
    }

    private AuthorKeywordsTp extractAuthorKeyWords() {
        return docTp.getItem().getItem().getBibrecord().getHead().getCitationInfo().getAuthorKeywords();
    }

    private String marshallAuthorKeywords(AuthorKeywordsTp authorKeywordsTp) {
        StringWriter sw = new StringWriter();
        JAXB.marshal(authorKeywordsTp, sw);
        return sw.toString();
    }

    private EntityDescription generateEntityDescription() {
        EntityDescription entityDescription = new EntityDescription();
        entityDescription.setReference(generateReference());
        entityDescription.setMainTitle(extractMainTitle());
        entityDescription.setAbstract(extractMainAbstract());
        entityDescription.setContributors(generateContributors());
        entityDescription.setTags(generatePlainTextTags());
        return entityDescription;
    }

    private String extractMainAbstract() {
        return getMainAbstract().map(this::marshallAbstract).orElse(null);
    }

    private Optional<AbstractTp> getMainAbstract() {
        return getAbstracts().stream().filter(this::isOriginalAbstract).findFirst();
    }

    private List<AbstractTp> getAbstracts() {
        return docTp.getItem().getItem().getBibrecord().getHead().getAbstracts().getAbstract();
    }

    private boolean isOriginalAbstract(AbstractTp abstractTp) {
        return YesnoAtt.Y.equals(abstractTp.getOriginal());
    }

    private String marshallAbstract(AbstractTp abstractTp) {
        StringWriter sw = new StringWriter();
        JAXB.marshal(abstractTp, sw);
        return sw.toString();
    }

    private List<String> generatePlainTextTags() {
        var authorKeywordsTp = extractAuthorKeyWords();
        return nonNull(authorKeywordsTp)
                   ? authorKeywordsTp
            .getAuthorKeyword()
            .stream()
            .map(keyword -> keyword.getContent()
                .stream()
                .map(this::extractContentString)
                .collect(Collectors.joining()))
            .collect(Collectors.toList())
                   : emptyList();
    }

    private String extractContentString(Object content) {
        if (content instanceof String) {
            return ((String) content).trim();
        } else if (content instanceof JAXBElement) {
            return extractContentString(((JAXBElement<?>) content).getValue());
        } else if (content instanceof SupTp) {
            return extractContentString(((SupTp) content).getContent());
        } else if (content instanceof InfTp) {
            return extractContentString(((InfTp) content).getContent());
        } else if (content instanceof ArrayList) {
            return ((ArrayList<?>) content).stream()
                .map(this::extractContentString)
                .collect(Collectors.joining());
        } else {
            throw new UnsupportedXmlElementException(String.format(MALFORMED_CONTENT_MESSAGE, content.getClass()));
        }
    }

    private String extractMainTitle() {
        return getMainTitleTextTp()
            .map(this::marshallMainTitleToXmlPreservingUnderlyingStructure)
            .orElse(null);
    }

    private List<Contributor> generateContributors() {
        return extractAuthorGroup()
            .stream()
            .map(this::generateContributorsFromAuthorGroup)
            .flatMap(Collection::stream)
            .collect(Collectors.toList());
    }

    private Reference generateReference() {
        Reference reference = new Reference();
        reference.setDoi(extractDOI());
        reference.setPublicationInstance(generatePublicationInstance());
        reference.setPublicationContext(getPublicationContext());
        return reference;
    }

    private PublicationInstance<? extends Pages> generatePublicationInstance() {
        var citationType = getCitationType();
        return citationType.map(this::convertCitationTypeToPublicationInstance).orElse(null);
    }

    private PublicationInstance<? extends Pages> convertCitationTypeToPublicationInstance(CitationTypeTp citationTypeTp) {
        return citationTypeTp.getCode() == CitationtypeAtt.AR
                   ? new JournalArticle()
                   : null;
    }

    private Optional<CitationTypeTp> getCitationType() {
        return docTp
            .getItem()
            .getItem()
            .getBibrecord()
            .getHead()
            .getCitationInfo()
            .getCitationType()
            .stream()
            .findFirst();
    }

    private URI extractDOI() {
        return new UriWrapper(DOI_OPEN_URL_FORMAT).addChild(docTp.getMeta().getDoi()).getUri();
    }

    private Optional<TitletextTp> getMainTitleTextTp() {
        return getTitleText()
            .stream()
            .filter(this::isTitleOriginal)
            .findFirst();
    }

    private boolean isTitleOriginal(TitletextTp titletextTp) {
        return titletextTp.getOriginal().equals(YesnoAtt.Y);
    }

    private List<TitletextTp> getTitleText() {
        return docTp.getItem().getItem().getBibrecord().getHead().getCitationTitle().getTitletext();
    }

    private List<Contributor> generateContributorsFromAuthorGroup(AuthorGroupTp authorGroupTp) {
        return authorGroupTp.getAuthorOrCollaboration()
            .stream()
            .map(this::generateContributorFromAuthorOrCollaboration)
            .collect(Collectors.toList());
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
        identity.setOrcId(author.getOrcid());
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

    private List<AuthorGroupTp> extractAuthorGroup() {
        return docTp.getItem().getItem().getBibrecord().getHead().getAuthorGroup();
    }

    private Set<AdditionalIdentifier> generateAdditionalIdentifiers() {
        return extractItemIdentifiers()
            .stream()
            .filter(this::isScopusIdentifier)
            .map(this::toAdditionalIdentifier)
            .collect(Collectors.toSet());
    }

    private String marshallMainTitleToXmlPreservingUnderlyingStructure(TitletextTp contents) {
        StringWriter sw = new StringWriter();
        JAXB.marshal(contents, sw);
        return sw.toString();
    }

    private List<ItemidTp> extractItemIdentifiers() {
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
        return new AdditionalIdentifier(ScopusConstants.ADDITIONAL_IDENTIFIERS_SCOPUS_ID_SOURCE_NAME,
                                        itemIdTp.getValue());
    }

    private PublicationContext getPublicationContext() {
        if (isJournal()) {
            return attempt(() -> createUnconfirmedJournal())
                .orElseThrow(fail -> logErrorAndThrowException(fail.getException()));
        }
        return ScopusConstants.EMPTY_PUBLICATION_CONTEXT;
    }

    private RuntimeException logErrorAndThrowException(Exception exception) {
        logger.error(exception.getMessage());
        return exception instanceof RuntimeException
                   ? (RuntimeException) exception
                   : new RuntimeException(exception);
    }

    private UnconfirmedJournal createUnconfirmedJournal() throws InvalidIssnException {
        var source = getSource();
        var sourceTitle = extractSourceTitle(source);
        var issnTpList = source.getIssn();
        var printIssn = findPrintIssn(issnTpList).orElse(null);
        var electronicIssn = findElectronicIssn(issnTpList).orElse(null);
        return new UnconfirmedJournal(sourceTitle, printIssn, electronicIssn);
    }

    private boolean isJournal() {
        return Optional.ofNullable(docTp)
            .map(DocTp::getMeta)
            .map(MetaTp::getSrctype)
            .map(srcTyp -> JOURNAL.equals(ScopusSourceType.valueOfCode(srcTyp)))
            .orElse(false);
    }

    private SourceTp getSource() {
        return docTp.getItem().getItem().getBibrecord().getHead().getSource();
    }

    private String extractSourceTitle(SourceTp sourceTp) {
        StringBuilder sourceTitle = new StringBuilder();
        sourceTp.getSourcetitle().getContent().forEach(sourceTitle::append);
        return sourceTitle.toString();
    }

    private Optional<String> findElectronicIssn(List<IssnTp> issnTpList) {
        return findIssn(issnTpList, ScopusConstants.ISSN_TYPE_ELECTRONIC);
    }

    private Optional<String> findPrintIssn(List<IssnTp> issnTpList) {
        return findIssn(issnTpList, ScopusConstants.ISSN_TYPE_PRINT);
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
}
