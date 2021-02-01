package no.unit.nva.doi.fetch.service;

import no.unit.nva.doi.transformer.utils.BareProxyClient;
import no.unit.nva.model.Contributor;
import no.unit.nva.model.Identity;
import no.unit.nva.model.Publication;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URISyntaxException;
import java.util.Optional;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;

public final class IdentityUpdater {

    public static final String PROBLEM_UPDATING_IDENTITY_MESSAGE = "Problem updating Identity";
    public static final String IGNORING_EXCEPTION = "Ignoring exception: ";
    private static final Logger logger = LoggerFactory.getLogger(IdentityUpdater.class);

    private IdentityUpdater() {
    }

    /**
     * Tries to update an Identity if it has an Orcid and no Arpid.
     *
     * @param bareProxyClient instantiated BareProxyClient
     * @param publication     publication which need updating Identity
     * @return an updated publication or the original if unchanged or exception occurs for some reason
     * @throws URISyntaxException thrown when error in orcid
     */
    @SuppressWarnings("PMD.AvoidRethrowingException")
    public static Publication enrichPublicationCreators(BareProxyClient bareProxyClient, Publication publication)
            throws URISyntaxException {
        try {
            if (nonNull(publication) && nonNull(publication.getEntityDescription())) {
                for (Contributor contributor : publication.getEntityDescription().getContributors()) {
                    final Identity identity = contributor.getIdentity();
                    if (possibleArpCandidate(identity)) {
                        updateIdentity(bareProxyClient, identity);
                    }
                }
            }
        } catch (IllegalArgumentException | URISyntaxException e) {
            logger.info(PROBLEM_UPDATING_IDENTITY_MESSAGE, e);
            throw e;
        } catch (Exception e) {
            logger.info(IGNORING_EXCEPTION, e);
        }
        return publication;
    }

    private static boolean possibleArpCandidate(Identity identity) {
        return isNull(identity.getArpId()) && nonNull(identity.getOrcId());
    }

    private static Identity updateIdentity(BareProxyClient bareProxyClient, Identity identity)
            throws URISyntaxException {
        try {
            Optional<String> arpId = bareProxyClient.lookupArpidForOrcid(identity.getOrcId());
            arpId.ifPresent(identity::setArpId);
        } catch (IllegalArgumentException e) {
            logger.info(PROBLEM_UPDATING_IDENTITY_MESSAGE, e);
            throw e;
        }
        return identity;
    }
}
