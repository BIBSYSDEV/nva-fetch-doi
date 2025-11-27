package no.unit.nva.doi.fetch.service;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static no.unit.nva.doi.fetch.commons.publication.model.VerificationStatus.NOT_VERIFIED;
import java.util.List;
import java.util.Optional;
import no.unit.nva.clients.cristin.CristinClient;
import no.unit.nva.clients.cristin.CristinPersonDto;
import no.unit.nva.doi.fetch.commons.publication.model.Contributor;
import no.unit.nva.doi.fetch.commons.publication.model.CreatePublicationRequest;
import no.unit.nva.doi.fetch.commons.publication.model.EntityDescription;
import no.unit.nva.doi.fetch.commons.publication.model.Identity;
import no.unit.nva.doi.fetch.commons.publication.model.VerificationStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class IdentityUpdater {

    public static final String PROBLEM_UPDATING_IDENTITY_MESSAGE = "Problem updating Identity, ignoring and moving on";
    private static final Logger logger = LoggerFactory.getLogger(IdentityUpdater.class);
    public static final int MAX_CONTRIBUTORS_TO_LOOKUP = 10;

    private IdentityUpdater() {
    }

    /**
     * Tries to update an Identity with identifier if it has an Orcid and no identifier.
     *
     * @param cristinClient instantiated CristinClient
     * @param publication        publication which need updating Identity
     * @return an updated publication with identifiers added to identities or the original if unchanged or exception
     *             occurs for some reason
     */
    public static CreatePublicationRequest enrichPublicationCreators(CristinClient cristinClient,
                                                                     CreatePublicationRequest publication) {

        var possibleContributors = extractPossibleContributors(publication);
        possibleContributors.ifPresent(contributors -> tryUpdatingContributorsOrLogError(cristinClient,
                                                                                         publication,
                                                                                         contributors));
        return publication;
    }

    private static Optional<List<Contributor>> extractPossibleContributors(CreatePublicationRequest publication) {
        return Optional.ofNullable(publication)
                   .map(CreatePublicationRequest::getEntityDescription)
                   .map(EntityDescription::getContributors);
    }

    private static void tryUpdatingContributorsOrLogError(CristinClient cristinClient,
                                                          CreatePublicationRequest publication,
                                                          List<Contributor> contributors) {
        try {
            updateContributors(cristinClient, publication, contributors);
        } catch (Exception e) {
            logger.info(PROBLEM_UPDATING_IDENTITY_MESSAGE, e);
        }
    }

    private static void updateContributors(CristinClient cristinClient, CreatePublicationRequest publication,
                                           List<Contributor> contributors) {
        var contributorsWithOnlyOrcid = contributors.stream()
                                            .filter(IdentityUpdater::hasOrcidButNotIdentifier).toList();
        if (contributorsWithOnlyOrcid.size() > MAX_CONTRIBUTORS_TO_LOOKUP) {
            logger.warn("Skipping updateContributors as too many without known cristin-identifier: {}",
                        publication.getEntityDescription().getMetadataSource());
            return;
        }

        contributorsWithOnlyOrcid
            .forEach(contributor -> updateIdentifierIfFoundFromOrcid(cristinClient, contributor.identity())
        );
    }

    private static boolean hasOrcidButNotIdentifier(Contributor contributor) {
        var identity = contributor.identity();
        return nonNull(identity) && isNull(identity.getId()) && nonNull(identity.getOrcId());
    }

    private static void updateIdentifierIfFoundFromOrcid(CristinClient cristinClient, Identity identity) {
        var person = cristinClient.getPerson(identity.getOrcId());
        if (person.isPresent()) {
            identity.setId(person.map(CristinPersonDto::id).orElse(null));
            identity.setVerificationStatus(person.map(CristinPersonDto::verified).map(VerificationStatus::fromBoolean).orElse(NOT_VERIFIED));
        }
    }
}
