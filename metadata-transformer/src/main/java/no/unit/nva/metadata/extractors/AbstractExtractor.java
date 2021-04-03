package no.unit.nva.metadata.extractors;

import no.unit.nva.metadata.type.DcTerms;
import no.unit.nva.model.EntityDescription;
import nva.commons.core.JacocoGenerated;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.Value;

import java.util.function.Function;

public final class AbstractExtractor {

    public static final Function<ExtractionPair, EntityDescription> apply = AbstractExtractor::extract;

    @JacocoGenerated
    private AbstractExtractor() {

    }

    private static EntityDescription extract(ExtractionPair extractionPair) {
        Statement statement = extractionPair.getStatement();
        EntityDescription entityDescription = extractionPair.getEntityDescription();
        if (isAbstract(statement.getPredicate(), extractionPair.isNoAbstract())) {
            return addAbstract(entityDescription, statement);
        }
        return entityDescription;
    }

    private static EntityDescription addAbstract(EntityDescription entityDescription, Statement statement) {
        Value object = statement.getObject();
        if (!ExtractorUtil.isNotLiteral(object)) {
            entityDescription.setAbstract(object.stringValue());
        }
        return entityDescription;
    }

    private static boolean isAbstract(IRI candidate, boolean noAbstract) {
        return DcTerms.ABSTRACT.getIri().equals(candidate)
                || DcTerms.DESCRIPTION.getIri().equals(candidate) && noAbstract;
    }
}
