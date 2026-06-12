package no.unit.nva.metadata.extractors;

import java.util.HashSet;
import java.util.Set;
import java.util.function.Consumer;
import no.unit.nva.doi.fetch.commons.publication.model.EntityDescription;
import org.eclipse.rdf4j.model.Statement;

public class MetadataExtractor {
  private final boolean abstractPropertyExists;
  private final EntityDescription entityDescription;
  private final Set<Consumer<ExtractionPair>> extractorList = new HashSet<>();

  public MetadataExtractor(EntityDescription entityDescription, boolean abstractPropertyExists) {
    this.entityDescription = entityDescription;
    this.abstractPropertyExists = abstractPropertyExists;
  }

  public MetadataExtractor withExtractor(Consumer<ExtractionPair> extractor) {
    extractorList.add(extractor);
    return this;
  }

  public void extract(Statement statement) {
    ExtractionPair extractionPair =
        new ExtractionPair(statement, entityDescription, abstractPropertyExists);

    for (Consumer<ExtractionPair> extractor : extractorList) {
      extractor.accept(extractionPair);
    }
  }
}
