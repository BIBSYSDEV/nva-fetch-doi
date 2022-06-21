package no.unit.nva.doi.fetch.service;

import no.unit.nva.doi.transformer.utils.CristinProxyClient;
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
        Optional.ofNullable(publication).map(Publication::getEntityDescription).ifPresent(entityDescription -> {
            try {
                updateContributors(cristinProxyClient, entityDescription);
            } catch (Exception e) {
                logger.info(PROBLEM_UPDATING_IDENTITY_MESSAGE, e);
            }
        });
        return publication;
    }

    private static void updateContributors(CristinProxyClient cristinProxyClient, EntityDescription entityDescription) {
        entityDescription.getContributors().forEach(contributor -> {
            var identity = contributor.getIdentity();
            if (hasNoIdentifierButCanPossiblyBeFetchedUsingOrcid(identity)) {
                updateIdentifierIfFoundFromOrcid(cristinProxyClient, identity);
            }
        });
    }

    private static boolean hasNoIdentifierButCanPossiblyBeFetchedUsingOrcid(Identity identity) {
        return isNull(identity.getId()) && nonNull(identity.getOrcId());
    }

    private static void updateIdentifierIfFoundFromOrcid(CristinProxyClient cristinProxyClient, Identity identity) {
        var identifier = cristinProxyClient.lookupIdentifierFromOrcid(identity.getOrcId());
        identifier.ifPresent(uri -> identity.setId(identifier.get()));
    }
}
