package no.unit.nva.metadata.filters;

import no.unit.nva.metadata.type.DcTerms;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.Value;

/**
 * Filters triples with property dcterms:title from the document model by length comparison, keeping the longest title.
 */
public final class FilterShorterTitles {

    private FilterShorterTitles(){

    }

    public static boolean apply(Model documentModel, Statement statement) {
        if (isTitle(statement)) {
            String currentTitle = statement.getObject().stringValue();
            int maxTitleLength = findMaxTitleLength(documentModel);
            return currentTitle.length() < maxTitleLength;
        }
        return false;
    }

    private static int findMaxTitleLength(Model documentModel) {
        return documentModel.stream()
                   .filter(FilterShorterTitles::isTitle)
                   .map(Statement::getObject)
                   .map(Value::stringValue)
                   .map(String::length)
                   .reduce(Math::max)
                   .orElse(0);
    }

    private static boolean isTitle(Statement statement) {
        return DcTerms.TITLE.getIri().equals(statement.getPredicate())
               && statement.getObject() instanceof Literal;
    }
}
