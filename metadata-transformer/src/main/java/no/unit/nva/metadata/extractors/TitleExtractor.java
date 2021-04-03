package no.unit.nva.metadata.extractors;

import no.unit.nva.metadata.type.DcTerms;
import no.unit.nva.model.EntityDescription;
import nva.commons.core.JacocoGenerated;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.Value;

import java.util.function.Function;

public final class TitleExtractor {

    public static final Function<ExtractionPair, EntityDescription> apply = TitleExtractor::extract;

    @JacocoGenerated
    private TitleExtractor() {

    }

    private static EntityDescription extract(ExtractionPair extractionPair) {
        Statement statement = extractionPair.getStatement();
        EntityDescription entityDescription = extractionPair.getEntityDescription();
        if (isTitle(statement.getPredicate())) {
            addTitle(entityDescription, statement);
        }
        return entityDescription;
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
