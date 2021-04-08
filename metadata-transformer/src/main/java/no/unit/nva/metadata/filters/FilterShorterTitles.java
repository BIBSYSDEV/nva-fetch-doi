package no.unit.nva.metadata.filters;

import no.unit.nva.metadata.extractors.ExtractorUtil;
import no.unit.nva.metadata.type.DcTerms;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.Value;

import java.util.Optional;

/**
 * Filters triples with property dcterms:title from the document model by length comparison, keeping the longest title.
 */
public final class FilterShorterTitles {
    public static boolean apply(Model documentModel, Statement statement) {
        return getTitleLength(documentModel).map(length -> selectLongestTitle(statement, length))
                .orElse(false);
    }

    private static Optional<String> getTitleLength(Model documentModel) {
        return documentModel.stream()
                .filter(FilterShorterTitles::isTitle)
                .map(Statement::getObject)
                .map(Value::stringValue)
                .findAny();
    }

    private static boolean selectLongestTitle(Statement statement, String title) {
        return title.length() > statement.getObject().stringValue().length();
    }

    private static boolean isTitle(Statement statement) {
        return DcTerms.TITLE.getIri().equals(statement.getPredicate())
                && !ExtractorUtil.isNotLiteral(statement.getObject());
    }
}
