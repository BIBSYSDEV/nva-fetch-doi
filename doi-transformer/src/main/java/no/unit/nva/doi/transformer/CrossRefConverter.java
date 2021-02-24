package no.unit.nva.doi.transformer;

import com.ibm.icu.text.RuleBasedNumberFormat;
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
import no.unit.nva.model.FileSet;
import no.unit.nva.model.Identity;
import no.unit.nva.model.Organization;
import no.unit.nva.model.Publication;
import no.unit.nva.model.PublicationDate;
import no.unit.nva.model.Reference;
import no.unit.nva.model.ResearchProject;
import no.unit.nva.model.Role;
import no.unit.nva.model.contexttypes.Book;
import no.unit.nva.model.contexttypes.Chapter;
import no.unit.nva.model.contexttypes.Journal;
import no.unit.nva.model.contexttypes.PublicationContext;
import no.unit.nva.model.exceptions.InvalidIsbnException;
import no.unit.nva.model.exceptions.InvalidIssnException;
import no.unit.nva.model.exceptions.MalformedContributorException;
import no.unit.nva.model.instancetypes.PublicationInstance;
import no.unit.nva.model.instancetypes.chapter.ChapterArticle;
import no.unit.nva.model.instancetypes.journal.JournalArticle;
import no.unit.nva.model.pages.Range;
import nva.commons.core.JacocoGenerated;
import nva.commons.core.attempt.Try;
import nva.commons.doi.DoiConverter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.URL;
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
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

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
    public static final String MISSING_CROSSREF_TYPE_IN_DOCUMENT = "Missing crossref type in document";
    private static final Logger logger = LoggerFactory.getLogger(CrossRefConverter.class);
    private static final String DEFAULT_LANGUAGE_ENGLISH = "en";
    public static final String CANNOT_CREATE_REFERENCE_FOR_PUBLICATION = ", cannot create reference for publication";
    public static final String HANDLING_ISSN_ISBN_CANNOT_CREATE_REFERENCE =
            "Error handling ISSN/ISBN" + CANNOT_CREATE_REFERENCE_FOR_PUBLICATION;

    public CrossRefConverter() {
        super(new SimpleLanguageDetector(), new DoiConverter());
    }

    /**
     * Creates a publication.
     *
     * @param document   a Java representation of a CrossRef document.
     * @param owner      the owning institution.
     * @param identifier the publication identifier.
     * @return an internal representation of the publication.
     * @throws UnsupportedDocumentTypeException thrown if a provided documentType is provided.
     */
    public Publication toPublication(CrossRefDocument document,
                                     String owner,
                                     UUID identifier) {

        if (document != null && hasTitle(document)) {
            return new Publication.Builder()
                    .withCreatedDate(extractInstantFromCrossrefDate(document.getCreated()))
                    .withModifiedDate(extractInstantFromCrossrefDate(document.getDeposited()))
                    .withPublishedDate(extractInstantFromCrossrefDate(document.getIssued()))
                    .withOwner(owner)
                    .withDoi(extractDOI(document)) // Cheating by using URL not DOI ?
                    .withIdentifier(new SortableIdentifier(identifier.toString()))
                    .withPublisher(extractAndCreatePublisher(document))
                    .withStatus(DEFAULT_NEW_PUBLICATION_STATUS)
                    .withIndexedDate(extractInstantFromCrossrefDate(document.getIndexed()))
                    .withLink(extractFulltextLinkAsURI(document))
                    .withProjects(createProjects())
                    .withFileSet(createFilseSet())
                    .withEntityDescription(new EntityDescription.Builder()
                            .withContributors(toContributors(document))
                            .withDate(extractIssuedDate(document))
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
            logger.error(HANDLING_ISSN_ISBN_CANNOT_CREATE_REFERENCE);
            return null;
        } catch (UnsupportedDocumentTypeException e) {
            logger.error(String.format(UNRECOGNIZED_TYPE_MESSAGE + CANNOT_CREATE_REFERENCE_FOR_PUBLICATION,
                    document.getType()));
            return null;
        }
    }

    private Range extractPages(CrossRefDocument document) {
        return StringUtils.parsePage(document.getPage());
    }

    private PublicationContext extractPublicationContext(CrossRefDocument document)
            throws UnsupportedDocumentTypeException, InvalidIssnException, InvalidIsbnException {

        CrossrefType crossrefType = getByType(document.getType());
        var publicationType = Optional.of(crossrefType.getPublicationType())
                .orElseThrow(() -> new IllegalArgumentException(MISSING_CROSSREF_TYPE_IN_DOCUMENT));

        return createContext(document, publicationType);
    }

    @JacocoGenerated
    private PublicationContext createContext(CrossRefDocument document, PublicationType publicationType)
            throws UnsupportedDocumentTypeException, InvalidIssnException, InvalidIsbnException {
        if (publicationType == PublicationType.JOURNAL_CONTENT) {
            return createJournalContext(document);
        } else if (publicationType == PublicationType.BOOK) {
            return createBookContext(document);
        } else if (publicationType == PublicationType.BOOK_CHAPTER) {
            return createChapterContext(document);
        } else {
            throw new UnsupportedDocumentTypeException(String.format(UNRECOGNIZED_TYPE_MESSAGE, document.getType()));
        }
    }

    private Book createBookContext(CrossRefDocument document) throws InvalidIsbnException {
        return new Book.Builder()
                .withLevel(null)
                .withOpenAccess(false)
                .withPeerReviewed(hasReviews(document))
                .withSeriesTitle(extractSeriesTitle(document))
                .withPublisher(extractPublisherName(document))
                .withUrl(extractFulltextLinkAsURL(document))
                .withIsbnList(extractIsbn(document))
                .build();
    }

    private Chapter createChapterContext(CrossRefDocument document) {
        return new Chapter.Builder()
                .withLinkedContext(extractFulltextLinkAsURI(document))
                .build();
    }


    private Journal createJournalContext(CrossRefDocument document) throws InvalidIssnException {
        // TODO actually call the Channel Register API and get the relevant details
        return new Journal.Builder()
                .withLevel(null)
                .withTitle(extractJournalTitle(document))
                .withOnlineIssn(extractOnlineIssn(document))
                .withPrintIssn(extractPrintIssn(document))
                .withOpenAccess(false)
                .withPeerReviewed(false)
                .build();
    }

    private boolean hasReviews(CrossRefDocument document) {
        return nonNull(document.getReview());
    }

    private List<String> extractIsbn(CrossRefDocument document) {
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
        return isNotEmpty(document.getPublisher())
                ? new Organization.Builder().withLabels(Map.of("name", document.getPublisher())).build()
                : null;
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
        } else if (byType == BOOK || byType == BOOK_CHAPTER) {
            return createChapterArticle(document);
        }
        throw new UnsupportedDocumentTypeException(String.format(UNRECOGNIZED_TYPE_MESSAGE, document.getType()));
    }

    private ChapterArticle createChapterArticle(CrossRefDocument document) {
        return new ChapterArticle.Builder()
                .withPages(extractPages(document))
                .withPeerReviewed(hasReviews(document)) // Same as in BasicContext
                .build();
    }

    private JournalArticle createJournalArticle(CrossRefDocument document) {
        return new JournalArticle.Builder()
                .withVolume(document.getVolume())
                .withIssue(document.getIssue())
                .withPages(extractPages(document))
                .build();
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
     * Convert this date given in date-parts to a PublicationDate.
     * For a partial-date data-parts is the only data set, not timestamp or date-time.
     * Only year is mandatory, the rest defaults to 1 it not set.
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

    protected List<Contributor> toContributors(CrossRefDocument document) {

        List<Contributor> contributors = new ArrayList<>();
        contributors.addAll(toContributorsWithoutRole(document.getAuthor()));
        contributors.addAll(toContributorsWithRole(document.getEditor(), Role.EDITOR));
        return contributors;
    }

    protected List<Contributor> toContributorsWithoutRole(List<CrossrefContributor> authors) {
        List<Contributor> contributors = Collections.emptyList();
        if (authors != null) {
            List<Try<Contributor>> contributorMappings =
                    IntStream.range(0, authors.size())
                            .boxed()
                            .map(attempt(index -> toContributor(authors.get(index), index + 1)))
                            .collect(Collectors.toList());

            reportFailures(contributorMappings);
            contributors = successfulMappings(contributorMappings);
        }
        return contributors;
    }

    protected List<Contributor> toContributorsWithRole(List<CrossrefContributor> authors, Role role) {
        List<Contributor> contributors = Collections.emptyList();
        if (authors != null) {
            List<Try<Contributor>> contributorMappings =
                    IntStream.range(0, authors.size())
                            .boxed()
                            .map(attempt(index ->
                                    toContributorWithRole(authors.get(index), role, index + 1)))
                            .collect(Collectors.toList());

            reportFailures(contributorMappings);
            contributors = successfulMappings(contributorMappings);
        }
        return contributors;
    }

    private List<Contributor> successfulMappings(List<Try<Contributor>> contributorMappings) {
        return contributorMappings.stream()
                .filter(Try::isSuccess)
                .map(Try::get)
                .collect(Collectors.toList());
    }

    private void reportFailures(List<Try<Contributor>> contributors) {
        contributors.stream().filter(Try::isFailure)
                .map(Try::getException)
                .forEach(getExceptionConsumer());
    }

    @JacocoGenerated
    private Consumer<Exception> getExceptionConsumer() {
        return e -> logger.error(e.getMessage(), e);
    }

    /**
     * Coverts an author to a Contributor (from external model to internal).
     *
     * @param author              the Author.
     * @param alternativeSequence sequence in case where the Author object does not contain a valid sequence entry
     * @return a Contributor object.
     * @throws MalformedContributorException when the contributer cannot be built.
     */
    private Contributor toContributor(CrossrefContributor author, int alternativeSequence) throws
            MalformedContributorException {
        Identity identity = new Identity.Builder()
                .withName(toName(author.getFamilyName(), author.getGivenName()))
                .withOrcId(author.getOrcid())
                .build();
        final Contributor.Builder contributorBuilder = new Contributor.Builder();
        if (nonNull(author.getAffiliation())) {
            contributorBuilder.withAffiliations(getAffiliations(author.getAffiliation()));
        }
        return contributorBuilder.withIdentity(identity)
                .withSequence(parseSequence(author.getSequence(), alternativeSequence)).build();
    }


    /**
     * Coverts an author to a Contributor with Role (from external model to internal).
     *
     * @param contributor         the Contributor.
     * @param role                Assigned role for the contributor
     * @param alternativeSequence sequence in case where the Author object does not contain a valid sequence entry
     * @return a Contributor object.
     * @throws MalformedContributorException when the contributor cannot be built.
     */
    private Contributor toContributorWithRole(CrossrefContributor contributor, Role role, int alternativeSequence)
            throws MalformedContributorException {
        Identity identity = new Identity.Builder()
                .withName(toName(contributor.getFamilyName(), contributor.getGivenName()))
                .withOrcId(contributor.getOrcid())
                .build();
        final Contributor.Builder contributorBuilder = new Contributor.Builder();
        if (nonNull(contributor.getAffiliation())) {
            contributorBuilder.withAffiliations(getAffiliations(contributor.getAffiliation()));
        }
        return contributorBuilder.withIdentity(identity)
                .withRole(role)
                .withSequence(parseSequence(contributor.getSequence(), alternativeSequence))
                .build();
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

    /**
     * Parses the "sequence" field of the cross-ref document, The "sequence" field shows if the author is the 1st, 2nd,
     * etc. author
     *
     * @param sequence ordinal string e.g. "first"
     * @return Ordinal in number format. "first" -> 1, "second" -> 2, etc.
     */
    private int parseSequence(String sequence, int alternativeSequence) {
        RuleBasedNumberFormat nf = new RuleBasedNumberFormat(Locale.UK, RuleBasedNumberFormat.SPELLOUT);
        try {
            return nf.parse(sequence).intValue();
        } catch (Exception e) {
            return alternativeSequence;
        }
    }

    private List<String> extractSubject(CrossRefDocument document) {
        return Optional.ofNullable(document.getSubject()).orElse(Collections.emptyList());
    }

    private String extractNpiSubjectHeading() {
        return null;
    }

    private FileSet createFilseSet() {
        return null;
    }

    private List<ResearchProject> createProjects() {
        return Collections.emptyList();
    }

    private URL extractFulltextLinkAsURL(CrossRefDocument document) {
        return extractFirstLinkToSourceDocument(document)
                .map(this::transformToUrl)
                .orElse(null);
    }

    private URL transformToUrl(URI uri) {
        return attempt(uri::toURL).orElse(fail -> handleMalformedUrlException());
    }

    private URL handleMalformedUrlException() {
        logger.warn("Malformed URL in CrossRef document");
        return null;
    }

    private URI extractFulltextLinkAsURI(CrossRefDocument document) {
        return  extractFirstLinkToSourceDocument(document).orElse(null);
    }

    /**
     * Extract first valid link to fulltext/source document.
     *
     * @param document CrossrefDocument containing data.
     * @return An optional containing first link in list, without filtering.
     */
    private Optional<URI> extractFirstLinkToSourceDocument(CrossRefDocument document) {
        // TODO Add filter to select what kind of document we want.
        try {
            List<Link> links = document.getLink();
            if (nonNull(links) && !links.isEmpty()) {
                return links.stream().findFirst().map(Link::getUrl).map(URI::create);
            }
        } catch (IllegalArgumentException e) {
            logger.warn("Malformed URL in CrossRef document link");
        }
        return Optional.empty();
    }

}
