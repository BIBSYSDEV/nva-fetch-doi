package no.sikt.nva.scopus;

import static java.util.Collections.emptyList;
import static java.util.Objects.nonNull;
import static no.sikt.nva.scopus.ScopusConstants.ADDITIONAL_IDENTIFIERS_SCOPUS_ID_SOURCE_NAME;
import static no.sikt.nva.scopus.ScopusConstants.DOI_OPEN_URL_FORMAT;
import static no.sikt.nva.scopus.ScopusConstants.INF_END;
import static no.sikt.nva.scopus.ScopusConstants.INF_START;
import static no.sikt.nva.scopus.ScopusConstants.SUP_END;
import static no.sikt.nva.scopus.ScopusConstants.SUP_START;
import static nva.commons.core.StringUtils.isEmpty;
import jakarta.xml.bind.JAXBElement;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import no.scopus.generated.AbstractTp;
import no.scopus.generated.AuthorGroupTp;
import no.scopus.generated.AuthorKeywordTp;
import no.scopus.generated.AuthorKeywordsTp;
import no.scopus.generated.CitationInfoTp;
import no.scopus.generated.CitationTypeTp;
import no.scopus.generated.CitationtypeAtt;
import no.scopus.generated.CorrespondenceTp;
import no.scopus.generated.DateSortTp;
import no.scopus.generated.DocTp;
import no.scopus.generated.HeadTp;
import no.scopus.generated.InfTp;
import no.scopus.generated.PagerangeTp;
import no.scopus.generated.SourceTp;
import no.scopus.generated.SupTp;
import no.scopus.generated.TitletextTp;
import no.scopus.generated.VolissTp;
import no.scopus.generated.VolisspagTp;
import no.scopus.generated.YesnoAtt;
import no.sikt.nva.scopus.conversion.ContributorExtractor;
import no.sikt.nva.scopus.conversion.PublicationContextCreator;
import no.sikt.nva.scopus.exception.UnsupportedCitationTypeException;
import no.unit.nva.metadata.CreatePublicationRequest;
import no.unit.nva.metadata.service.MetadataService;
import no.unit.nva.model.AdditionalIdentifier;
import no.unit.nva.model.EntityDescription;
import no.unit.nva.model.PublicationDate;
import no.unit.nva.model.Reference;
import no.unit.nva.model.instancetypes.PublicationInstance;
import no.unit.nva.model.instancetypes.book.BookMonograph;
import no.unit.nva.model.instancetypes.journal.JournalArticle;
import no.unit.nva.model.instancetypes.journal.JournalArticleContentType;
import no.unit.nva.model.instancetypes.journal.JournalCorrigendum;
import no.unit.nva.model.instancetypes.journal.JournalLeader;
import no.unit.nva.model.instancetypes.journal.JournalLetter;
import no.unit.nva.model.pages.Pages;
import no.unit.nva.model.pages.Range;
import nva.commons.core.paths.UriWrapper;

@SuppressWarnings({"PMD.GodClass", "PMD.CouplingBetweenObjects"})
public class ScopusConverter {

    public static final String UNSUPPORTED_CITATION_TYPE_MESSAGE = "Unsupported citation type, cannot convert eid %s";
    private final DocTp docTp;
    private final MetadataService metadataService;

    protected ScopusConverter(DocTp docTp, MetadataService metadataService) {
        this.docTp = docTp;
        this.metadataService = metadataService;
    }

    public CreatePublicationRequest generateCreatePublicationRequest() {
        CreatePublicationRequest createPublicationRequest = new CreatePublicationRequest();
        createPublicationRequest.setAdditionalIdentifiers(generateAdditionalIdentifiers());
        createPublicationRequest.setEntityDescription(generateEntityDescription());
        return createPublicationRequest;
    }

    private Optional<AuthorKeywordsTp> extractAuthorKeyWords() {
        return Optional.ofNullable(extractHead())
            .map(HeadTp::getCitationInfo)
            .map(CitationInfoTp::getAuthorKeywords);
    }

