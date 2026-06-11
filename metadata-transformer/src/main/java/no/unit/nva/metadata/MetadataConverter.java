package no.unit.nva.metadata;

import java.util.Optional;
import no.unit.nva.doi.fetch.commons.publication.model.CreatePublicationRequest;
import no.unit.nva.doi.fetch.commons.publication.model.EntityDescription;
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
        .withExtractor(AbstractExtractor.APPLY)
        .withExtractor(ContributorExtractor.APPLY)
        .withExtractor(DateExtractor.APPLY)
        .withExtractor(DescriptionExtractor.APPLY)
        .withExtractor(DocumentTypeExtractor.APPLY)
        .withExtractor(DoiExtractor.APPLY)
        .withExtractor(LanguageExtractor.APPLY)
        .withExtractor(TagExtractor.APPLY)
        .withExtractor(TitleExtractor.APPLY);
  }

  private boolean hasAbstractPropertyInDocumentModel() {
    return metadata.contains(null, DcTerms.ABSTRACT.getIri(), null);
  }
}
