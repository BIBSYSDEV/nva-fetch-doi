package no.sikt.nva.scopus;

import static java.util.Collections.emptyList;
import static java.util.Objects.nonNull;
import static no.sikt.nva.scopus.ScopusConstants.DOI_OPEN_URL_FORMAT;
import static no.sikt.nva.scopus.ScopusConstants.ORCID_DOMAIN_URL;
import static nva.commons.core.StringUtils.isNotBlank;
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
import no.scopus.generated.AuthorKeywordTp;
import no.scopus.generated.AuthorKeywordsTp;
import no.scopus.generated.AuthorTp;
import no.scopus.generated.CitationInfoTp;
import no.scopus.generated.CitationTypeTp;
import no.scopus.generated.CitationtypeAtt;
import no.scopus.generated.CollaborationTp;
import no.scopus.generated.DateSortTp;
import no.scopus.generated.DocTp;
import no.scopus.generated.HeadTp;
import no.scopus.generated.InfTp;
import no.scopus.generated.ItemidTp;
import no.scopus.generated.SupTp;
import no.scopus.generated.TitletextTp;
import no.scopus.generated.YesnoAtt;
import no.sikt.nva.scopus.conversion.PublicationContextCreator;
import no.sikt.nva.scopus.exception.UnsupportedCitationTypeException;
import no.unit.nva.metadata.CreatePublicationRequest;
import no.unit.nva.metadata.service.MetadataService;
import no.unit.nva.model.AdditionalIdentifier;
import no.unit.nva.model.Contributor;
import no.unit.nva.model.EntityDescription;
import no.unit.nva.model.Identity;
import no.unit.nva.model.PublicationDate;
import no.unit.nva.model.Reference;
import no.unit.nva.model.instancetypes.PublicationInstance;
import no.unit.nva.model.instancetypes.book.BookMonograph;
import no.unit.nva.model.instancetypes.journal.JournalArticle;
import no.unit.nva.model.pages.Pages;
import nva.commons.core.paths.UriWrapper;

@SuppressWarnings("PMD.GodClass")
class ScopusConverter {

    public static final String UNSUPPORTED_CITATION_TYPE_MESSAGE = "Unsupported citation type, cannot convert eid %s";
    private final DocTp docTp;
    private final MetadataService metadataService;
    public static final String NAME_DELIMITER = ", ";

    protected ScopusConverter(DocTp docTp, MetadataService metadataService) {
        this.docTp = docTp;
        this.metadataService = metadataService;
    }

    public CreatePublicationRequest generateCreatePublicationRequest() {
        CreatePublicationRequest createPublicationRequest = new CreatePublicationRequest();
        createPublicationRequest.setAdditionalIdentifiers(generateAdditionalIdentifiers());
        createPublicationRequest.setEntityDescription(generateEntityDescription());
        createPublicationRequest.setAuthorKeywordsXmlFormat(generateAuthorKeyWordsXml());
        return createPublicationRequest;
    }

    private String generateAuthorKeyWordsXml() {
        return extractAuthorKeyWords()
            .map(this::marshallAuthorKeywords)
            .orElse(null);
    }

    private Optional<AuthorKeywordsTp> extractAuthorKeyWords() {
        return Optional.ofNullable(extractHead())
            .map(HeadTp::getCitationInfo)
            .map(CitationInfoTp::getAuthorKeywords);
    }

    private HeadTp extractHead() {
        return docTp.getItem().getItem().getBibrecord().getHead();
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
        entityDescription.setDate(extractPublicationDate());
        return entityDescription;
    }

    private PublicationDate extractPublicationDate() {
        var publicationDate = getDateSortTp();
        return new PublicationDate.Builder().withDay(publicationDate.getDay())
            .withMonth(publicationDate.getMonth())
            .withYear(publicationDate.getYear())
            .build();
    }

    /*
    According to the "SciVerse SCOPUS CUSTOM DATA DOCUMENTATION" dateSort contains the publication date if it exists,
     if not there are several rules to determine what's the second-best date is. See "SciVerse SCOPUS CUSTOM DATA
     DOCUMENTATION" for details.
     */
    private DateSortTp getDateSortTp() {
        return docTp.getItem().getItem().getProcessInfo().getDateSort();
    }