    private HeadTp extractHead() {
        return docTp.getItem().getItem().getBibrecord().getHead();
    }

    private EntityDescription generateEntityDescription() {
        EntityDescription entityDescription = new EntityDescription();
        entityDescription.setReference(generateReference());
        entityDescription.setMainTitle(extractMainTitle());
        entityDescription.setAbstract(extractMainAbstract());
        entityDescription.setContributors(new ContributorExtractor(
            extractCorrespondence(), extractAuthorGroup()).generateContributors());
        entityDescription.setTags(generateTags());
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
        return getMainAbstract().flatMap(this::extractAbstractStringOrReturnNull).orElse(null);
    }

    private Optional<String> returnNullInsteadOfEmptyString(String input) {
        return isEmpty(input.trim()) ? Optional.empty() : Optional.of(input);
    }

    private Optional<String> extractAbstractStringOrReturnNull(AbstractTp abstractTp) {
        return returnNullInsteadOfEmptyString(extractAbstractString(abstractTp));
    }

    private String extractAbstractString(AbstractTp abstractTp) {
        return
            abstractTp
                .getPara()
                .stream()
                .map(para -> extractContentAndPreserveXmlSupAndInfTags(para.getContent()))
                .collect(Collectors.joining());
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

    private List<String> generateTags() {
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
        return keyword
            .getContent()
            .stream()
            .map(ScopusConverter::extractContentAndPreserveXmlSupAndInfTags)
            .collect(Collectors.joining());

    }

    public static String extractContentString(Object content) {
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
                .map(ScopusConverter::extractContentString)
                .collect(Collectors.joining());
        }
    }

    public static String extractContentAndPreserveXmlSupAndInfTags(Object content) {
        if (content instanceof String) {
            return ((String) content).trim();
        } else if (content instanceof JAXBElement) {
            return extractContentAndPreserveXmlSupAndInfTags(((JAXBElement<?>) content).getValue());
        } else if (content instanceof SupTp) {
            return SUP_START + extractContentAndPreserveXmlSupAndInfTags(((SupTp) content).getContent()) + SUP_END;
        } else if (content instanceof InfTp) {
            return INF_START + extractContentAndPreserveXmlSupAndInfTags(((InfTp) content).getContent()) + INF_END;
        } else {
            return ((ArrayList<?>) content).stream()
                .map(ScopusConverter::extractContentAndPreserveXmlSupAndInfTags)
                .collect(Collectors.joining());
        }
    }

    private String extractMainTitle() {
        return getMainTitleTextTp()
            .map(this::extractMainTitleContent)
            .orElse(null);
    }

