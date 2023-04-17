package no.unit.nva.doi.fetch.service;

import java.util.Collections;
import java.util.Objects;
import no.unit.nva.doi.transformer.utils.CristinProxyClient;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.model.Contributor;
import no.unit.nva.model.EntityDescription;
import no.unit.nva.model.Identity;
import no.unit.nva.model.Organization;
import no.unit.nva.model.Publication;
import no.unit.nva.model.ResourceOwner;
import no.unit.nva.model.role.Role;
import no.unit.nva.model.role.RoleType;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static no.unit.nva.testutils.RandomDataGenerator.randomUri;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class IdentityUpdaterTest {

    public static final String SAMPLE_ORCID = "https://sandbox.orcid.org/0000-1111-2222-3333";
    public static final String ILLEGAL_ORCID = "hts:sdbox.orcid.org?0'0-1111-2222-3333";
    public static final String SAMPLE_IDENTITY_IDENTIFIER = "https://api.dev.nva.aws.unit.no/cristin/person/123456";

    @Test
    public void shouldUpdatePublicationWithIdentifierWhenEnrichingPublicationAndCreatorHasOrcid() {
        var identity = createIdentityWithOrcid();
        var cristinProxyClient = mock(CristinProxyClient.class);
        var sampleIdentifier = URI.create(SAMPLE_IDENTITY_IDENTIFIER);
        when(cristinProxyClient.lookupIdentifierFromOrcid(any())).thenReturn(Optional.of(sampleIdentifier));
        var publication = createPublicationWithIdentity(identity);
        var intialIdentifiers = getIdentifiers(publication);

        assertThat(hasIdentifierMatchingSample(intialIdentifiers), equalTo(false));

        var updatedPublication = IdentityUpdater.enrichPublicationCreators(cristinProxyClient, publication);
        var updatedIdentifiers = getIdentifiers(updatedPublication);

        assertThat(hasIdentifierMatchingSample(updatedIdentifiers), equalTo(true));
    }

    @Test
    public void shouldNotAlterPublicationWhenEnrichingPublicationCreatorsWithoutOrcid() {
        var cristinProxyClient = mock(CristinProxyClient.class);
        var publication = createPublicationWithIdentity(new Identity());
        var updatedPublication = IdentityUpdater.enrichPublicationCreators(cristinProxyClient, publication);

        assertNotNull(publication);
        assertNotNull(updatedPublication);
        assertThat(updatedPublication, equalTo(publication));
    }

    @Test
    public void shouldIgnoreExceptionsAndReturnUnmodifiedPublicationWhenEnrichPublicationCreatorsThrowsException() {
        var identity = new Identity.Builder().withOrcId(ILLEGAL_ORCID).build();
        var publication = createPublicationWithIdentity(identity);
        var cristinProxyClient = mock(CristinProxyClient.class);
        doThrow(new RuntimeException()).when(cristinProxyClient).lookupIdentifierFromOrcid(any());
        var updatedPublication = IdentityUpdater.enrichPublicationCreators(cristinProxyClient, publication);

        assertThat(updatedPublication, equalTo(publication));
    }

    @Test
    public void shouldNotUpdateIdentifierWhenIdentifierAlreadyExistsWhenEnrichingPublicationCreators() {
        var identity = new Identity.Builder().withId(randomUri()).withOrcId(SAMPLE_ORCID).build();
        var publication = createPublicationWithIdentity(identity);
        var cristinProxyClient = mock(CristinProxyClient.class);
        var updatedPublication = IdentityUpdater.enrichPublicationCreators(cristinProxyClient, publication);

        assertThat(updatedPublication, equalTo(publication));
    }

    @Test
    public void shouldNotAlterPublicationWhenMissingIdentityCallingEnrichPublicationCreators() {
        var publication = createPublicationWithIdentity(null);
        var cristinProxyClient = mock(CristinProxyClient.class);
        var updatedPublication = IdentityUpdater.enrichPublicationCreators(cristinProxyClient, publication);

        assertThat(updatedPublication, equalTo(publication));
    }

    private List<URI> getIdentifiers(Publication publication) {
        return Optional.ofNullable(publication)
                   .map(Publication::getEntityDescription)
                   .map(EntityDescription::getContributors)
                   .orElse(Collections.emptyList())
                   .stream()
                   .map(Contributor::getIdentity)
                   .filter(Objects::nonNull)
                   .map(Identity::getId)
                   .filter(Objects::nonNull)
                   .collect(Collectors.toList());
    }

    private boolean hasIdentifierMatchingSample(List<URI> identifiers) {
        return identifiers.stream()
                   .filter(Objects::nonNull)
                   .map(URI::toString)
                   .anyMatch(SAMPLE_IDENTITY_IDENTIFIER::contains);
    }

    private Identity createIdentityWithOrcid() {
        return new Identity.Builder().withOrcId(SAMPLE_ORCID).build();
    }


    private Publication createPublicationWithIdentity(Identity identity) {
        var contributor = new Contributor.Builder()
                .withRole(new RoleType(Role.CREATOR))
                .withIdentity(identity)
                .build();
        return new Publication.Builder()
                .withIdentifier(new SortableIdentifier(UUID.randomUUID().toString()))
                .withModifiedDate(Instant.now())
                .withResourceOwner(randomResourceOwner())
                .withPublisher(new Organization.Builder()
                        .withId(URI.create("http://example.org/publisher/1"))
                        .build()
                )
                .withEntityDescription(new EntityDescription.Builder()
                        .withContributors(List.of(contributor))
                        .build())
                .build();
    }

    private ResourceOwner randomResourceOwner() {
        return new ResourceOwner(randomString(),randomUri());
    }
}