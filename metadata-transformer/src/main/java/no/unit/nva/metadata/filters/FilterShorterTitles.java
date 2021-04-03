package no.unit.nva.metadata.filters;

import no.unit.nva.metadata.extractors.ExtractorUtil;
import no.unit.nva.metadata.type.DcTerms;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.Value;

public class FilterShorterTitles {
    public static boolean apply(Model model, Statement statement) {
        if (ExtractorUtil.isNotLiteral(statement.getObject())) {
            return false;
        }

        return model.stream().filter(item -> statement.getPredicate().equals(DcTerms.TITLE.getIri()))
                .map(Statement::getObject)
                .filter(object -> object instanceof Literal)
                .map(Value::stringValue)
                .map(String::length)
                .anyMatch(length -> length > statement.getObject().stringValue().length());
    }
}
