package no.unit.nva.metadata;

import no.unit.nva.PublicationMapper;
import no.unit.nva.api.CreatePublicationRequest;
import no.unit.nva.model.Contributor;
import no.unit.nva.model.EntityDescription;
import no.unit.nva.model.Identity;
import no.unit.nva.model.Organization;
import no.unit.nva.model.Publication;
import no.unit.nva.model.exceptions.MalformedContributorException;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.URI;
import java.util.List;

import static nva.commons.core.JsonUtils.objectMapper;

public class MetadataConverterTest {

    @Test
    public void test() throws MalformedContributorException, IOException {
        Contributor contributor = new Contributor.Builder()
                .withIdentity(new Identity.Builder().withName("Mr. Contributor").build())
                .build();
        EntityDescription entityDescription = new EntityDescription.Builder()
                .withDescription("Description")
                .withMainTitle("Main title")
                .withTags(List.of("A, B, C"))
                .withContributors(List.of(contributor))
                .build();

        CreatePublicationRequest request = new CreatePublicationRequest();
        request.setEntityDescription(entityDescription);

        objectMapper.writeValue(System.out, request);

        Publication publication = PublicationMapper.toNewPublication(
                request,
                "Owner",
                URI.create("http://example.org/handle"),
                URI.create("http://example.org/link"),
                new Organization.Builder().withId(URI.create("http://example.org/org")).build());

        objectMapper.writeValue(System.out, publication);
    }

}
