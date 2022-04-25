package no.unit.nva.doi.fetch.service;

class IdentityUpdaterTest {

//    public static final String SAMPLE_ORCID = "https://sandbox.orcid.org/0000-1111-2222-3333";
//    public static final String SAMPLE_ARPID = "https://api.dev.nva.aws.unit.no/person/123456789";
//    public static final String ILLEGAL_ORCID = "hts:sdbox.orcid.org?0'0-1111-2222-3333";
//
//    @Test
//    public void enrichPublicationCreatorsUpdatesPublicationWithCreatorHavingOrcidAndNoArpid() throws
//            URISyntaxException {
//        Identity identity = createIdentityWithOrcid();
//        BareProxyClient bareProxyClient = mock(BareProxyClient.class);
//        when(bareProxyClient.lookupArpidForOrcid(any())).thenReturn(Optional.of(SAMPLE_ARPID));
//        Publication publication = createPublicationWithIdentity(identity);
//
//        List<String> intialArpIds = getArpIds(publication);
//        assertFalse(intialArpIds.contains(SAMPLE_ARPID));
//
//        Publication updatedPublication = IdentityUpdater.enrichPublicationCreators(bareProxyClient, publication);
//        List<String> ids = getIdentifierIds(updatedPublication);
//        assertTrue(ids.contains(SAMPLE_ARPID));
//    }
//
//    private List<String> getArpIds(Publication updatedPublication) {
//        return updatedPublication.getEntityDescription().getContributors().stream()
//                .map(Contributor::getIdentity).map(Identity::getArpId).collect(Collectors.toList());
//    }
//
//    private List<String> getIdentifierIds(Publication updatedPublication) {
//        return updatedPublication.getEntityDescription().getContributors().stream()
//                .map(Contributor::getIdentity).map(Identity::getId).map(URI::toString).collect(Collectors.toList());
//    }
//
//
//    @Test
//    public void enrichPublicationCreatorsDoesNotAlterPublicationWithoutOrcid() {
//        Identity identity = new Identity();
//        BareProxyClient bareProxyClient = mock(BareProxyClient.class);
//        Publication publication = createPublicationWithIdentity(identity);
//        Publication updatedPublication = IdentityUpdater.enrichPublicationCreators(bareProxyClient, publication);
//        assertNotNull(publication);
//        assertNotNull(updatedPublication);
//        assertEquals(publication, updatedPublication);
//    }
//
//    @Test
//    public void enrichPublicationCreatorsDoesNotAlterPublicationWithoutEntityDescription() {
//        Identity identity = new Identity();
//        BareProxyClient bareProxyClient = mock(BareProxyClient.class);
//        Publication publication = createPublicationWithIdentity(identity);
//        publication.setEntityDescription(null);
//        Publication updatedPublication = IdentityUpdater.enrichPublicationCreators(bareProxyClient, publication);
//        assertNotNull(publication);
//        assertNotNull(updatedPublication);
//    }
//
//
//    @Test
//    public void enrichPublicationCreatorsDoesUpdatePublicationCreatorWithOrcid() {
//        Identity identity = createIdentityWithOrcid();
//        BareProxyClient bareProxyClient = mock(BareProxyClient.class);
//        Publication publication = createPublicationWithIdentity(identity);
//        Publication updatedPublication = IdentityUpdater.enrichPublicationCreators(bareProxyClient, publication);
//        assertNotNull(publication);
//        assertNotNull(updatedPublication);
//    }
//
//    @Test
//    public void enrichPublicationCreatorsDoesThrowExceptionWhenOrcidHasErrors() throws URISyntaxException {
//        Identity identity = new Identity();
//        identity.setOrcId(ILLEGAL_ORCID);
//        BareProxyClient bareProxyClient = mock(BareProxyClient.class);
//        when(bareProxyClient.lookupArpidForOrcid(any())).thenThrow(IllegalArgumentException.class);
//        Publication publication = createPublicationWithIdentity(identity);
//
//        assertThrows(IllegalArgumentException.class,
//            () -> IdentityUpdater.enrichPublicationCreators(bareProxyClient, publication));
//    }
//
//
//    private Identity createIdentityWithOrcid() {
//        Identity identity = new Identity();
//        identity.setOrcId(SAMPLE_ORCID);
//        return identity;
//    }
//
//
//    private Publication createPublicationWithIdentity(Identity identity) {
//        Contributor contributor = new Contributor.Builder()
//                .withRole(Role.CREATOR)
//                .withIdentity(identity)
//                .build();
//        return new Publication.Builder()
//                .withIdentifier(new SortableIdentifier(UUID.randomUUID().toString()))
//                .withModifiedDate(Instant.now())
//                .withOwner("owner")
//                .withPublisher(new Organization.Builder()
//                        .withId(URI.create("http://example.org/publisher/1"))
//                        .build()
//                )
//                .withEntityDescription(new EntityDescription.Builder()
//                        .withContributors(List.of(contributor))
//                        .build())
//                .build();
//    }
}