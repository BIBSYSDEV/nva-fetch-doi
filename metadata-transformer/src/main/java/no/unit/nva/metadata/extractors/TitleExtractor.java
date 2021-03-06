package no.unit.nva.metadata.extractors;

import no.unit.nva.model.EntityDescription;

import java.util.function.Function;

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
