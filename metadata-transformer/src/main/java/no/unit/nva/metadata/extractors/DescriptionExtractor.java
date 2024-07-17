package no.unit.nva.metadata.extractors;

import java.util.function.Function;
import no.unit.nva.doi.fetch.commons.publication.model.EntityDescription;

public final class DescriptionExtractor {

    public static final Function<ExtractionPair, EntityDescription> apply = DescriptionExtractor::extract;

    private DescriptionExtractor() {

    }

    private static EntityDescription extract(ExtractionPair extractionPair) {
        EntityDescription entityDescription = extractionPair.getEntityDescription();
        if (extractionPair.isDescription()) {
            entityDescription.setDescription(extractionPair.getStatementLiteral());
        }
        return entityDescription;
    }
}
