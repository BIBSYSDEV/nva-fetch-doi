package no.unit.nva.metadata.filters;

import no.unit.nva.metadata.extractors.ExtractorUtil;
import no.unit.nva.metadata.type.DcTerms;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.Value;

/**
 * Filters triples with property dcterms:title from the document model by length comparison, keeping the longest title.
 */
public final class FilterShorterTitles {
    public static boolean apply(Model documentModel, Statement statement) {
        if (isNotTitle(statement)) {
            return false;
        }

        return documentModel.stream().filter(item -> DcTerms.TITLE.getIri().equals(statement.getPredicate()))
                .map(Statement::getObject)
                .filter(object -> object instanceof Literal)
                .map(Value::stringValue)
                .map(String::length)
                .anyMatch(length -> length > statement.getObject().stringValue().length());
    }

    private static boolean isNotTitle(Statement statement) {
        return ExtractorUtil.isNotLiteral(statement.getObject())
                && !DcTerms.TITLE.getIri().equals(statement.getPredicate());
    }
}