    private String extractMainAbstract() {
        return getMainAbstract().map(this::marshallAbstract).orElse(null);
    }

    private Optional<AbstractTp> getMainAbstract() {
        return nonNull(getAbstracts())
                   ? getAbstracts().stream().filter(this::isOriginalAbstract).findFirst()
                   : Optional.empty();
    }

    private List<AbstractTp> getAbstracts() {
        return nonNull(extractHead().getAbstracts())
                   ? extractHead().getAbstracts().getAbstract()
                   : null;
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
        return extractAuthorKeyWords()
            .map(this::extractKeywordsAsStrings)
            .orElse(emptyList());
    }

    private List<String> extractKeywordsAsStrings(AuthorKeywordsTp authorKeywordsTp) {
        return authorKeywordsTp
            .getAuthorKeyword()
            .stream()
            .map(this::extractConcatenatedKeywordString)
            .collect(Collectors.toList());
    }

    private String extractConcatenatedKeywordString(AuthorKeywordTp keyword) {
        return keyword.getContent().stream().map(this::extractContentString).collect(Collectors.joining());
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
        } else {
            return ((ArrayList<?>) content).stream()
                .map(this::extractContentString)
                .collect(Collectors.joining());
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
        reference.setPublicationInstance(generatePublicationInstance());
        reference.setDoi(extractDOI());
        reference.setPublicationContext(new PublicationContextCreator(metadataService, docTp).getPublicationContext());
        return reference;
    }

    private URI extractDOI() {
        return nonNull(docTp.getMeta().getDoi())
                   ? UriWrapper.fromUri(DOI_OPEN_URL_FORMAT).addChild(docTp.getMeta().getDoi()).getUri()
                   : null;
    }

    private PublicationInstance<? extends Pages> generatePublicationInstance() {
        return getCitationType()
            .flatMap(this::convertCitationTypeToPublicationInstance)
            .orElseThrow(this::getUnsupportedCitationTypeException);
    }

    private UnsupportedCitationTypeException getUnsupportedCitationTypeException() {
        return new UnsupportedCitationTypeException(
            String.format(UNSUPPORTED_CITATION_TYPE_MESSAGE, docTp.getMeta().getEid()));
    }

    private Optional<PublicationInstance<? extends Pages>> convertCitationTypeToPublicationInstance(
        CitationtypeAtt citationtypeAtt) {
        switch (citationtypeAtt) {
            case AR:
                return Optional.of(new JournalArticle());
            case BK:
            case CH:
                return Optional.of(new BookMonograph());
            default:
                return Optional.empty();
        }
    }

    private Optional<CitationtypeAtt> getCitationType() {
        return docTp
            .getItem()
            .getItem()
            .getBibrecord()
            .getHead()
            .getCitationInfo()
            .getCitationType()
            .stream()
            .findFirst()
            .map(CitationTypeTp::getCode);
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
        return extractHead().getCitationTitle().getTitletext();
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
        identity.setOrcId(getOrcidAsUriString(author));
        return new Contributor(identity, null, null, getSequenceNumber(author), false);
    }

    private String getOrcidAsUriString(AuthorTp authorTp) {
        return isNotBlank(authorTp.getOrcid()) ? craftOrcidUriString(authorTp.getOrcid()) : null;
    }

    private String craftOrcidUriString(String potentiallyMalformedOrcidString) {
        return potentiallyMalformedOrcidString.contains(ORCID_DOMAIN_URL)
                   ? potentiallyMalformedOrcidString
                   : ORCID_DOMAIN_URL + potentiallyMalformedOrcidString;
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
        return author.getPreferredName().getSurname() + NAME_DELIMITER + author.getPreferredName().getGivenName();
    }

    private String determineContributorName(CollaborationTp collaborationTp) {
        return collaborationTp.getIndexedName();
    }

    private List<AuthorGroupTp> extractAuthorGroup() {
        return extractHead().getAuthorGroup();
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
}
