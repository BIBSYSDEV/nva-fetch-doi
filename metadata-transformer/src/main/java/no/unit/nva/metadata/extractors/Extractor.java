package no.unit.nva.metadata.extractors;

import no.unit.nva.model.EntityDescription;
import org.eclipse.rdf4j.model.Statement;

public interface Extractor {
    EntityDescription apply(EntityDescription entityDescription, Statement statement);
}
