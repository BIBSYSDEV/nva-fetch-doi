package no.unit.nva.doi.transformer;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static java.util.function.Predicate.not;
import static no.unit.nva.doi.transformer.model.crossrefmodel.CrossrefDate.DAY_INDEX;
import static no.unit.nva.doi.transformer.model.crossrefmodel.CrossrefDate.FROM_DATE_INDEX_IN_DATE_ARRAY;
import static no.unit.nva.doi.transformer.model.crossrefmodel.CrossrefDate.MONTH_INDEX;
import static no.unit.nva.doi.transformer.model.crossrefmodel.CrossrefDate.YEAR_INDEX;
import static no.unit.nva.doi.transformer.utils.CrossrefType.BOOK;
import static no.unit.nva.doi.transformer.utils.CrossrefType.BOOK_CHAPTER;
import static no.unit.nva.doi.transformer.utils.CrossrefType.JOURNAL_ARTICLE;
import static no.unit.nva.doi.transformer.utils.CrossrefType.getByType;
import static nva.commons.core.StringUtils.isNotEmpty;
import static nva.commons.core.attempt.Try.attempt;
import java.net.URI;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import no.unit.nva.doi.fetch.exceptions.UnsupportedDocumentTypeException;
import no.unit.nva.doi.transformer.language.LanguageMapper;
import no.unit.nva.doi.transformer.language.SimpleLanguageDetector;
import no.unit.nva.doi.transformer.model.crossrefmodel.CrossRefDocument;
import no.unit.nva.doi.transformer.model.crossrefmodel.CrossrefAffiliation;
import no.unit.nva.doi.transformer.model.crossrefmodel.CrossrefContributor;
import no.unit.nva.doi.transformer.model.crossrefmodel.CrossrefDate;
import no.unit.nva.doi.transformer.model.crossrefmodel.Isxn;
import no.unit.nva.doi.transformer.model.crossrefmodel.Link;
import no.unit.nva.doi.transformer.utils.CrossrefType;
import no.unit.nva.doi.transformer.utils.IsbnCleaner;
import no.unit.nva.doi.transformer.utils.IssnCleaner;
import no.unit.nva.doi.transformer.utils.PublicationType;
import no.unit.nva.doi.transformer.utils.StringUtils;
import no.unit.nva.doi.transformer.utils.TextLang;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.model.Contributor;
import no.unit.nva.model.EntityDescription;
import no.unit.nva.model.Identity;
import no.unit.nva.model.Organization;
import no.unit.nva.model.Publication;
import no.unit.nva.model.PublicationDate;
import no.unit.nva.model.Reference;
import no.unit.nva.model.ResearchProject;
import no.unit.nva.model.ResourceOwner;
import no.unit.nva.model.Username;
import no.unit.nva.model.associatedartifacts.AssociatedArtifactList;
import no.unit.nva.model.contexttypes.Anthology;
import no.unit.nva.model.contexttypes.Book;
import no.unit.nva.model.contexttypes.PublicationContext;
import no.unit.nva.model.contexttypes.UnconfirmedJournal;
import no.unit.nva.model.contexttypes.UnconfirmedPublisher;
import no.unit.nva.model.contexttypes.UnconfirmedSeries;
import no.unit.nva.model.exceptions.InvalidIsbnException;
import no.unit.nva.model.exceptions.InvalidIssnException;
import no.unit.nva.model.instancetypes.PublicationInstance;
import no.unit.nva.model.instancetypes.book.AcademicMonograph;
import no.unit.nva.model.instancetypes.book.BookAnthology;
import no.unit.nva.model.instancetypes.book.BookMonograph;
import no.unit.nva.model.instancetypes.chapter.AcademicChapter;
import no.unit.nva.model.instancetypes.journal.AcademicArticle;
import no.unit.nva.model.instancetypes.journal.JournalArticle;
import no.unit.nva.model.pages.MonographPages;
import no.unit.nva.model.pages.Range;
import no.unit.nva.model.role.Role;
import no.unit.nva.model.role.RoleType;
import nva.commons.core.JacocoGenerated;
import nva.commons.core.attempt.Failure;
import nva.commons.core.exceptions.ExceptionUtils;
import nva.commons.doi.DoiConverter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SuppressWarnings({"PMD.GodClass", "PMD.CouplingBetweenObjects"})
public class CrossRefConverter extends AbstractConverter {