    private String extractMainTitleContent(TitletextTp titletextTp) {
        return extractContentAndPreserveXmlSupAndInfTags(titletextTp.getContent());
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

    /*
    See enum explanation in "SCOPUS CUSTOM DATA DOCUMENTATION", copy can be found at
    https://isikt.sharepoint.com/:b:/s/Dovre/EQGVGp2Xn-RDvDi8zg3XFlQB6vo95nGLbINztJcXjStG5w?e=O9wQwB
     */
    private Optional<PublicationInstance<? extends Pages>> convertCitationTypeToPublicationInstance(
        CitationtypeAtt citationtypeAtt) {
        switch (citationtypeAtt) {
            case AR:
                return Optional.of(generateJournalArticle());
            case BK:
            case CH:
                return Optional.of(new BookMonograph());
            case ED:
                return Optional.of(generateJournalLeader());
            case ER:
                return Optional.of(generateJournalCorrigendum());
            case LE:
            case NO:
                return Optional.of(generateJournalLetter());
            case RE:
            case SH:
                return Optional.of(generateJournalArticle(JournalArticleContentType.REVIEW_ARTICLE));
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

    private JournalArticle generateJournalArticle(JournalArticleContentType contentType) {
        JournalArticle journalArticle = generateJournalArticle();
        journalArticle.setContentType(contentType);
        return journalArticle;
    }

    private JournalArticle generateJournalArticle() {
        JournalArticle journalArticle = new JournalArticle();
        extractPages().ifPresent(journalArticle::setPages);
        extractVolume().ifPresent(journalArticle::setVolume);
        extractIssue().ifPresent(journalArticle::setIssue);
        extractArticleNumber().ifPresent(journalArticle::setArticleNumber);
        return journalArticle;
    }

    private JournalLeader generateJournalLeader() {
        JournalLeader.Builder builder = new JournalLeader.Builder();
        extractPages().ifPresent(builder::withPages);
        extractVolume().ifPresent(builder::withVolume);
        extractIssue().ifPresent(builder::withIssue);
        extractArticleNumber().ifPresent(builder::withArticleNumber);
        return builder.build();
    }

    private JournalCorrigendum generateJournalCorrigendum() {
        JournalCorrigendum.Builder builder = new JournalCorrigendum.Builder();
        extractPages().ifPresent(builder::withPages);
        extractVolume().ifPresent(builder::withVolume);
        extractIssue().ifPresent(builder::withIssue);
        extractArticleNumber().ifPresent(builder::withArticleNumber);
        builder.withCorrigendumFor(ScopusConstants.DUMMY_URI);
        return builder.build();
    }

    private JournalLetter generateJournalLetter() {
        JournalLetter.Builder builder = new JournalLetter.Builder();
        extractPages().ifPresent(builder::withPages);
        extractVolume().ifPresent(builder::withVolume);
        extractIssue().ifPresent(builder::withIssue);
        extractArticleNumber().ifPresent(builder::withArticleNumber);
        return builder.build();
    }

    private Optional<Range> extractPages() {
        return getVolisspagTpStream()
            .filter(this::isPageRange)
            .map(this::extractPageRange)
            .findAny().orElse(Optional.empty());
    }

    private Stream<JAXBElement<?>> getVolisspagTpStream() {
        return Optional.ofNullable(getSourceTp().getVolisspag())
            .map(VolisspagTp::getContent)
            .orElse(emptyList())
            .stream();
    }

    private Optional<String> extractVolume() {
        return getVolisspagTpStream()
            .filter(this::isVolumeIssue)
            .map(this::extractVolumeValue)
            .findAny().orElse(Optional.empty());
    }

    private Optional<String> extractIssue() {
        return getVolisspagTpStream()
            .filter(this::isVolumeIssue)
            .map(this::extractIssueValue)
            .findAny().orElse(Optional.empty());
    }

    private Optional<String> extractArticleNumber() {
        return Optional.ofNullable(getSourceTp().getArticleNumber());
    }

    private boolean isVolumeIssue(JAXBElement<?> content) {
        return content.getValue() instanceof VolissTp;
    }

    private Optional<String> extractVolumeValue(JAXBElement<?> content) {
        return Optional.ofNullable(((VolissTp) content.getValue()).getVolume());
    }

    private Optional<String> extractIssueValue(JAXBElement<?> content) {
        return Optional.ofNullable(((VolissTp) content.getValue()).getIssue());
    }

    private SourceTp getSourceTp() {
        return docTp.getItem()
            .getItem()
            .getBibrecord()
            .getHead()
            .getSource();
    }

    private boolean isPageRange(JAXBElement<?> content) {
        return content.getValue() instanceof PagerangeTp;
    }

    private Optional<Range> extractPageRange(JAXBElement<?> content) {
        return Optional.of(new Range(((PagerangeTp) content.getValue()).getFirst(),
                                     ((PagerangeTp) content.getValue()).getLast()));
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

    private Set<AdditionalIdentifier> generateAdditionalIdentifiers() {
        return Set.of(extractScopusIdentifier());
    }

    private AdditionalIdentifier extractScopusIdentifier() {
        return new AdditionalIdentifier(ADDITIONAL_IDENTIFIERS_SCOPUS_ID_SOURCE_NAME, docTp.getMeta().getEid());

    }

    private List<AuthorGroupTp> extractAuthorGroup() {
        return extractHead().getAuthorGroup();
    }

    private List<CorrespondenceTp> extractCorrespondence() {
        return extractHead().getCorrespondence();
    }
}
