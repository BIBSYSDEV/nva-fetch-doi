package no.unit.nva.metadata.extractors;

import java.util.function.Function;
import no.unit.nva.doi.fetch.commons.publication.model.EntityDescription;

public final class TitleExtractor {

    public static final Function<ExtractionPair, EntityDescription> apply = TitleExtractor::extract;

    private TitleExtractor() {

    }

    private static EntityDescription extract(ExtractionPair extractionPair) {
        EntityDescription entityDescription = extractionPair.getEntityDescription();
        if (extractionPair.isTitle()) {
            addTitle(extractionPair);
        }
        return entityDescription;
    }

    private static void addTitle(ExtractionPair extractionPair) {
        extractionPair.getEntityDescription().setMainTitle(extractionPair.getStatementLiteral());
    }
}
