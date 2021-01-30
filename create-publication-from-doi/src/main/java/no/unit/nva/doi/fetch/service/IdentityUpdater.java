package no.unit.nva.doi.fetch.service;

import no.unit.nva.doi.transformer.utils.BareProxyClient;
import no.unit.nva.model.Contributor;
import no.unit.nva.model.Identity;
import no.unit.nva.model.Publication;
import nva.commons.utils.JacocoGenerated;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Optional;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;

public class IdentityUpdater {

    public static final String PROBLEM_UPDATING_IDENTITY_MESSAGE = "Problem updating Identity";
    private static final Logger logger = LoggerFactory.getLogger(IdentityUpdater.class);
    private static final transient BareProxyClient bareProxyClient = new BareProxyClient();

    /**
     * Tries to update an Identity if it has an Orcid and no Arpid.
     * @param apiUrl URL to BareProxyService
     * @param publication publication which need updating Identity
     * @return an updatet publication or the original if unchanged or exception occurs for some reason
     */
    @JacocoGenerated
    public static Publication enrichPublicationCreators(URI apiUrl, Publication publication) {
        try {
            for (Contributor contributor : publication.getEntityDescription().getContributors()) {
                final Identity identity = contributor.getIdentity();
                if (possibleArpCandidate(identity)) {
                    updateIdentity(apiUrl, identity);
                }
            }
        } catch (Exception e) {
            logger.info("Ignoring exception: ",e);
        }
        return publication;
    }

    @JacocoGenerated
    private static boolean possibleArpCandidate(Identity identity) {
        return isNull(identity.getArpId()) && nonNull(identity.getOrcId());
    }

    @JacocoGenerated
    private static void updateIdentity(URI apiUrl, Identity identity) {
        try {
            Optional<String> arpId = bareProxyClient.lookupArpidForOrcid(apiUrl, identity.getOrcId());
            arpId.ifPresent(identity::setArpId);
        } catch (URISyntaxException e) {
            logger.info(PROBLEM_UPDATING_IDENTITY_MESSAGE, e);
        }
    }

}
