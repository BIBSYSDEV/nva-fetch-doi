package no.unit.nva.metadata.extractors;

import no.unit.nva.model.EntityDescription;
import no.unit.nva.model.Reference;

import java.net.URI;
import java.util.function.Function;

public final class DoiExtractor {
    public static final Function<ExtractionPair, EntityDescription> apply = DoiExtractor::extract;

    private DoiExtractor() {

    }

    private static EntityDescription extract(ExtractionPair extractionPair) {
        if (extractionPair.isDoi()) {
            addDoi(extractionPair);
        }
        return extractionPair.getEntityDescription();
    }

    private static void addDoi(ExtractionPair extractionPair) {
        EntityDescription entityDescription = extractionPair.getEntityDescription();
        Reference reference = ExtractorUtil.getReference(entityDescription);
        reference.setDoi(URI.create(extractionPair.getStatementLiteral()));
        entityDescription.setReference(reference);
    }
}
