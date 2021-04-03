package no.unit.nva.metadata.extractors;

import no.unit.nva.metadata.type.DcTerms;
import no.unit.nva.model.EntityDescription;
import nva.commons.core.JacocoGenerated;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.Value;

import java.util.function.Function;

public final class DescriptionExtractor {

    public static final Function<ExtractionPair, EntityDescription> apply = DescriptionExtractor::extract;

    @JacocoGenerated
    private DescriptionExtractor() {

    }

    private static EntityDescription extract(ExtractionPair extractionPair) {
        Statement statement = extractionPair.getStatement();
        EntityDescription entityDescription = extractionPair.getEntityDescription();
        if (isDescription(statement.getPredicate(), extractionPair.isNoAbstract())) {
            addDescription(entityDescription, statement);
        }
        return entityDescription;
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
