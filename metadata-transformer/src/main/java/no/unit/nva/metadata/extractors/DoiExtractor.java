package no.unit.nva.metadata.extractors;

import java.net.URI;
import java.util.function.Consumer;

public final class DoiExtractor {
  public static final Consumer<ExtractionPair> APPLY = DoiExtractor::extract;

  private DoiExtractor() {}

  private static void extract(ExtractionPair extractionPair) {
    if (extractionPair.isDoi()) {
      addDoi(extractionPair);
    }
  }

  private static void addDoi(ExtractionPair extractionPair) {
    var entityDescription = extractionPair.getEntityDescription();
    var reference = ExtractorUtil.getReference(entityDescription);
    reference.setDoi(URI.create(extractionPair.getStatementLiteral()));
    entityDescription.setReference(reference);
  }
}
