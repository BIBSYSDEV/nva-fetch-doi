package no.unit.nva.doi.fetch.service;

public final class IdentityUpdater {

//    public static final String PROBLEM_UPDATING_IDENTITY_MESSAGE = "Problem updating Identity";
//    public static final String IGNORING_EXCEPTION = "Ignoring exception: ";
//    private static final Logger logger = LoggerFactory.getLogger(IdentityUpdater.class);
//
//    private IdentityUpdater() {
//    }
//
//    /**
//     * Tries to update an Identity if it has an Orcid and no Arpid.
//     *
//     * @param bareProxyClient instantiated BareProxyClient
//     * @param publication     publication which need updating Identity
//     * @return an updated publication or the original if unchanged or exception occurs for some reason
//     * @throws URISyntaxException thrown when error in orcid
//     */
//    @SuppressWarnings("PMD.AvoidRethrowingException")
//    public static Publication enrichPublicationCreators(BareProxyClient bareProxyClient, Publication publication) {
//        try {
//            if (publicationHasData(publication)) {
//                updateContributors(bareProxyClient, publication);
//            }
//        } catch (IllegalArgumentException | URISyntaxException e) {
//            logErrorAndThrowIllegalArgumentException(new IllegalArgumentException(e));
//        } catch (Exception e) {
//            logger.info(IGNORING_EXCEPTION, e);
//        }
//        return publication;
//    }
//
//    private static void logErrorAndThrowIllegalArgumentException(IllegalArgumentException e) {
//        logger.info(PROBLEM_UPDATING_IDENTITY_MESSAGE, e);
//        throw e;
//    }
//
//    private static void updateContributors(BareProxyClient bareProxyClient, Publication publication)
//            throws URISyntaxException {
//        for (Contributor contributor : publication.getEntityDescription().getContributors()) {
//            final Identity identity = contributor.getIdentity();
//            if (possibleArpCandidate(identity)) {
//                updateIdentity(bareProxyClient, identity);
//            }
//        }
//    }
//
//    private static boolean publicationHasData(Publication publication) {
//        return nonNull(publication) && nonNull(publication.getEntityDescription());
//    }
//
//    private static boolean possibleArpCandidate(Identity identity) {
//        return isNull(identity.getArpId()) && nonNull(identity.getOrcId());
//    }
//
//    private static Identity updateIdentity(BareProxyClient bareProxyClient, Identity identity)
//            throws URISyntaxException {
//        try {
//            Optional<String> arpId = bareProxyClient.lookupArpidForOrcid(identity.getOrcId());
//            if (arpId.isPresent()) {
//                identity.setId(URI.create(arpId.get()));
//            }
//        } catch (IllegalArgumentException e) {
//            logErrorAndThrowIllegalArgumentException(e);
//        }
//        return identity;
//    }
}