    public static final String INVALID_ENTRY_ERROR = "The entry is empty or has no title";
    public static final URI CROSSEF_URI = URI.create("https://www.crossref.org/");
    public static final String CROSSREF = "crossref";
    public static final String UNRECOGNIZED_TYPE_MESSAGE = "The publication type \"%s\" was not recognized";
    public static final String MISSING_DATE_FIELD_ISSUED =
        "CrossRef document does not contain required date field 'issued'";
    public static final int FIRST_MONTH_IN_YEAR = 1;
    public static final int FIRST_DAY_IN_MONTH = 1;
    public static final String CANNOT_CREATE_REFERENCE_FOR_PUBLICATION = ", cannot create reference for publication";
    public static final String NULL_SERIES_NUMBER = null;
    private static final Logger logger = LoggerFactory.getLogger(CrossRefConverter.class);
    private static final String DEFAULT_LANGUAGE_ENGLISH = "en";
    private static final URI UNDEFINED_AFFILIATION = null;

    public CrossRefConverter() {
        this(new DoiConverter());
    }

    public CrossRefConverter(DoiConverter doiConverter) {
        super(new SimpleLanguageDetector(), doiConverter);
    }

    /**
     * Creates a publication.
     *
     * @param document   a Java representation of a CrossRef document.
     * @param owner      the owning institution.
     * @param identifier the publication identifier.
     * @return an internal representation of the publication.
     */
    public Publication toPublication(CrossRefDocument document,
                                     String owner,
                                     UUID identifier) {

        if (document != null && hasTitle(document)) {
            return new Publication.Builder()
                       .withCreatedDate(extractInstantFromCrossrefDate(document.getCreated()))
                       .withModifiedDate(extractInstantFromCrossrefDate(document.getDeposited()))
                       .withPublishedDate(extractInstantFromCrossrefDate(document.getIssued()))
                       .withResourceOwner(new ResourceOwner(new Username(owner), UNDEFINED_AFFILIATION))
                       .withDoi(extractDOI(document)) // Cheating by using URL not DOI ?
                       .withIdentifier(new SortableIdentifier(identifier.toString()))
                       .withPublisher(extractAndCreatePublisher(document))
                       .withStatus(DEFAULT_NEW_PUBLICATION_STATUS)
                       .withIndexedDate(extractInstantFromCrossrefDate(document.getIndexed()))
                       .withLink(extractFulltextLinkAsUri(document))
                       .withProjects(createProjects())
                       .withAssociatedArtifacts(createAssociatedArtifactList())
                       .withEntityDescription(new EntityDescription.Builder()
                                                  .withContributors(toContributors(document))
                                                  .withPublicationDate(extractIssuedDate(document))
                                                  .withMainTitle(extractTitle(document))
                                                  .withAlternativeTitles(extractAlternativeTitles(document))
                                                  .withAbstract(extractAbstract(document))
                                                  .withLanguage(extractLanguage(document))
                                                  .withNpiSubjectHeading(extractNpiSubjectHeading())
                                                  .withTags(extractSubject(document))
                                                  .withReference(extractReference(document))
                                                  .withMetadataSource(extractMetadataSource(document))
                                                  .build())
                       .build();
        }
        throw new IllegalArgumentException(INVALID_ENTRY_ERROR);
    }

