package no.unit.nva.metadata.extractors;

import no.unit.nva.metadata.type.DcTerms;
import no.unit.nva.model.EntityDescription;
import nva.commons.core.JacocoGenerated;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.Value;

public final class DescriptionExtractor {

    @JacocoGenerated
    private DescriptionExtractor() {

    }

    public static void extract(EntityDescription entityDescription, Statement statement, boolean noAbstract) {
        if (isDescription(statement.getPredicate(), noAbstract)) {
            addDescription(entityDescription, statement);
        }
    }

    private static void addDescription(EntityDescription entityDescription, Statement statement) {
        Value object = statement.getObject();
        if (ExtractorUtil.isNotLiteral(object)) {
            return;
        }
        entityDescription.setDescription(object.stringValue());
    }

    private static boolean isDescription(IRI target, boolean noAbstract) {
        return DcTerms.DESCRIPTION.getIri().equals(target) && !noAbstract;
    }
}
