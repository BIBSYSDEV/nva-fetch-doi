package no.unit.nva.doi.transformer;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static java.util.function.Predicate.not;
import static nva.commons.utils.attempt.Try.attempt;

import com.ibm.icu.text.RuleBasedNumberFormat;
import java.net.URI;
import java.time.Instant;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import no.unit.nva.doi.transformer.language.LanguageMapper;
import no.unit.nva.doi.transformer.language.SimpleLanguageDetector;
import no.unit.nva.doi.transformer.model.crossrefmodel.CrossRefDocument;
import no.unit.nva.doi.transformer.model.crossrefmodel.CrossrefAuthor;
import no.unit.nva.doi.transformer.model.crossrefmodel.CrossrefDate;
import no.unit.nva.doi.transformer.model.crossrefmodel.Issn;
import no.unit.nva.doi.transformer.utils.CrossrefType;
import no.unit.nva.doi.transformer.utils.IssnCleaner;
import no.unit.nva.doi.transformer.utils.StringUtils;
import no.unit.nva.doi.transformer.utils.TextLang;
import no.unit.nva.model.Contributor;
import no.unit.nva.model.EntityDescription;
import no.unit.nva.model.FileSet;
import no.unit.nva.model.Identity;
import no.unit.nva.model.Publication;
import no.unit.nva.model.PublicationDate;
import no.unit.nva.model.PublicationType;
import no.unit.nva.model.Reference;
import no.unit.nva.model.ResearchProject;
import no.unit.nva.model.contexttypes.Journal;
import no.unit.nva.model.contexttypes.PublicationContext;
import no.unit.nva.model.exceptions.InvalidIssnException;
import no.unit.nva.model.exceptions.InvalidPageTypeException;
import no.unit.nva.model.exceptions.MalformedContributorException;
import no.unit.nva.model.instancetypes.JournalArticle;
import no.unit.nva.model.instancetypes.PublicationInstance;
import no.unit.nva.model.pages.Range;
import nva.commons.utils.attempt.Try;
import nva.commons.utils.doi.DoiConverterImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CrossRefConverter extends AbstractConverter {

    public static final String INVALID_ENTRY_ERROR = "The entry is empty or has no title";
    public static final URI CROSSEF_URI = URI.create("https://www.crossref.org/");
    public static final String CROSSREF = "crossref";
    public static final String UNRECOGNIZED_TYPE_MESSAGE = "The publication type \"%s\" was not recognized";
    private static final Logger logger = LoggerFactory.getLogger(CrossRefConverter.class);

    public CrossRefConverter() {
        super(new SimpleLanguageDetector(), new DoiConverterImpl());
    }

    /**
     * Creates a publication.
     *
     * @param document    a Java representation of a CrossRef document.
     * @param now         Instant.
     * @param owner       the owning institution.
     * @param identifier  the publication identifier.
     * @param publisherId the id for a publisher.
     * @return an internal representation of the publication.
     * @throws InvalidIssnException     thrown if a provided ISSN is invalid.
     * @throws InvalidPageTypeException thrown if the provided page type is incompatible with the publication instance
     *                                  type.
     */
    public Publication toPublication(CrossRefDocument document,
                                     Instant now,
                                     String owner,
                                     UUID identifier,
                                     URI publisherId) throws InvalidIssnException, InvalidPageTypeException {

        if (document != null && hasTitle(document)) {
            return new Publication.Builder()
                .withCreatedDate(now)
                .withModifiedDate(now)
                .withPublishedDate(createPublishedDate())
                .withOwner(owner)
                .withIdentifier(identifier)
                .withPublisher(toPublisher(publisherId))
                .withStatus(DEFAULT_NEW_PUBLICATION_STATUS)
                .withIndexedDate(createIndexedDate())
                .withHandle(createHandle())
                .withLink(createLink())
                .withProject(createProject())
                .withFileSet(createFilseSet())
                .withEntityDescription(new EntityDescription.Builder()
                    .withContributors(toContributors(document.getAuthor()))
                    .withDate(extractDate(document).orElse(null))
                    .withMainTitle(extractTitle(document))
                    .withAlternativeTitles(extractAlternativeTitles(document))
                    .withAbstract(extractAbstract(document))
                    .withLanguage(extractLanguage(document))
                    .withNpiSubjectHeading(extractNpiSubjectHeading())
                    .withTags(extractTags())
                    .withDescription(extractDescription())
                    .withReference(extractReference(document))
                    .withMetadataSource(extractMetadataSource(document))
                    .build())
                .build();
        }
        throw new IllegalArgumentException(INVALID_ENTRY_ERROR);
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

    private Reference extractReference(CrossRefDocument document) throws InvalidIssnException,
                                                                         InvalidPageTypeException {
        PublicationInstance instance = extractPublicationInstance(document);
        PublicationContext context = extractPublicationContext(document);
        return new Reference.Builder()
            .withDoi(doiConverter.toUri(document.getDoi()))
            .withPublishingContext(context)
            .withPublicationInstance(instance)
            .build();
    }

    private Range extractPages(CrossRefDocument document) {
        return StringUtils.parsePage(document.getPage());
    }

    private PublicationContext extractPublicationContext(CrossRefDocument document) throws InvalidIssnException {
        CrossrefType crossrefType = CrossrefType.getByType(document.getType());
        PublicationType publicationType = crossrefType.getPublicationType();

        if (nonNull(publicationType) && publicationType.equals(PublicationType.JOURNAL_CONTENT)) {
            // TODO actually call the Channel Register API and get the relevant details
            return new Journal.Builder()
                .withLevel(null)
                .withTitle(extractJournalTitle(document))
                .withOnlineIssn(extractOnlineIssn(document))
                .withPrintIssn(extractPrintIssn(document))
                .withOpenAccess(false)
                .withPeerReviewed(false)
                .build();
        } else {
            throw new IllegalArgumentException(String.format(UNRECOGNIZED_TYPE_MESSAGE, document.getType()));
        }
    }

    private String extractPrintIssn(CrossRefDocument document) {
        return IssnCleaner.clean(filterIssnsByType(document, Issn.IssnType.PRINT));
    }

    private String extractOnlineIssn(CrossRefDocument document) {
        return IssnCleaner.clean(filterIssnsByType(document, Issn.IssnType.ELECTRONIC));
    }

    private String filterIssnsByType(CrossRefDocument crossRefDocument, Issn.IssnType type) {
        List<Issn> issns = crossRefDocument.getIssnType();
        if (isNull(issns) || issns.isEmpty()) {
            return null;
        }

        return issns.stream().filter(issn -> issn.getType().equals(type))
            .map(Issn::getValue)
            .findAny()
            .orElse(null);
    }

    private PublicationInstance extractPublicationInstance(CrossRefDocument document) throws InvalidPageTypeException {
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

    private String extractDescription() {
        return null;
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
    private Optional<PublicationDate> extractDate(CrossRefDocument document) {
        Optional<Integer> earliestYear = Optional.ofNullable(document.getPublishedPrint())
            .flatMap(CrossrefDate::extractEarliestYear);

        return earliestYear.map(this::toDate);
    }

    protected List<Contributor> toContributors(List<CrossrefAuthor> authors) {
        if (authors != null) {
            List<Try<Contributor>> contributorMappings =
                IntStream.range(0, authors.size())
                    .boxed()
                    .map(attempt(index -> toContributor(authors.get(index), index + 1)))
                    .collect(Collectors.toList());

            reportFailures(contributorMappings);
            return successfulMappings(contributorMappings);
        } else {
            return Collections.emptyList();
        }
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
            .forEach(e -> logger.error(e.getMessage(), e));
    }

    /**
     * Coverts an author to a Contributor (from external model to internal).
     *
     * @param author              the Author.
     * @param alternativeSequence sequence in case where the Author object does not contain a valid sequence entry
     * @return a Contributor object.
     * @throws MalformedContributorException when the contributer cannot be built.
     */
    private Contributor toContributor(CrossrefAuthor author, int alternativeSequence) throws
                                                                                      MalformedContributorException {
        Identity identity =
            new Identity.Builder().withName(toName(author.getFamilyName(), author.getGivenName())).build();
        return new Contributor.Builder().withIdentity(identity)
            .withSequence(parseSequence(author.getSequence(), alternativeSequence)).build();
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

    private List<String> extractTags() {
        return null;
    }

    private String extractNpiSubjectHeading() {
        return null;
    }

    private FileSet createFilseSet() {
        return null;
    }

    private ResearchProject createProject() {
        return null;
    }

    private URI createLink() {
        return null;
    }

    private URI createHandle() {
        return null;
    }

    private Instant createIndexedDate() {
        return null;
    }

    private Instant createPublishedDate() {
        return null;
    }
}
