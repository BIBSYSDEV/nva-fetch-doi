package no.unit.nva.metadata.extractors;

import no.unit.nva.model.EntityDescription;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import static java.util.Objects.nonNull;

public final class TagExtractor {
    public static final Function<ExtractionPair, EntityDescription> apply = TagExtractor::extract;

    private TagExtractor() {

    }

    private static EntityDescription extract(ExtractionPair extractionPair) {
        if (extractionPair.isTag()) {
            addTag(extractionPair);
        }
        return extractionPair.getEntityDescription();
    }

    private static void addTag(ExtractionPair extractionPair) {
        List<String> tags = new ArrayList<>();
        EntityDescription entityDescription = extractionPair.getEntityDescription();
        List<String> existingTags = entityDescription.getTags();
        if (nonNull(existingTags) && !existingTags.isEmpty()) {
            tags.addAll(existingTags);
        }
        tags.add(extractionPair.getStatementLiteral());
        entityDescription.setTags(List.of(tags.toArray(new String[]{})));
    }
}
