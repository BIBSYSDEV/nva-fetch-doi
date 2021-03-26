package no.unit.nva.metadata;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.ValueFactory;

public interface OntologyProperty {
    IRI getIri(ValueFactory valueFactory);
}
