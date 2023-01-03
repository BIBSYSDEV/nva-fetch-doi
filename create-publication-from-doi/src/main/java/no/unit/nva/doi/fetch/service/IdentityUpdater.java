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
import java.util.stream.Collectors;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;

public final class IdentityUpdater {

    public static final String PROBLEM_UPDATING_IDENTITY_MESSAGE = "Problem updating Identity, ignoring and moving on";
    private static final Logger logger = LoggerFactory.getLogger(IdentityUpdater.class);

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
                                                                                         contributors));
        return publication;
    }

    private static Optional<List<Contributor>> extractPossibleContributors(Publication publication) {
        return Optional.ofNullable(publication)
                   .map(Publication::getEntityDescription)
                   .map(EntityDescription::getContributors);
    }

    private static void tryUpdatingContributorsOrLogError(CristinProxyClient cristinProxyClient,
                                                          List<Contributor> contributors) {
        try {
            updateContributors(cristinProxyClient, contributors);
        } catch (Exception e) {
            logger.info(PROBLEM_UPDATING_IDENTITY_MESSAGE, e);
        }
    }

    private static void updateContributors(CristinProxyClient cristinProxyClient, List<Contributor> contributors) {
        var orcids = contributors.stream().map(Contributor::getIdentity)
                .filter(IdentityUpdater::hasNoIdentifierButCanPossiblyBeFetchedUsingOrcid)
                .map(Identity::getOrcId)
                .collect(Collectors.toList());
        var orcidPersonUris = cristinProxyClient.lookupIdentifiersFromOrcid(orcids);

        contributors.forEach(contributor -> {
            var identity = contributor.getIdentity();

            Optional.ofNullable(identity.getOrcId())
                    .ifPresent(value -> identity.setId(orcidPersonUris.get(value)));
        });
    }

    private static boolean hasNoIdentifierButCanPossiblyBeFetchedUsingOrcid(Identity identity) {
        return nonNull(identity) && isNull(identity.getId()) && nonNull(identity.getOrcId());
    }
}
