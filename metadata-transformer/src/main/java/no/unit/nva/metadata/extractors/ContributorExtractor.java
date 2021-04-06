package no.unit.nva.metadata.extractors;

import no.unit.nva.model.Contributor;
import no.unit.nva.model.EntityDescription;
import no.unit.nva.model.Identity;
import no.unit.nva.model.NameType;
import no.unit.nva.model.Role;
import no.unit.nva.model.exceptions.MalformedContributorException;
import nva.commons.core.JacocoGenerated;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import static java.util.Objects.isNull;

public final class ContributorExtractor {
    private static final Logger logger = LoggerFactory.getLogger(ContributorExtractor.class);

    public static final Function<ExtractionPair, EntityDescription> apply = extractOrConsumeError();

    @JacocoGenerated
    private ContributorExtractor() {

    }

    private static Function<ExtractionPair, EntityDescription> extractOrConsumeError() {
        return (extractionPair) -> {
            try {
                return ContributorExtractor.extract(extractionPair);
            } catch (MalformedContributorException e) {
                logger.warn("Could not create contributor for statement " + extractionPair.getStatement());
                return extractionPair.getEntityDescription();
            }
        };
    }

    private static EntityDescription extract(ExtractionPair extractionPair) throws MalformedContributorException {
        if (extractionPair.isContributor()) {
            addContributor(extractionPair);
        }
        return extractionPair.getEntityDescription();
    }

    private static void addContributor(ExtractionPair extractionPair)
            throws MalformedContributorException {
        Contributor contributor = new Contributor.Builder()
                .withRole(Role.CREATOR)
                .withIdentity(extractIdentity(extractionPair.getObject()))
                .build();
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

    private static Identity extractIdentity(String name) {
        return new Identity.Builder()
                .withName(name)
                .withNameType(NameType.PERSONAL)
                .build();
    }
}
