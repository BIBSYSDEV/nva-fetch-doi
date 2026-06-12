package no.unit.nva.metadata.extractors;

import java.util.function.Consumer;

public final class DescriptionExtractor {

  public static final Consumer<ExtractionPair> APPLY = DescriptionExtractor::extract;

  private DescriptionExtractor() {}

  private static void extract(ExtractionPair extractionPair) {
    if (extractionPair.isDescription()) {
      extractionPair.getEntityDescription().setDescription(extractionPair.getStatementLiteral());
    }
  }
}
