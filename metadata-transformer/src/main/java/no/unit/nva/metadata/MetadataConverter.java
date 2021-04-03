package no.unit.nva.metadata;

import no.unit.nva.api.CreatePublicationRequest;
import no.unit.nva.metadata.extractors.AbstractExtractor;
import no.unit.nva.metadata.extractors.ContributorExtractor;
import no.unit.nva.metadata.extractors.DateExtractor;
import no.unit.nva.metadata.extractors.DescriptionExtractor;
import no.unit.nva.metadata.extractors.DocumentTypeExtractor;
import no.unit.nva.metadata.extractors.DoiExtractor;
import no.unit.nva.metadata.extractors.LanguageExtractor;
import no.unit.nva.metadata.extractors.TagExtractor;
import no.unit.nva.metadata.extractors.TitleExtractor;
import no.unit.nva.metadata.filters.FilterDuplicateContributors;
import no.unit.nva.metadata.filters.FilterShorterTitles;
import no.unit.nva.metadata.type.DcTerms;
import no.unit.nva.model.EntityDescription;
import no.unit.nva.model.exceptions.InvalidIsbnException;
import no.unit.nva.model.exceptions.InvalidIssnException;
import no.unit.nva.model.exceptions.MalformedContributorException;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Statement;

import java.util.Optional;

public class MetadataConverter {

    private final Model metadata;

    public MetadataConverter(Model metadata) {
        this.metadata = metadata;
    }

    public Optional<CreatePublicationRequest> extractPublicationRequest() throws MalformedContributorException,
            InvalidIssnException, InvalidIsbnException {
        if (metadata.isEmpty()) {
            return Optional.empty();
        }
        prepareDataForTransformation();
        EntityDescription entityDescription = new EntityDescription();
        int hash = entityDescription.hashCode();
        for (Statement statement : metadata) {
            updateCreatePublicationRequest(entityDescription, statement);
        }

        return hash != entityDescription.hashCode()
                ? Optional.of(getCreatePublicationRequest(entityDescription))
                : Optional.empty();
    }

    private CreatePublicationRequest getCreatePublicationRequest(EntityDescription entityDescription) {
        CreatePublicationRequest createPublicationRequest = new CreatePublicationRequest();
        createPublicationRequest.setEntityDescription(entityDescription);
        return createPublicationRequest;
    }

    private void prepareDataForTransformation() {
        metadata.removeIf(statement -> FilterShorterTitles.apply(metadata, statement));
        metadata.removeIf(statement -> FilterDuplicateContributors.apply(metadata, statement));
    }

    private void updateCreatePublicationRequest(EntityDescription entityDescription, Statement statement)
            throws MalformedContributorException, InvalidIssnException, InvalidIsbnException {
        ContributorExtractor.extract(entityDescription, statement);
        DateExtractor.extract(entityDescription, statement);
        TitleExtractor.extract(entityDescription, statement);
        DoiExtractor.extract(entityDescription, statement);
        LanguageExtractor.extract(entityDescription, statement);
        boolean noAbstract = noAbstractIsPresent();
        AbstractExtractor.extract(entityDescription, statement, noAbstract);
        DescriptionExtractor.extract(entityDescription, statement, noAbstract);
        TagExtractor.extract(entityDescription, statement);
        DocumentTypeExtractor.extract(entityDescription, statement);
    }

    private boolean noAbstractIsPresent() {
        return !metadata.contains(null, DcTerms.ABSTRACT.getIri(), null);
    }
}
