package no.unit.nva.metadata.filters;

import no.unit.nva.metadata.type.DcTerms;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Statement;

public final class FilterDuplicateContributors {

    private FilterDuplicateContributors() {

    }

    public static boolean apply(Model model, Statement statement) {
        return DcTerms.CONTRIBUTOR.getIri().equals(statement.getPredicate())
               && model.contains(statement.getSubject(),
                                 DcTerms.CREATOR.getIri(), statement.getObject());
    }
}
