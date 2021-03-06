package no.unit.nva.metadata.extractors;

import no.unit.nva.model.EntityDescription;

import java.util.function.Function;

/**
 * Extractor for journal abstracts.
 */
public final class AbstractExtractor {

    public static final Function<ExtractionPair, EntityDescription> apply = AbstractExtractor::extract;

    private AbstractExtractor() {

    }

    private static EntityDescription extract(ExtractionPair extractionPair) {
        if (extractionPair.isAbstract()) {
            return addAbstract(extractionPair);
        }
        return extractionPair.getEntityDescription();
    }

    private static EntityDescription addAbstract(ExtractionPair extractionPair) {
        EntityDescription entityDescription = extractionPair.getEntityDescription();
        entityDescription.setAbstract(extractionPair.getStatementLiteral());
        return entityDescription;
    }
}
