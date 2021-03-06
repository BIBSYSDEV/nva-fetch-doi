package no.unit.nva.doi.fetch.service;

import no.unit.nva.doi.transformer.utils.BareProxyClient;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.model.Contributor;
import no.unit.nva.model.EntityDescription;
import no.unit.nva.model.Identity;
import no.unit.nva.model.Organization;
import no.unit.nva.model.Publication;
import no.unit.nva.model.Role;
import no.unit.nva.model.exceptions.MalformedContributorException;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.net.URISyntaxException;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class IdentityUpdaterTest {

    public static final String SAMPLE_ORCID = "https://sandbox.orcid.org/0000-1111-2222-3333";
    public static final String SAMPLE_ARPID = "https://api.dev.nva.aws.unit.no/person/123456789";
    public static final String ILLEGAL_ORCID = "hts:sdbox.orcid.org?0'0-1111-2222-3333";

    @Test
    public void enrichPublicationCreatorsUpdatesPublicationWithCreatorHavingOrcidAndNoArpid() throws URISyntaxException,
            MalformedContributorException {
        Identity identity = createIdentityWithOrcid();
        BareProxyClient bareProxyClient = mock(BareProxyClient.class);
        when(bareProxyClient.lookupArpidForOrcid(any())).thenReturn(Optional.of(SAMPLE_ARPID));
        Publication publication = createPublicationWithIdentity(identity);

        List<String> intialArpIds = getArpIds(publication);
        assertFalse(intialArpIds.contains(SAMPLE_ARPID));

        Publication updatedPublication = IdentityUpdater.enrichPublicationCreators(bareProxyClient, publication);
        List<String> ids = getIdentifierIds(updatedPublication);
        assertTrue(ids.contains(SAMPLE_ARPID));
    }

    private List<String> getArpIds(Publication updatedPublication) {
        return updatedPublication.getEntityDescription().getContributors().stream()
                .map(Contributor::getIdentity).map(Identity::getArpId).collect(Collectors.toList());
    }

    private List<String> getIdentifierIds(Publication updatedPublication) {
        return updatedPublication.getEntityDescription().getContributors().stream()
                .map(Contributor::getIdentity).map(Identity::getId).map(URI::toString).collect(Collectors.toList());
    }


    @Test
    public void enrichPublicationCreatorsDoesNotAlterPublicationWithoutOrcid() throws URISyntaxException,
            MalformedContributorException {
        Identity identity = new Identity();
        BareProxyClient bareProxyClient = mock(BareProxyClient.class);
        Publication publication = createPublicationWithIdentity(identity);
        Publication updatedPublication = IdentityUpdater.enrichPublicationCreators(bareProxyClient, publication);
        assertNotNull(publication);
        assertNotNull(updatedPublication);
        assertEquals(publication, updatedPublication);
    }

    @Test
    public void enrichPublicationCreatorsDoesNotAlterPublicationWithoutEntityDescription() throws URISyntaxException,
            MalformedContributorException {
        Identity identity = new Identity();
        BareProxyClient bareProxyClient = mock(BareProxyClient.class);
        Publication publication = createPublicationWithIdentity(identity);
        publication.setEntityDescription(null);
        Publication updatedPublication = IdentityUpdater.enrichPublicationCreators(bareProxyClient, publication);
        assertNotNull(publication);
        assertNotNull(updatedPublication);
    }


    @Test
    public void enrichPublicationCreatorsDoesUpdatePublicationCreatorWithOrcid() throws URISyntaxException,
            MalformedContributorException {
        Identity identity = createIdentityWithOrcid();
        BareProxyClient bareProxyClient = mock(BareProxyClient.class);
        Publication publication = createPublicationWithIdentity(identity);
        Publication updatedPublication = IdentityUpdater.enrichPublicationCreators(bareProxyClient, publication);
        assertNotNull(publication);
        assertNotNull(updatedPublication);
    }

    @Test
    public void enrichPublicationCreatorsDoesThrowExceptionWhenOrcidHasErrors() throws URISyntaxException,
            MalformedContributorException {
        Identity identity = new Identity();
        identity.setOrcId(ILLEGAL_ORCID);
        BareProxyClient bareProxyClient = mock(BareProxyClient.class);
        when(bareProxyClient.lookupArpidForOrcid(any())).thenThrow(IllegalArgumentException.class);
        Publication publication = createPublicationWithIdentity(identity);

        assertThrows(IllegalArgumentException.class,
            () -> IdentityUpdater.enrichPublicationCreators(bareProxyClient, publication));
    }


    private Identity createIdentityWithOrcid() {
        Identity identity = new Identity();
        identity.setOrcId(SAMPLE_ORCID);
        return identity;
    }


    private Publication createPublicationWithIdentity(Identity identity) throws MalformedContributorException {
        Contributor contributor = new Contributor.Builder()
                .withRole(Role.CREATOR)
                .withIdentity(identity)
                .build();
        return new Publication.Builder()
                .withIdentifier(new SortableIdentifier(UUID.randomUUID().toString()))
                .withModifiedDate(Instant.now())
                .withOwner("owner")
                .withPublisher(new Organization.Builder()
                        .withId(URI.create("http://example.org/publisher/1"))
                        .build()
                )
                .withEntityDescription(new EntityDescription.Builder()
                        .withContributors(List.of(contributor))
                        .build())
                .build();
    }
}