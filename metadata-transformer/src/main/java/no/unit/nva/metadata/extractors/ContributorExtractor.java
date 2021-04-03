package no.unit.nva.metadata.extractors;

import no.unit.nva.metadata.type.DcTerms;
import no.unit.nva.model.Contributor;
import no.unit.nva.model.EntityDescription;
import no.unit.nva.model.Identity;
import no.unit.nva.model.NameType;
import no.unit.nva.model.Role;
import no.unit.nva.model.exceptions.MalformedContributorException;
import nva.commons.core.JacocoGenerated;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.Value;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static java.util.Objects.isNull;

public final class ContributorExtractor {
    private static final Set<IRI> CONTRIBUTOR_PROPERTIES = Set.of(DcTerms.CREATOR.getIri(),
            DcTerms.CONTRIBUTOR.getIri());

    @JacocoGenerated
    private ContributorExtractor() {

    }

    public static void extract(EntityDescription entityDescription, Statement statement)
            throws MalformedContributorException {
        if (isContributor(statement.getPredicate())) {
            addContributor(entityDescription, statement.getObject());
        }
    }

    private static void addContributor(EntityDescription entityDescription, Value object)
            throws MalformedContributorException {
        if (object instanceof Literal) {
            Contributor contributor = new Contributor.Builder()
                    .withRole(Role.CREATOR)
                    .withIdentity(extractIdentity(object))
                    .build();
            List<Contributor> contributorList = entityDescription.getContributors();
            if (isNull(contributorList)) {
                ArrayList<Contributor> newList = new ArrayList<>();
                newList.add(contributor);
                entityDescription.setContributors(newList);
            } else {
                contributorList.add(contributor);
            }
        }
    }

    private static Identity extractIdentity(Value object) {
        return new Identity.Builder()
                .withName(object.stringValue())
                .withNameType(NameType.PERSONAL)
                .build();
    }

    private static boolean isContributor(IRI target) {
        return CONTRIBUTOR_PROPERTIES.contains(target);
    }
}
