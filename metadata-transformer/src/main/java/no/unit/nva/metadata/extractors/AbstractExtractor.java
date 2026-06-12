package no.unit.nva.metadata.extractors;

import java.util.function.Consumer;

/** Extractor for journal abstracts. */
public final class AbstractExtractor {

  public static final Consumer<ExtractionPair> APPLY = AbstractExtractor::extract;

  private AbstractExtractor() {}

  private static void extract(ExtractionPair extractionPair) {
    if (extractionPair.isAbstract()) {
      addAbstract(extractionPair);
    }
  }

  private static void addAbstract(ExtractionPair extractionPair) {
    extractionPair.getEntityDescription().setMainAbstract(extractionPair.getStatementLiteral());
  }
}
