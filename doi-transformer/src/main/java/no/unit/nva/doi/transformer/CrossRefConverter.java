package no.unit.nva.doi.transformer;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static java.util.function.Predicate.not;
import static no.unit.nva.doi.transformer.model.crossrefmodel.CrossrefDate.DAY_INDEX;
import static no.unit.nva.doi.transformer.model.crossrefmodel.CrossrefDate.FROM_DATE_INDEX_IN_DATE_ARRAY;
import static no.unit.nva.doi.transformer.model.crossrefmodel.CrossrefDate.MONTH_INDEX;
import static no.unit.nva.doi.transformer.model.crossrefmodel.CrossrefDate.YEAR_INDEX;
import static no.unit.nva.doi.transformer.utils.CrossrefType.getByType;
import static nva.commons.core.attempt.Try.attempt;
import java.net.URI;
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
import java.util.stream.Collectors;
import no.unit.nva.doi.fetch.commons.publication.model.Agent;
import no.unit.nva.doi.fetch.commons.publication.model.Contributor;
import no.unit.nva.doi.fetch.commons.publication.model.CreatePublicationRequest;
import no.unit.nva.doi.fetch.commons.publication.model.CreatePublicationRequest.Builder;
import no.unit.nva.doi.fetch.commons.publication.model.EntityDescription;
import no.unit.nva.doi.fetch.commons.publication.model.Identity;
import no.unit.nva.doi.fetch.commons.publication.model.MonographPages;
import no.unit.nva.doi.fetch.commons.publication.model.PublicationContext;
import no.unit.nva.doi.fetch.commons.publication.model.PublicationDate;
import no.unit.nva.doi.fetch.commons.publication.model.PublicationInstance;
import no.unit.nva.doi.fetch.commons.publication.model.Range;
import no.unit.nva.doi.fetch.commons.publication.model.Reference;
import no.unit.nva.doi.fetch.commons.publication.model.Role;
import no.unit.nva.doi.fetch.commons.publication.model.UnconfirmedOrganization;
import no.unit.nva.doi.fetch.commons.publication.model.UnconfirmedPublisher;
import no.unit.nva.doi.fetch.commons.publication.model.contexttypes.Anthology;
import no.unit.nva.doi.fetch.commons.publication.model.contexttypes.Book;
import no.unit.nva.doi.fetch.commons.publication.model.contexttypes.UnconfirmedJournal;
import no.unit.nva.doi.fetch.commons.publication.model.contexttypes.UnconfirmedSeries;
import no.unit.nva.doi.fetch.commons.publication.model.instancetypes.AcademicArticle;
import no.unit.nva.doi.fetch.commons.publication.model.instancetypes.AcademicChapter;
import no.unit.nva.doi.fetch.commons.publication.model.instancetypes.AcademicMonograph;
import no.unit.nva.doi.fetch.commons.publication.model.instancetypes.BookAnthology;
import no.unit.nva.doi.fetch.exceptions.UnsupportedDocumentTypeException;
import no.unit.nva.doi.transformer.language.LanguageMapper;
import no.unit.nva.doi.transformer.language.SimpleLanguageDetector;
import no.unit.nva.doi.transformer.model.crossrefmodel.CrossRefDocument;
import no.unit.nva.doi.transformer.model.crossrefmodel.CrossrefAffiliation;
import no.unit.nva.doi.transformer.model.crossrefmodel.CrossrefContributor;
import no.unit.nva.doi.transformer.model.crossrefmodel.CrossrefDate;
import no.unit.nva.doi.transformer.model.crossrefmodel.Isxn;
import no.unit.nva.doi.transformer.model.crossrefmodel.Isxn.IsxnType;
import no.unit.nva.doi.transformer.utils.CrossrefType;
import no.unit.nva.doi.transformer.utils.IsbnCleaner;
import no.unit.nva.doi.transformer.utils.IssnCleaner;
import no.unit.nva.doi.transformer.utils.PublicationType;
import no.unit.nva.doi.transformer.utils.StringUtils;
import no.unit.nva.doi.transformer.utils.TextLang;
import nva.commons.core.JacocoGenerated;
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

    public CrossRefConverter() {
        this(new DoiConverter());
    }

    public CrossRefConverter(DoiConverter doiConverter) {
        super(new SimpleLanguageDetector(), doiConverter);
    }

    public CreatePublicationRequest toPublication(CrossRefDocument document) {

        if (document != null && hasTitle(document)) {
            return new Builder()
                       //.withCreatedDate(extractInstantFromCrossrefDate(document.getCreated()))
                       //.withModifiedDate(extractInstantFromCrossrefDate(document.getDeposited()))
                       //.withPublishedDate(extractInstantFromCrossrefDate(document.getIssued()))
                       //.withResourceOwner(new ResourceOwner(new Username(owner), UNDEFINED_AFFILIATION))
                       //.withDoi(extractDOI(document)) // Cheating by using URL not DOI ?
                       //.withIdentifier(new SortableIdentifier(identifier.toString()))
                       //.withStatus(DEFAULT_NEW_PUBLICATION_STATUS)
                       //.withIndexedDate(extractInstantFromCrossrefDate(document.getIndexed()))
                       //.withLink(extractFulltextLinkAsUri(document))
                       //.withProjects(createProjects())
                       //.withAssociatedArtifacts(createAssociatedArtifactList())
                       .withEntityDescription(new EntityDescription.Builder()
                                                  .withContributors(toContributors(document))
                                                  .withPublicationDate(extractIssuedDate(document))
                                                  .withMainTitle(extractTitle(document))
                                                  .withAlternativeTitles(extractAlternativeTitles(document))
                                                  .withMainAbstract(extractAbstract(document))
                                                  .withLanguage(extractLanguage(document))
                                                  //.withNpiSubjectHeading(extractNpiSubjectHeading())
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
        contributors.addAll(toContributorsWithRole(document.getAuthor(), new Role("Creator")));
        contributors.addAll(toContributorsWithRole(document.getEditor(), new Role("Editor")));
        return contributors;
    }

    protected List<Contributor> toContributorsWithRole(List<CrossrefContributor> crossrefContributors, Role role) {
        List<CrossrefContributor> contributorsWithName = removeContributorsWithoutNames(crossrefContributors);
        return convertCrossRefContributorsToNvaContributors(contributorsWithName, role);
    }

    private List<Contributor> convertCrossRefContributorsToNvaContributors(
        List<CrossrefContributor> crossrefContributors,
        Role role) {
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
            return new Reference.Builder()
                       .withDoi(doiConverter.toUri(document.getDoi()))
                       .withPublicationContext(extractPublicationContext(document).orElse(null))
                       .withPublicationInstance(extractPublicationInstance(document).orElse(null))
                       .build();
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
                   ? new MonographPages(null)
                   : new MonographPages(pages);
    }

    private Optional<PublicationContext> extractPublicationContext(CrossRefDocument document)
        throws UnsupportedDocumentTypeException {
        return getByType(document.getType())
            .map(CrossrefType::getPublicationType)
            .map(publicationType -> createContext(document, publicationType));
    }

    @JacocoGenerated
    private PublicationContext createContext(CrossRefDocument document, PublicationType publicationType) {
        return switch (publicationType) {
            case JOURNAL_CONTENT -> createJournalContext(document);
            case BOOK, EDITED_BOOK -> createBookContext(document);
            case BOOK_CHAPTER -> createAnthologyContext();
            case null -> null;
        };
    }

    private Book createBookContext(CrossRefDocument document) {
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
        return new Anthology(null);
    }

    private UnconfirmedJournal createJournalContext(CrossRefDocument document) {
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

    private String extractPrintIssn(CrossRefDocument document) {
        return IssnCleaner.clean(filterIssnsByType(document, IsxnType.PRINT));
    }

    private String extractOnlineIssn(CrossRefDocument document) {
        return IssnCleaner.clean(filterIssnsByType(document, IsxnType.ELECTRONIC));
    }

    private String filterIssnsByType(CrossRefDocument crossRefDocument, IsxnType type) {
        List<Isxn> issns = crossRefDocument.getIssnType();
        if (isNull(issns) || issns.isEmpty()) {
            return null;
        }
        return issns.stream().filter(issn -> issn.getType().equals(type))
                   .map(Isxn::getValue)
                   .findAny()
                   .orElse(null);
    }

    private Optional<PublicationInstance> extractPublicationInstance(CrossRefDocument document) {
        return getByType(document.getType())
                   .map(crossrefType -> createInstance(document, crossrefType));

    }

    private PublicationInstance createInstance(CrossRefDocument document, CrossrefType publicationType) {
        return switch (publicationType) {
            case JOURNAL_ARTICLE -> academicArticle(document);
            case BOOK -> createBook(document);
            case BOOK_CHAPTER -> createChapterArticle(document);
            case EDITED_BOOK -> createBookAnthology(document);
            default -> null;
        };
    }

    private PublicationInstance createBook(CrossRefDocument document) {
        if (hasEditor(document)) {
            return createBookAnthology(document);
        } else {
            return createBookMonograph(document);
        }
    }

    private boolean hasEditor(CrossRefDocument document) {
        return nonNull(document.getEditor()) && !document.getEditor().isEmpty();
    }

    private BookAnthology createBookAnthology(CrossRefDocument document) {
        return new BookAnthology(extractMonographPages(document));
    }

    private AcademicMonograph createBookMonograph(CrossRefDocument document) {
        return new AcademicMonograph(extractMonographPages(document));
    }

    private AcademicChapter createChapterArticle(CrossRefDocument document) {
        return new AcademicChapter(extractRangePages(document));
    }

    private AcademicArticle academicArticle(CrossRefDocument document) {
        return new AcademicArticle(extractRangePages(document), document.getVolume(), document.getIssue());
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

        return new PublicationDate(String.valueOf(year), String.valueOf(month), String.valueOf(day));
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
                                              Role role,
                                              int registrationSequence) {
        var identity = new Identity(null, toName(contributor.getGivenName(), contributor.getFamilyName()),
                                    "Personal",
                                    contributor.getOrcid());
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

    private List<Agent> getAffiliations(List<CrossrefAffiliation> crossrefAffiliations) {
        return crossrefAffiliations.stream()
                   .map(CrossrefAffiliation::getName)
                   .map(this::createOrganization)
                   .collect(Collectors.toList());
    }

    private Agent createOrganization(String name) {
        return new UnconfirmedOrganization(name);
    }

    private List<String> extractSubject(CrossRefDocument document) {
        return Optional.ofNullable(document.getSubject()).orElse(Collections.emptyList());
    }
}
