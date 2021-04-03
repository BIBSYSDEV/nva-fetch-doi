package no.unit.nva.metadata.extractors;

import no.unit.nva.metadata.type.Bibo;
import no.unit.nva.model.EntityDescription;
import no.unit.nva.model.Reference;
import nva.commons.core.JacocoGenerated;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Statement;

import java.net.URI;

public final class DoiExtractor {

    @JacocoGenerated
    private DoiExtractor() {

    }

    public static void extract(EntityDescription entityDescription, Statement statement) {
        if (isDoi(statement.getPredicate())) {
            addDoi(entityDescription, statement);
        }
    }

    private static boolean isDoi(IRI candidate) {
        return Bibo.DOI.getIri().equals(candidate);
    }


    private static void addDoi(EntityDescription entityDescription, Statement statement) {
        if (!(statement.getObject() instanceof IRI)) {
            return;
        }
        Reference reference = ExtractorUtil.getReference(entityDescription);
        reference.setDoi(URI.create(statement.getObject().stringValue()));
        entityDescription.setReference(reference);
    }
}
