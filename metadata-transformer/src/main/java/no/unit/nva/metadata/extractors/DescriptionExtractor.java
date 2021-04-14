package no.unit.nva.metadata.extractors;

import no.unit.nva.model.EntityDescription;
import nva.commons.core.JacocoGenerated;

import java.util.function.Function;

public final class DescriptionExtractor {

    public static final Function<ExtractionPair, EntityDescription> apply = DescriptionExtractor::extract;

    @JacocoGenerated
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
