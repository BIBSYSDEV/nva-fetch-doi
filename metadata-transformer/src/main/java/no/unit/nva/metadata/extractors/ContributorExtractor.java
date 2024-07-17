package no.unit.nva.metadata.extractors;

import static java.util.Objects.isNull;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;
import no.unit.nva.doi.fetch.commons.publication.model.Contributor;
import no.unit.nva.doi.fetch.commons.publication.model.EntityDescription;
import no.unit.nva.doi.fetch.commons.publication.model.Identity;
import no.unit.nva.doi.fetch.commons.publication.model.Role;

public final class ContributorExtractor {

    public static final Function<ExtractionPair, EntityDescription> apply = ContributorExtractor::extract;

    private ContributorExtractor() {

    }

    private static EntityDescription extract(ExtractionPair extractionPair) {
        if (extractionPair.isContributor()) {
            addContributor(extractionPair);
        }
        return extractionPair.getEntityDescription();
    }

    private static void addContributor(ExtractionPair extractionPair) {
        Contributor contributor = createContributorWithoutCorrespondingAuthorInfo(extractionPair);
        EntityDescription entityDescription = extractionPair.getEntityDescription();
        List<Contributor> contributorList = entityDescription.getContributors();
        if (isNull(contributorList)) {
            ArrayList<Contributor> newList = new ArrayList<>();
            newList.add(contributor);
            entityDescription.setContributors(newList);
        } else {
            ArrayList<Contributor> newContributorList = new ArrayList<>(entityDescription.getContributors());
            newContributorList.add(contributor);
            entityDescription.setContributors(newContributorList);
        }
    }

    private static Contributor createContributorWithoutCorrespondingAuthorInfo(ExtractionPair extractionPair) {
        var role = new Role("Creator");
        var identity = extractIdentity(extractionPair.getStatementLiteral());

        return new Contributor(role, identity, Collections.emptyList(), null);
    }

    private static Identity extractIdentity(String name) {
        var nameType = "Personal";
        return new Identity(null, name, nameType, null);
    }
}
