package no.unit.nva.metadata.extractors;

import no.unit.nva.model.EntityDescription;
import org.eclipse.rdf4j.model.Statement;

import java.util.HashSet;
import java.util.Set;
import java.util.function.Function;

public class MetadataExtractor {
    private final Set<Function<ExtractionPair, EntityDescription>> extractorList = new HashSet<>();

    public MetadataExtractor register(Function<ExtractionPair, EntityDescription> extractor) {
        extractorList.add(extractor);
        return this;
    }

    public void extract(Statement statement, EntityDescription entityDescription, boolean noAbstract) {
        ExtractionPair extractionPair = new ExtractionPair(statement, entityDescription, noAbstract);

        for (Function<ExtractionPair, EntityDescription> extractor : extractorList) {
            extractor.apply(extractionPair);
        }
    }
}