    protected List<Contributor> toContributors(CrossRefDocument document) {

        List<Contributor> contributors = new ArrayList<>();
        contributors.addAll(toContributorsWithRole(document.getAuthor(), new RoleType(Role.CREATOR)));
        contributors.addAll(toContributorsWithRole(document.getEditor(), new RoleType(Role.EDITOR)));
        return contributors;
    }

    protected List<Contributor> toContributorsWithRole(List<CrossrefContributor> crossrefContributors, RoleType role) {
        List<CrossrefContributor> contributorsWithName = removeContributorsWithoutNames(crossrefContributors);
        return convertCrossRefContributorsToNvaContributors(contributorsWithName, role);
    }

    private List<Contributor> convertCrossRefContributorsToNvaContributors(
        List<CrossrefContributor> crossrefContributors,
        RoleType role) {
        List<Contributor> nvaContributors = new ArrayList<>();
        for (int index = 0; index < crossrefContributors.size(); index++) {
            int authorSequence = index + 1;
            Contributor currentContributor =
                toContributorWithRole(crossrefContributors.get(index), role, authorSequence);
            nvaContributors.add(currentContributor);
        }
        return nvaContributors;
    }

    private List<CrossrefContributor> removeContributorsWithoutNames(
        List<CrossrefContributor> crossrefContributors) {
        return Optional.ofNullable(crossrefContributors)
                   .stream()
                   .flatMap(Collection::stream)
                   .filter(this::hasName)
                   .collect(Collectors.toList());
    }

    private boolean hasName(CrossrefContributor crossrefContributor) {
        return nva.commons.core.StringUtils.isNotBlank(crossrefContributor.getFamilyName())
               || nva.commons.core.StringUtils.isNotBlank(crossrefContributor.getGivenName());
    }

    private URI extractDOI(CrossRefDocument document) {
        final String urlString = document.getUrl();
        return isNotEmpty(urlString) ? URI.create(urlString) : null;
    }

    private URI extractMetadataSource(CrossRefDocument document) {
        if (containsCrossrefAsSource(document)) {
            return CROSSEF_URI;
        } else {
            return tryCreatingUri(document.getSource()).orElse(null);
        }
    }

    private boolean containsCrossrefAsSource(CrossRefDocument document) {
        return Optional.ofNullable(document.getSource())
                   .map(str -> str.toLowerCase(Locale.getDefault()))
                   .filter(str -> str.contains(CROSSREF))
                   .isPresent();
    }

