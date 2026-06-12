package no.unit.nva.metadata.extractors;

import java.util.function.Consumer;

public final class TitleExtractor {

  public static final Consumer<ExtractionPair> APPLY = TitleExtractor::extract;

  private TitleExtractor() {}

  private static void extract(ExtractionPair extractionPair) {
    if (extractionPair.isTitle()) {
      addTitle(extractionPair);
    }
  }

  private static void addTitle(ExtractionPair extractionPair) {
    extractionPair.getEntityDescription().setMainTitle(extractionPair.getStatementLiteral());
  }
}
