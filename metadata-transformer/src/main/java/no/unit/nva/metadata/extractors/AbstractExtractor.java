package no.unit.nva.metadata.extractors;

import java.util.function.Function;
import no.unit.nva.doi.fetch.commons.publication.model.EntityDescription;

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
        entityDescription.setMainAbstract(extractionPair.getStatementLiteral());
        return entityDescription;
    }
}
