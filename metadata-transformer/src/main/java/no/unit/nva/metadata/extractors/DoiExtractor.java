package no.unit.nva.metadata.extractors;

import no.unit.nva.metadata.type.Bibo;
import no.unit.nva.model.EntityDescription;
import no.unit.nva.model.Reference;
import nva.commons.core.JacocoGenerated;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Statement;

import java.net.URI;
import java.util.function.Function;

public final class DoiExtractor {

    public static final Function<ExtractionPair, EntityDescription> apply = DoiExtractor::extract;

    @JacocoGenerated
    private DoiExtractor() {

    }

    private static EntityDescription extract(ExtractionPair extractionPair) {
        Statement statement = extractionPair.getStatement();
        EntityDescription entityDescription = extractionPair.getEntityDescription();
        if (isDoi(statement.getPredicate())) {
            addDoi(entityDescription, statement);
        }
        return entityDescription;
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
