package no.unit.nva.metadata.extractors;

import no.unit.nva.metadata.type.DcTerms;
import no.unit.nva.model.EntityDescription;
import nva.commons.core.JacocoGenerated;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.Value;

public final class AbstractExtractor {

    @JacocoGenerated
    private AbstractExtractor() {

    }

    public static void extract(EntityDescription entityDescription, Statement statement, boolean noAbstract) {
        if (isAbstract(statement.getPredicate(), noAbstract)) {
            addAbstract(entityDescription, statement);
        }
    }

    private static void addAbstract(EntityDescription entityDescription, Statement statement) {
        Value object = statement.getObject();
        if (ExtractorUtil.isNotLiteral(object)) {
            return;
        }
        entityDescription.setAbstract(object.stringValue());
    }

    private static boolean isAbstract(IRI candidate, boolean noAbstract) {
        return DcTerms.ABSTRACT.getIri().equals(candidate)
                || DcTerms.DESCRIPTION.getIri().equals(candidate) && noAbstract;
    }
}
