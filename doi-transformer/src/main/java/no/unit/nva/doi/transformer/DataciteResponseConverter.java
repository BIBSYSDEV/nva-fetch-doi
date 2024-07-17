package no.unit.nva.doi.transformer;

import static java.util.Objects.nonNull;
import static java.util.function.Predicate.not;
import static nva.commons.core.attempt.Try.attempt;
import java.net.URI;
import java.util.AbstractMap.SimpleEntry;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import no.sikt.nva.doi.fetch.jsonconfig.Json;
import no.unit.nva.doi.fetch.commons.publication.model.Contributor;
import no.unit.nva.doi.fetch.commons.publication.model.CreatePublicationRequest;
import no.unit.nva.doi.fetch.commons.publication.model.EntityDescription;
import no.unit.nva.doi.fetch.commons.publication.model.Identity;
import no.unit.nva.doi.fetch.commons.publication.model.PublicationContext;
import no.unit.nva.doi.fetch.commons.publication.model.PublicationInstance;
import no.unit.nva.doi.fetch.commons.publication.model.Range;
import no.unit.nva.doi.fetch.commons.publication.model.Reference;
import no.unit.nva.doi.fetch.commons.publication.model.contexttypes.UnconfirmedJournal;
import no.unit.nva.doi.fetch.commons.publication.model.instancetypes.AcademicArticle;
import no.unit.nva.doi.transformer.exception.MalformedContributorException;
import no.unit.nva.doi.transformer.language.SimpleLanguageDetector;
import no.unit.nva.doi.transformer.model.datacitemodel.DataciteContainer;
import no.unit.nva.doi.transformer.model.datacitemodel.DataciteCreator;
import no.unit.nva.doi.transformer.model.datacitemodel.DataciteRelatedIdentifier;
import no.unit.nva.doi.transformer.model.datacitemodel.DataciteResponse;
import no.unit.nva.doi.transformer.model.datacitemodel.DataciteRights;
import no.unit.nva.doi.transformer.model.datacitemodel.DataciteTitle;
import no.unit.nva.doi.transformer.utils.DataciteRelatedIdentifierType;
import no.unit.nva.doi.transformer.utils.DataciteRelationType;
import no.unit.nva.doi.transformer.utils.DataciteTypesUtil;
import no.unit.nva.doi.transformer.utils.InvalidIssnException;
import no.unit.nva.doi.transformer.utils.IssnCleaner;
import no.unit.nva.doi.transformer.utils.LicensingIndicator;
import no.unit.nva.doi.transformer.utils.PublicationType;
import nva.commons.core.StringUtils;
import nva.commons.doi.DoiConverter;

public class DataciteResponseConverter extends AbstractConverter {

    public static final String CREATOR_HAS_NO_NAME_ERROR = "Creator has no name:";

    public DataciteResponseConverter() {
        this(new DoiConverter());
    }

    public DataciteResponseConverter(DoiConverter doiConverter) {
        super(new SimpleLanguageDetector(), doiConverter);
    }

    public CreatePublicationRequest toPublication(DataciteResponse dataciteResponse) throws InvalidIssnException {

/*
    private EntityDescription entityDescription;
    private AssociatedArtifactList associatedArtifacts;
    @JsonProperty("@context")
    private JsonNode context;
    private List<ResearchProject> projects;
    private List<URI> subjects;
    private Set<AdditionalIdentifierBase> additionalIdentifiers;
    private List<Funding> fundings;
    @Size(min = 1, max = 256)
    @Pattern(regexp = "^[\\p{L}\\d][\\p{L}\\d\\s]*\\S$")
    private String rightsHolder;
    private PublicationStatus status;
    private List<ImportDetail> importDetails;

    private List<PublicationNoteBase> publicationNotes;
    private URI duplicateOf;

 */
        return new CreatePublicationRequest.Builder()
                   //.withCreatedDate(now)
                   //.withModifiedDate(now)
                   //.withPublishedDate(extractPublishedDate())
                   //.withResourceOwner(new ResourceOwner(new Username(owner), UNDEFINED_AFFILIATION))
                   //.withPublisher(toPublisher(publisherId))
                   //.withIdentifier(new SortableIdentifier(identifier.toString()))
                   //.withStatus(DEFAULT_NEW_PUBLICATION_STATUS)
                   //.withHandle(extractHandle())
                   //.withLink(extractLink(dataciteResponse))
                   //.withIndexedDate(extractIndexedDate())
                   //.withProjects(extractProjects())
                   .withEntityDescription(
                       new EntityDescription.Builder()
                           .withContributors(toContributors(dataciteResponse.getCreators()))
                           .withPublicationDate(toDate(dataciteResponse.getPublicationYear()))
                           .withMainTitle(extractMainTitle(dataciteResponse))
                           .withMainAbstract(extractAbstract())
                           .withAlternativeTitles(extractAlternativeTitles(dataciteResponse))
                           .withLanguage(createLanguage())
                           .withReference(createReference(dataciteResponse))
                           .withTags(createTags())
                           .withDescription(createDescription())
                           .build())
                   .build();
    }

    private Map<String, String> extractAlternativeTitles(DataciteResponse dataciteResponse) {
        String mainTitle = extractMainTitle(dataciteResponse);
        return dataciteResponse.getTitles().stream()
                   .filter(not(t -> t.getTitle().equals(mainTitle)))
                   .map(t -> detectLanguage(t.getTitle()))
                   .map(e -> new SimpleEntry<>(e.getText(), e.getLanguage().toString()))
                   .collect(Collectors.toMap(SimpleEntry::getKey, SimpleEntry::getValue));
    }

    private String createDescription() {
        return null;
    }

