package no.unit.nva.metadata.extractors;

import no.unit.nva.model.EntityDescription;
import org.eclipse.rdf4j.model.Statement;

import java.util.HashSet;
import java.util.Set;
import java.util.function.Function;

public class MetadataExtractor {
    private final boolean abstractPropertyExists;
    private final EntityDescription entityDescription;
    private final Set<Function<ExtractionPair, EntityDescription>> extractorList = new HashSet<>();

    public MetadataExtractor(EntityDescription entityDescription, boolean abstractPropertyExists) {
        this.entityDescription = entityDescription;
        this.abstractPropertyExists = abstractPropertyExists;
    }

    public MetadataExtractor withExtractor(Function<ExtractionPair, EntityDescription> extractor) {
        extractorList.add(extractor);
        return this;
    }

    public void extract(Statement statement) {
        ExtractionPair extractionPair = new ExtractionPair(statement, entityDescription, abstractPropertyExists);

        for (Function<ExtractionPair, EntityDescription> extractor : extractorList) {
            extractor.apply(extractionPair);
        }
    }
}
