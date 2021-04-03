package no.unit.nva.metadata.type;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;

public enum Bibo implements OntologyProperty {
    DOI("doi"),
    ISBN("isbn"),
    ISSN("eissn");

    private static final String BIBO_PREFIX = "http://purl.org/ontology/bibo/";
    private final String localName;

    Bibo(String localName) {
        this.localName = localName;
    }

    @Override
    public IRI getIri() {
        return SimpleValueFactory.getInstance().createIRI(BIBO_PREFIX, localName);
    }

}
