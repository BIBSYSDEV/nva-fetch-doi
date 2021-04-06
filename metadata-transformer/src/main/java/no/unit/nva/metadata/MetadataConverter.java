package no.unit.nva.metadata;

import no.unit.nva.api.CreatePublicationRequest;
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

import java.util.Optional;

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
    public Optional<CreatePublicationRequest> extractPublicationRequest() {
        if (metadata.isEmpty()) {
            return Optional.empty();
        }
        prepareDataForTransformation();
        MetadataExtractor extractor = configureExtractor();
        for (Statement statement : metadata) {
            extractor.extract(statement, entityDescription, noAbstractIsPresent());
        }

        return entityDescriptionIsPopulated()
                ? Optional.of(getCreatePublicationRequest())
                : Optional.empty();
    }

    private boolean entityDescriptionIsPopulated() {
        return emptyDescriptionHash != entityDescription.hashCode();
    }

    private CreatePublicationRequest getCreatePublicationRequest() {
        CreatePublicationRequest createPublicationRequest = new CreatePublicationRequest();
        createPublicationRequest.setEntityDescription(entityDescription);
        return createPublicationRequest;
    }

    private void prepareDataForTransformation() {
        metadata.removeIf(statement -> FilterShorterTitles.apply(metadata, statement));
        metadata.removeIf(statement -> FilterDuplicateContributors.apply(metadata, statement));
    }

    private MetadataExtractor configureExtractor() {
        return new MetadataExtractor()
                .register(AbstractExtractor.apply)
                .register(ContributorExtractor.apply)
                .register(DateExtractor.apply)
                .register(DescriptionExtractor.apply)
                .register(DocumentTypeExtractor.apply)
                .register(DoiExtractor.apply)
                .register(LanguageExtractor.apply)
                .register(TagExtractor.apply)
                .register(TitleExtractor.apply);
    }

    private boolean noAbstractIsPresent() {
        return !metadata.contains(null, DcTerms.ABSTRACT.getIri(), null);
    }
}
