package no.unit.nva.metadata.type;

import org.eclipse.rdf4j.model.IRI;

@FunctionalInterface
public interface OntologyProperty {
    IRI getIri();
}
