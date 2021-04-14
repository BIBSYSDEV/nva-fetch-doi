package no.unit.nva.metadata.extractors;

import no.unit.nva.model.Contributor;
import no.unit.nva.model.EntityDescription;
import no.unit.nva.model.Identity;
import no.unit.nva.model.NameType;
import no.unit.nva.model.Role;
import nva.commons.core.JacocoGenerated;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import static java.util.Objects.isNull;
import static nva.commons.core.attempt.Try.attempt;

public final class ContributorExtractor {

    public static final Function<ExtractionPair, EntityDescription> apply = ContributorExtractor::extract;

    @JacocoGenerated
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
            contributorList.add(contributor);
        }
    }

    private static Contributor createContributorWithoutCorrespondingAuthorInfo(ExtractionPair extractionPair) {
        return attempt(() -> new Contributor.Builder()
                .withRole(Role.CREATOR)
                .withIdentity(extractIdentity(extractionPair.getStatementLiteral()))
                .build()).orElseThrow();
    }

    private static Identity extractIdentity(String name) {
        return new Identity.Builder()
                .withName(name)
                .withNameType(NameType.PERSONAL)
                .build();
    }
}
