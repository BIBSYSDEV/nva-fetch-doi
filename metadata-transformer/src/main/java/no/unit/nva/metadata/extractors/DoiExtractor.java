package no.unit.nva.metadata.extractors;

import java.net.URI;
import java.util.function.Function;
import no.unit.nva.doi.fetch.commons.publication.model.EntityDescription;

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
        var entityDescription = extractionPair.getEntityDescription();
        var reference = ExtractorUtil.getReference(entityDescription);
        reference.setDoi(URI.create(extractionPair.getStatementLiteral()));
        entityDescription.setReference(reference);
    }
}
