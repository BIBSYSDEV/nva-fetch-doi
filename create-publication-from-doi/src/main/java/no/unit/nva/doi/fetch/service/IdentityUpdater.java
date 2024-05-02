package no.unit.nva.doi.fetch.service;

import java.util.List;
import no.unit.nva.doi.transformer.utils.CristinProxyClient;
import no.unit.nva.model.Contributor;
import no.unit.nva.model.EntityDescription;
import no.unit.nva.model.Identity;
import no.unit.nva.model.Publication;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;

public final class IdentityUpdater {

    public static final String PROBLEM_UPDATING_IDENTITY_MESSAGE = "Problem updating Identity, ignoring and moving on";
    private static final Logger logger = LoggerFactory.getLogger(IdentityUpdater.class);
    public static final int MAX_CONTRIBUTORS_TO_LOOKUP = 10;

    private IdentityUpdater() {
    }

    /**
     * Tries to update an Identity with identifier if it has an Orcid and no identifier.
     *
     * @param cristinProxyClient instantiated CristinProxyClient
     * @param publication        publication which need updating Identity
     * @return an updated publication with identifiers added to identities or the original if unchanged or exception
     *             occurs for some reason
     */
    public static Publication enrichPublicationCreators(CristinProxyClient cristinProxyClient,
                                                        Publication publication) {

        var possibleContributors = extractPossibleContributors(publication);
        possibleContributors.ifPresent(contributors -> tryUpdatingContributorsOrLogError(cristinProxyClient,
                                                                                         publication,
                                                                                         contributors));
        return publication;
    }

    private static Optional<List<Contributor>> extractPossibleContributors(Publication publication) {
        return Optional.ofNullable(publication)
                   .map(Publication::getEntityDescription)
                   .map(EntityDescription::getContributors);
    }

    private static void tryUpdatingContributorsOrLogError(CristinProxyClient cristinProxyClient,
                                                          Publication publication, List<Contributor> contributors) {
        try {
            updateContributors(cristinProxyClient, publication, contributors);
        } catch (Exception e) {
            logger.info(PROBLEM_UPDATING_IDENTITY_MESSAGE, e);
        }
    }

    private static void updateContributors(CristinProxyClient cristinProxyClient, Publication publication,
                                           List<Contributor> contributors) {
        var contributorsWithOnlyOrcid = contributors.stream()
                                            .filter(IdentityUpdater::hasOrcidButNotIdentifier).toList();
        if (contributorsWithOnlyOrcid.size() > MAX_CONTRIBUTORS_TO_LOOKUP) {
            logger.warn("Skipper updateContributors as too many without known cristin-identifier "
                        + publication.getIdentifier());
            return;
        }

        contributorsWithOnlyOrcid
            .forEach(contributor -> updateIdentifierIfFoundFromOrcid(cristinProxyClient, contributor.getIdentity())
        );
    }

    private static boolean hasOrcidButNotIdentifier(Contributor contributor) {
        var identity = contributor.getIdentity();
        return nonNull(identity) && isNull(identity.getId()) && nonNull(identity.getOrcId());
    }

    private static void updateIdentifierIfFoundFromOrcid(CristinProxyClient cristinProxyClient, Identity identity) {
        var identifier = cristinProxyClient.lookupIdentifierFromOrcid(identity.getOrcId());
        identifier.ifPresent(identity::setId);
    }
}
