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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.Function;

import static java.util.Objects.isNull;

public final class ContributorExtractor {
    private static final Logger logger = LoggerFactory.getLogger(ContributorExtractor.class);
    private static final Set<IRI> CONTRIBUTOR_PROPERTIES = Set.of(DcTerms.CREATOR.getIri(),
            DcTerms.CONTRIBUTOR.getIri());
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

    @SuppressWarnings("PMD.CloseResource")
    private static EntityDescription extract(ExtractionPair extractionPair) throws MalformedContributorException {
        Statement statement = extractionPair.getStatement();
        EntityDescription entityDescription = extractionPair.getEntityDescription();
        if (isContributor(statement.getPredicate())) {
            addContributor(entityDescription, statement.getObject());
        }
        return entityDescription;
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