    private List<String> createTags() {
        return Collections.emptyList();
    }

    private Reference createReference(DataciteResponse dataciteResponse) throws InvalidIssnException {
        return new Reference.Builder()
                   .withDoi(doiConverter.toUri(dataciteResponse.getDoi()))
                   .withPublicationContext(extractPublicationContext(dataciteResponse))
                   .withPublicationInstance(extractPublicationInstance(dataciteResponse))
                   .build();
    }

    private PublicationInstance extractPublicationInstance(DataciteResponse dataciteResponse) {
        PublicationInstance publicationInstance = null;
        if (PublicationType.JOURNAL_CONTENT.equals(extractPublicationType(dataciteResponse))) {
            DataciteContainer container = dataciteResponse.getContainer();
            String issue = container.getIssue();
            String volume = container.getVolume();
            publicationInstance = new AcademicArticle("AcademicArticle", extractPages(container), volume, issue);
        }
        return publicationInstance;
    }

    private Range extractPages(DataciteContainer container) {
        return new Range(container.getFirstPage(), container.getLastPage());
    }

    private PublicationContext extractPublicationContext(DataciteResponse dataciteResponse)
        throws InvalidIssnException {
        PublicationContext publicationContext = null;
        PublicationType type = extractPublicationType(dataciteResponse);
        if (nonNull(type) && type.equals(PublicationType.JOURNAL_CONTENT)) {
            publicationContext = new UnconfirmedJournal(
                dataciteResponse.getContainer().getTitle(),
                extractPrintIssn(dataciteResponse),
                extractOnlineIssn(dataciteResponse)
            );
        }
        return publicationContext;
    }

    private String extractOnlineIssn(DataciteResponse dataciteResponse) {
        DataciteRelatedIdentifier result = extractIsPartOfRelations(dataciteResponse)
                                               .stream()
                                               .filter(this::isOnlineIssn)
                                               .findAny()
                                               .orElse(null);
        return nonNull(result) ? IssnCleaner.clean(result.getRelatedIdentifier()) : null;
    }

    private boolean isOnlineIssn(DataciteRelatedIdentifier identifier) {
        return DataciteRelatedIdentifierType.getByCode(identifier.getRelatedIdentifierType())
                   .equals(DataciteRelatedIdentifierType.EISSN);
    }

    private String extractPrintIssn(DataciteResponse dataciteResponse) {
        DataciteRelatedIdentifier result = extractIsPartOfRelations(dataciteResponse)
                                               .stream()
                                               .filter(this::isPrintIssn)
                                               .findAny()
                                               .orElse(null);
        return nonNull(result) ? IssnCleaner.clean(result.getRelatedIdentifier()) : null;
    }

    private List<DataciteRelatedIdentifier> extractIsPartOfRelations(DataciteResponse dataciteResponse) {
        return dataciteResponse.getRelatedIdentifiers()
                   .stream()
                   .filter(this::isPartOf)
                   .collect(Collectors.toList());
    }

    private boolean isPrintIssn(DataciteRelatedIdentifier identifier) {
        return DataciteRelatedIdentifierType.getByCode(identifier.getRelatedIdentifierType())
                   .equals(DataciteRelatedIdentifierType.ISSN);
    }

    private boolean isPartOf(DataciteRelatedIdentifier identifier) {
        return DataciteRelationType.getByRelation(identifier.getRelationType())
                   .equals(DataciteRelationType.IS_PART_OF);
    }

    protected boolean hasOpenAccessRights(DataciteRights dataciteRights) {
        return Optional.ofNullable(dataciteRights.getRightsUri())
                   .map(LicensingIndicator::isOpen).orElse(false);
    }

    private PublicationType extractPublicationType(DataciteResponse dataciteResponse) {
        return DataciteTypesUtil.mapToType(dataciteResponse);
    }

    private String extractAbstract() {
        return null;
    }

    private URI createLanguage() {
        return null;
    }

    protected String extractMainTitle(DataciteResponse response) {
        Stream<String> titleStrings = response.getTitles().stream().map(DataciteTitle::getTitle);
        return getMainTitle(titleStrings);
    }

    protected List<Contributor> toContributors(List<DataciteCreator> creators) {
        return IntStream.range(0, creators.size())
                   .boxed()
                   .map(i -> toCreator(creators.get(i), i + 1))
                   .flatMap(Optional::stream)
                   .collect(Collectors.toList());
    }

    protected Optional<Contributor> toCreator(DataciteCreator dataciteCreator, Integer sequence) {
        try {
            Contributor nvaContributor = new Contributor.Builder()
                                             .withIdentity(createCreatorIdentity(dataciteCreator))
                                             .withAffiliations(Collections.emptyList())
                                             .withSequence(sequence)
                                             .build();
            return Optional.of(nvaContributor);
        } catch (MalformedContributorException e) {
            return Optional.empty();
        }
    }

    private Identity createCreatorIdentity(DataciteCreator dataciteCreator) throws MalformedContributorException {
        if (creatorHasNoName(dataciteCreator)) {
            String jsonString = attempt(() -> Json.writeValueAsString(dataciteCreator))
                                    .orElseThrow();
            throw new MalformedContributorException(CREATOR_HAS_NO_NAME_ERROR + jsonString);
        }

        return new Identity(null,
                            toName(dataciteCreator.getGivenName(), dataciteCreator.getFamilyName()),
                            "Personal",
                            null);
    }

    private boolean creatorHasNoName(DataciteCreator dataciteCreator) {
        return StringUtils.isBlank(dataciteCreator.getFamilyName())
               && StringUtils.isBlank(dataciteCreator.getGivenName());
    }
}
