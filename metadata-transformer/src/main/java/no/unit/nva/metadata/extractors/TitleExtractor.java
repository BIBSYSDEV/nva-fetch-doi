package no.unit.nva.metadata.extractors;

import no.unit.nva.metadata.type.DcTerms;
import no.unit.nva.model.EntityDescription;
import nva.commons.core.JacocoGenerated;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.Value;

public final class TitleExtractor {

    @JacocoGenerated
    private TitleExtractor() {

    }

    public static void extract(EntityDescription entityDescription, Statement statement) {
        if (isTitle(statement.getPredicate())) {
            addTitle(entityDescription, statement);
        }
    }

    private static void addTitle(EntityDescription entityDescription, Statement statement) {
        Value object = statement.getObject();
        if (ExtractorUtil.isNotLiteral(object)) {
            return;
        }
        entityDescription.setMainTitle(object.stringValue());
    }

    private static boolean isTitle(IRI target) {
        return DcTerms.TITLE.getIri().equals(target);
    }
}