    private Optional<URI> tryCreatingUri(String source) {
        try {
            return Optional.of(URI.create(source));
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    private Reference extractReference(CrossRefDocument document) {
        try {
            PublicationInstance<?> instance = extractPublicationInstance(document);
            PublicationContext context = extractPublicationContext(document);
            return new Reference.Builder()
                       .withDoi(doiConverter.toUri(document.getDoi()))
                       .withPublishingContext(context)
                       .withPublicationInstance(instance)
                       .build();
        } catch (InvalidIssnException | InvalidIsbnException e) {
            // The exceptions are logged elsewhere
            return null;
        } catch (UnsupportedDocumentTypeException e) {
            logger.error(String.format(UNRECOGNIZED_TYPE_MESSAGE + CANNOT_CREATE_REFERENCE_FOR_PUBLICATION,
                                       document.getType()));
            return null;
        }
    }

    private Range extractRangePages(CrossRefDocument document) {
        return StringUtils.parsePage(document.getPage());
    }

    private MonographPages extractMonographPages(CrossRefDocument document) {
        String pages = document.getPage();
        return isNull(pages)
                   ? new MonographPages.Builder().build()
                   : new MonographPages.Builder().withPages(pages).build();
    }

    private PublicationContext extractPublicationContext(CrossRefDocument document)
        throws UnsupportedDocumentTypeException, InvalidIssnException, InvalidIsbnException {

        CrossrefType crossrefType = getByType(document.getType());
        return createContext(document, crossrefType.getPublicationType());
    }

    @JacocoGenerated
    private PublicationContext createContext(CrossRefDocument document, PublicationType publicationType)
        throws UnsupportedDocumentTypeException, InvalidIssnException, InvalidIsbnException {
        if (publicationType == PublicationType.JOURNAL_CONTENT) {
            return createJournalContext(document);
        } else if (publicationType == PublicationType.BOOK) {
            return createBookContext(document);
        } else if (publicationType == PublicationType.BOOK_CHAPTER) {
            return createAnthologyContext();
        } else {
            throw new UnsupportedDocumentTypeException(String.format(UNRECOGNIZED_TYPE_MESSAGE, document.getType()));
        }
    }

    private Book createBookContext(CrossRefDocument document) throws InvalidIsbnException, InvalidIssnException {
        return new Book(
            new UnconfirmedSeries(
                extractSeriesTitle(document),
                extractPrintIssn(document),
                extractOnlineIssn(document)),
            NULL_SERIES_NUMBER,
            new UnconfirmedPublisher(extractPublisherName(document)),
            extractIsbnList(document)
        );
    }

    private Anthology createAnthologyContext() {
        return new Anthology();
    }

    private UnconfirmedJournal createJournalContext(CrossRefDocument document) throws InvalidIssnException {
        // TODO actually call the Channel Register API and get the relevant details
        return new UnconfirmedJournal(
            extractJournalTitle(document),
            extractPrintIssn(document),
            extractOnlineIssn(document)
        );
    }

    private List<String> extractIsbnList(CrossRefDocument document) {
        // Document can have both 'ISBN' and 'isbn-type'
        List<Isxn> isbnType = document.getIsbnType();
        final List<String> isbn = document.getIsbn();
        if (isEmptyOrNullIsbn(isbnType, isbn)) {
            return Collections.emptyList();
        }
        Set<String> isbnCandidates = new HashSet<>();
        if (nonNull(isbnType)) {
            isbnCandidates.addAll(isbnType.stream()
                                      .map(Isxn::getValue)
                                      .map(IsbnCleaner::clean)
                                      .filter(Objects::nonNull)
                                      .collect(Collectors.toSet()));
        }
        if (nonNull(isbn)) {
            isbnCandidates.addAll(isbn.stream()
                                      .map(IsbnCleaner::clean)
                                      .filter(Objects::nonNull)
                                      .collect(Collectors.toSet()));
        }

        return isbnCandidates.isEmpty() ? Collections.emptyList() : new ArrayList<>(isbnCandidates);
    }

    private boolean isEmptyOrNullIsbn(List<Isxn> isbnType, List<String> isbn) {
        return (isNull(isbnType) || isbnType.isEmpty()) && (isNull(isbn) || isbn.isEmpty());
    }

    private String extractSeriesTitle(CrossRefDocument document) {
        if (isNull(document.getContainerTitle()) || document.getContainerTitle().isEmpty()) {
            return null;
        }
        return document.getContainerTitle().stream()
                   .findFirst()
                   .orElse(null);
    }

    private String extractPublisherName(CrossRefDocument document) {
        return document.getPublisher();
    }

    private Organization extractAndCreatePublisher(CrossRefDocument document) {
        return Optional.ofNullable(document.getPublisher())
                   .filter(nva.commons.core.StringUtils::isNotEmpty)
                   .map(this::getOrganization)
                   .orElse(null);
    }

    private Organization getOrganization(String publisher) {
        return new Organization.Builder().withLabels(Map.of("name", publisher)).build();
    }

    private String extractPrintIssn(CrossRefDocument document) {
        return IssnCleaner.clean(filterIssnsByType(document, Isxn.IsxnType.PRINT));
    }

    private String extractOnlineIssn(CrossRefDocument document) {
        return IssnCleaner.clean(filterIssnsByType(document, Isxn.IsxnType.ELECTRONIC));
    }

    private String filterIssnsByType(CrossRefDocument crossRefDocument, Isxn.IsxnType type) {
        List<Isxn> issns = crossRefDocument.getIssnType();
        if (isNull(issns) || issns.isEmpty()) {
            return null;
        }
        return issns.stream().filter(issn -> issn.getType().equals(type))
                   .map(Isxn::getValue)
                   .findAny()
                   .orElse(null);
    }

    private PublicationInstance<?> extractPublicationInstance(CrossRefDocument document)
        throws UnsupportedDocumentTypeException {
        CrossrefType byType = getByType(document.getType());
        if (byType == JOURNAL_ARTICLE) {
            return createJournalArticle(document);
        } else if (byType == BOOK) {
            if (hasEditor(document)) {
                return createBookAnthology(document);
            } else {
                return createBookMonograph(document);
            }
        } else if (byType == BOOK_CHAPTER) {
            return createChapterArticle(document);
        }
        throw new UnsupportedDocumentTypeException(String.format(UNRECOGNIZED_TYPE_MESSAGE, document.getType()));
    }

    private boolean hasEditor(CrossRefDocument document) {
        return nonNull(document.getEditor()) && !document.getEditor().isEmpty();
    }

    private BookAnthology createBookAnthology(CrossRefDocument document) {
        return new BookAnthology(extractMonographPages(document));
    }

    private BookMonograph createBookMonograph(CrossRefDocument document) {
        return new AcademicMonograph(extractMonographPages(document));
    }

    private AcademicChapter createChapterArticle(CrossRefDocument document) {
        return new AcademicChapter(extractRangePages(document));
    }

    private JournalArticle createJournalArticle(CrossRefDocument document) {
        return new AcademicArticle(extractRangePages(document), document.getVolume(), document.getIssue(), null);
    }

    private String extractJournalTitle(CrossRefDocument document) {
        return Optional.ofNullable(document.getContainerTitle())
                   .stream()
                   .flatMap(Collection::stream)
                   .findFirst()
                   .orElse(null);
    }

    private URI extractLanguage(CrossRefDocument document) {
        return LanguageMapper.getUriFromIsoAsOptional(document.getLanguage()).orElse(null);
    }

    private String extractAbstract(CrossRefDocument document) {
        return Optional.ofNullable(document.getAbstractText())
                   .map(StringUtils::removeXmlTags)
                   .orElse(null);
    }

    private Map<String, String> extractAlternativeTitles(CrossRefDocument document) {
        String mainTitle = extractTitle(document);
        return document.getTitle().stream()
                   .filter(not(title -> title.equals(mainTitle)))
                   .map(this::detectLanguage)
                   .collect(Collectors.toConcurrentMap(TextLang::getText, e -> e.getLanguage().toString()));
    }

    private boolean hasTitle(CrossRefDocument document) {
        return document.getTitle() != null && !document.getTitle().isEmpty();
    }

    private String extractTitle(CrossRefDocument document) {
        return getMainTitle(document.getTitle().stream());
    }

    /**
     * For more details about how date is extracted see {@link CrossrefDate}.
     *
     * @param document A crossref JSON document
     * @return The earliest year found in publication dates
     */
    private PublicationDate extractIssuedDate(CrossRefDocument document) {
        final CrossrefDate issued = document.getIssued();
        if (hasValue(issued)) {
            return partialDateToPublicationDate(issued.getDateParts()[FROM_DATE_INDEX_IN_DATE_ARRAY]);
        } else {
            logger.warn(MISSING_DATE_FIELD_ISSUED);
            return null;
        }
    }

    private boolean hasValue(CrossrefDate crossrefDate) {
        return !isNull(crossrefDate)
               && crossrefDate.hasYear(crossrefDate.getDateParts()[FROM_DATE_INDEX_IN_DATE_ARRAY]);
    }

    /**
     * Convert this date given in date-parts to a PublicationDate. For a partial-date data-parts is the only data set,
     * not timestamp or date-time. Only year is mandatory, the rest defaults to 1 it not set.
     *
     * @return PublicationDate containing data from this crossref date
     */
    @JacocoGenerated
    private PublicationDate partialDateToPublicationDate(int... fromDatePart) {
        final int year = fromDatePart[YEAR_INDEX];
        final int month = fromDatePart.length > MONTH_INDEX ? fromDatePart[MONTH_INDEX] : FIRST_MONTH_IN_YEAR;
        final int day = fromDatePart.length > DAY_INDEX ? fromDatePart[DAY_INDEX] : FIRST_DAY_IN_MONTH;
        return new PublicationDate.Builder()
                   .withYear(String.valueOf(year))
                   .withMonth(String.valueOf(month))
                   .withDay(String.valueOf(day))
                   .build();
    }

    private Instant extractInstantFromCrossrefDate(CrossrefDate crossrefDate) {
        return nonNull(crossrefDate) ? crossrefDate.toInstant() : null;
    }

    /**
     * Coverts an author to a Contributor with Role (from external model to internal).
     *
     * @param contributor          the Contributor.
     * @param role                 Assigned role for the contributor
     * @param registrationSequence sequence in case where the Author object does not contain a valid sequence entry
     * @return an Optional Contributor object if the transformation succeeds.
     */
    private Contributor toContributorWithRole(CrossrefContributor contributor,
                                              RoleType role,
                                              int registrationSequence) {
        Identity identity = new Identity.Builder()
                                .withName(toName(contributor.getGivenName(), contributor.getFamilyName()))
                                .withOrcId(contributor.getOrcid())
                                .build();
        final Contributor.Builder contributorBuilder = new Contributor.Builder();
        if (nonNull(contributor.getAffiliation())) {
            contributorBuilder.withAffiliations(getAffiliations(contributor.getAffiliation()));
        }
        return attempt(() -> contributorBuilder
                                 .withIdentity(identity)
                                 .withRole(role)
                                 .withSequence(registrationSequence)
                                 .build())
                   .orElseThrow();
    }

    private List<Organization> getAffiliations(List<CrossrefAffiliation> crossrefAffiliations) {
        return crossrefAffiliations.stream()
                   .map(CrossrefAffiliation::getName)
                   .map(this::createOrganization)
                   .collect(Collectors.toList());
    }

    private Organization createOrganization(String name) {
        Organization organization = new Organization();
        Map<String, String> labels = Map.of(DEFAULT_LANGUAGE_ENGLISH, name);
        organization.setLabels(labels);
        return organization;
    }

    private List<String> extractSubject(CrossRefDocument document) {
        return Optional.ofNullable(document.getSubject()).orElse(Collections.emptyList());
    }

    private String extractNpiSubjectHeading() {
        return null;
    }

    private AssociatedArtifactList createAssociatedArtifactList() {
        return new AssociatedArtifactList(Collections.emptyList());
    }

    private List<ResearchProject> createProjects() {
        return Collections.emptyList();
    }

    private URI extractFulltextLinkAsUri(CrossRefDocument document) {
        return extractFirstLinkToSourceDocument(document.getLink()).orElse(null);
    }

    private Optional<URI> extractFirstLinkToSourceDocument(List<Link> links) {
        return hasLink(links) ? getFirstLinkAsUri(links) : Optional.empty();
    }

    private Optional<URI> getFirstLinkAsUri(List<Link> links) {
        return links.stream()
                   .findFirst()
                   .map(Link::getUrl)
                   .map(attempt(URI::create))
                   .flatMap(attempt -> attempt.toOptional(this::handleMalformedUriException));
    }

    private void handleMalformedUriException(Failure<URI> fail) {
        logger.warn(ExceptionUtils.stackTraceInSingleLine(fail.getException()));
    }

    private boolean hasLink(List<Link> links) {
        return nonNull(links) && !links.isEmpty();
    }
}
