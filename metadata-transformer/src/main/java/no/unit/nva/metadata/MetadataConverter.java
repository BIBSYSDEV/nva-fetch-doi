package no.unit.nva.metadata;

import java.util.Optional;
import no.unit.nva.metadata.extractors.AbstractExtractor;
import no.unit.nva.metadata.extractors.ContributorExtractor;
import no.unit.nva.metadata.extractors.DateExtractor;
import no.unit.nva.metadata.extractors.DescriptionExtractor;
import no.unit.nva.metadata.extractors.DocumentTypeExtractor;
import no.unit.nva.metadata.extractors.DoiExtractor;
import no.unit.nva.metadata.extractors.LanguageExtractor;
import no.unit.nva.metadata.extractors.MetadataExtractor;
import no.unit.nva.metadata.extractors.TagExtractor;
import no.unit.nva.metadata.extractors.TitleExtractor;
import no.unit.nva.metadata.filters.FilterDuplicateContributors;
import no.unit.nva.metadata.filters.FilterShorterTitles;
import no.unit.nva.metadata.type.DcTerms;
import no.unit.nva.model.EntityDescription;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Statement;

public class MetadataConverter {

    private final Model metadata;
    private final EntityDescription entityDescription;
    private final int emptyDescriptionHash;

    public MetadataConverter(Model metadata) {
        this.metadata = metadata;
        this.entityDescription = new EntityDescription();
        this.emptyDescriptionHash = entityDescription.hashCode();
    }

    @SuppressWarnings("PMD.CloseResource")
    public Optional<CreatePublicationRequest> generateCreatePublicationRequest() {
        if (metadata.isEmpty()) {
            return Optional.empty();
        }
        prepareDataForTransformation();
        MetadataExtractor extractor = configureExtractor();
        for (Statement statement : metadata) {
            extractor.extract(statement);
        }

        return entityDescriptionIsPopulated()
                ? Optional.of(wrapEntityDescriptionWithCreatePublicationRequest())
                : Optional.empty();
    }

    private boolean entityDescriptionIsPopulated() {
        return emptyDescriptionHash != entityDescription.hashCode();
    }

    private CreatePublicationRequest wrapEntityDescriptionWithCreatePublicationRequest() {
        CreatePublicationRequest createPublicationRequest = new CreatePublicationRequest();
        createPublicationRequest.setEntityDescription(entityDescription);
        return createPublicationRequest;
    }

    private void prepareDataForTransformation() {
        metadata.removeIf(statement -> FilterShorterTitles.apply(metadata, statement));
        metadata.removeIf(statement -> FilterDuplicateContributors.apply(metadata, statement));
    }

    private MetadataExtractor configureExtractor() {
        return new MetadataExtractor(entityDescription, hasAbstractPropertyInDocumentModel())
                .withExtractor(AbstractExtractor.apply)
                .withExtractor(ContributorExtractor.apply)
                .withExtractor(DateExtractor.apply)
                .withExtractor(DescriptionExtractor.apply)
                .withExtractor(DocumentTypeExtractor.apply)
                .withExtractor(DoiExtractor.apply)
                .withExtractor(LanguageExtractor.apply)
                .withExtractor(TagExtractor.apply)
                .withExtractor(TitleExtractor.apply);
    }

    private boolean hasAbstractPropertyInDocumentModel() {
        return metadata.contains(null, DcTerms.ABSTRACT.getIri(), null);
    }
}
