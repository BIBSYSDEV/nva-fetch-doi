package no.unit.nva.metadata.extractors;

import static java.util.Objects.nonNull;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import no.unit.nva.doi.fetch.commons.publication.model.EntityDescription;

public final class TagExtractor {
  public static final Consumer<ExtractionPair> APPLY = TagExtractor::extract;

  private TagExtractor() {}

  private static void extract(ExtractionPair extractionPair) {
    if (extractionPair.isTag()) {
      addTag(extractionPair);
    }
  }

  private static void addTag(ExtractionPair extractionPair) {
    List<String> tags = new ArrayList<>();
    EntityDescription entityDescription = extractionPair.getEntityDescription();
    List<String> existingTags = entityDescription.getTags();
    if (nonNull(existingTags) && !existingTags.isEmpty()) {
      tags.addAll(existingTags);
    }
    tags.add(extractionPair.getStatementLiteral());
    entityDescription.setTags(List.of(tags.toArray(new String[] {})));
  }
}
