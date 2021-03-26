package no.unit.nva.metadata;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.ValueFactory;

public enum Bibo implements OntologyProperty {
    DOI("doi");

    private static final String BIBO_PREFIX = "http://purl.org/ontology/bibo/";
    private final String localName;

    Bibo(String localName) {
        this.localName = localName;
    }

    @Override
    public IRI getIri(ValueFactory valueFactory) {
        return valueFactory.createIRI(BIBO_PREFIX, localName);
    }

}
