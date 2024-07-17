package no.unit.nva.doi.fetch.service;

import static no.unit.nva.testutils.RandomDataGenerator.randomUri;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import java.net.URI;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import no.unit.nva.doi.fetch.commons.publication.model.Contributor;
import no.unit.nva.doi.fetch.commons.publication.model.CreatePublicationRequest;
import no.unit.nva.doi.fetch.commons.publication.model.EntityDescription;
import no.unit.nva.doi.fetch.commons.publication.model.Identity;
import no.unit.nva.doi.fetch.commons.publication.model.Role;
import no.unit.nva.doi.transformer.utils.CristinProxyClient;
import org.hamcrest.beans.HasPropertyWithValue;
import org.hamcrest.core.Every;
import org.junit.jupiter.api.Test;

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
        var initialIdentifiers = getIdentifiers(publication);

        assertThat(hasIdentifierMatchingSample(initialIdentifiers), equalTo(false));

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
        var identity = new Identity(null, null, null, ILLEGAL_ORCID);
        var publication = createPublicationWithIdentity(identity);
        var cristinProxyClient = mock(CristinProxyClient.class);
        doThrow(new RuntimeException()).when(cristinProxyClient).lookupIdentifierFromOrcid(any());
        var updatedPublication = IdentityUpdater.enrichPublicationCreators(cristinProxyClient, publication);

        assertThat(updatedPublication, equalTo(publication));
    }

    @Test
    public void shouldNotUpdateIdentifierWhenIdentifierAlreadyExistsWhenEnrichingPublicationCreators() {
        var identity = new Identity(randomUri(), null, null, SAMPLE_ORCID);
        var publication = createPublicationWithIdentity(identity);
        var cristinProxyClient = mock(CristinProxyClient.class);
        var updatedPublication = IdentityUpdater.enrichPublicationCreators(cristinProxyClient, publication);

        assertThat(updatedPublication, equalTo(publication));
    }

    @Test
    public void shouldNotAlterPublicationWhenMissingIdentityCallingEnrichPublicationCreators() {
        var publication = createPublicationWithIdentities(List.of());
        var cristinProxyClient = mock(CristinProxyClient.class);
        var updatedPublication = IdentityUpdater.enrichPublicationCreators(cristinProxyClient, publication);

        assertThat(updatedPublication, equalTo(publication));
    }

    @Test
    public void shouldNotEnrichContributorsWhenMoreThanTenUnverifiedContributorsExist() {
        var identities = Stream.generate(this::createIdentityWithOrcid).limit(11).toList();
        var publication = createPublicationWithIdentities(identities);
        var cristinProxyClient = mock(CristinProxyClient.class);
        var sampleIdentifier = URI.create(SAMPLE_IDENTITY_IDENTIFIER);
        when(cristinProxyClient.lookupIdentifierFromOrcid(any())).thenReturn(Optional.of(sampleIdentifier));

        var updatedPublication = IdentityUpdater.enrichPublicationCreators(cristinProxyClient, publication);

        assertThat(identities, Every.everyItem(HasPropertyWithValue.hasProperty("id", equalTo(null))));
        assertThat(updatedPublication, equalTo(publication));
    }

    private List<URI> getIdentifiers(CreatePublicationRequest publication) {
        return Optional.ofNullable(publication)
                   .map(CreatePublicationRequest::getEntityDescription)
                   .map(EntityDescription::getContributors)
                   .orElse(Collections.emptyList())
                   .stream()
                   .map(Contributor::identity)
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
        return new Identity(null, null, null, SAMPLE_ORCID);
    }

    private CreatePublicationRequest createPublicationWithIdentity(Identity identity) {
        return createPublicationWithIdentities(List.of(identity));
    }

    private CreatePublicationRequest createPublicationWithIdentities(List<Identity> identities) {
        var contributors = identities.stream().map(identity -> new Contributor.Builder()
                                                                   .withRole(new Role("Creator"))
                                                                   .withIdentity(identity)
                                                                   .build())
                               .toList();

        return new CreatePublicationRequest.Builder()
                   .withEntityDescription(new EntityDescription.Builder()
                                              .withContributors(contributors)
                                              .build())
                   .build();
    }
}