package no.unit.nva.metadata;

import static no.unit.nva.hamcrest.DoesNotHaveEmptyValues.doesNotHaveEmptyValues;
import static no.unit.nva.hamcrest.DoesNotHaveEmptyValues.doesNotHaveEmptyValuesIgnoringFields;
import static no.unit.nva.testutils.RandomDataGenerator.randomDoi;
import static no.unit.nva.testutils.RandomDataGenerator.randomJson;
import static nva.commons.core.attempt.Try.attempt;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.List;
import java.util.Set;
import no.unit.nva.commons.json.JsonUtils;
import no.unit.nva.model.EntityDescription;
import no.unit.nva.model.Reference;
import no.unit.nva.model.instancetypes.chapter.AcademicChapter;
import no.unit.nva.model.testing.PublicationContextBuilder;
import no.unit.nva.model.testing.PublicationGenerator;
import no.unit.nva.model.testing.PublicationInstanceBuilder;
import org.junit.jupiter.api.Test;

class CreatePublicationRequestTest {

    private static final String RIGHTS_HOLDER = "My imagined rights holder";
    private static final Set<String> FIELDS_TO_IGNORE = Set.of("importDetails");

    @Test
    void shouldRoundTripToJsonWithoutInformationLoss() {
        CreatePublicationRequest originalRequest = sampleRequest();
        var json = originalRequest.toJsonString();
        var fromJson = CreatePublicationRequest.fromJson(json);
        assertThat(fromJson, is(equalTo(originalRequest)));
    }

    private CreatePublicationRequest sampleRequest() {
        var sample = PublicationGenerator.randomPublication();
        addReferenceToEntityDescription(sample.getEntityDescription());

        CreatePublicationRequest request = new CreatePublicationRequest();
        request.setAdditionalIdentifiers(sample.getAdditionalIdentifiers());
        request.setContext(randomJsonNode());
        request.setEntityDescription(sample.getEntityDescription());
        request.setProjects(sample.getProjects());
        request.setAssociatedArtifacts(sample.getAssociatedArtifacts());
        request.setSubjects(sample.getSubjects());
        request.setFundings(sample.getFundings());
        request.setRightsHolder(RIGHTS_HOLDER);
        assertThat(request, doesNotHaveEmptyValuesIgnoringFields(FIELDS_TO_IGNORE));
        return request;
    }

    private void addReferenceToEntityDescription(EntityDescription entityDescription) {
        entityDescription.setReference(
            new Reference.Builder().withDoi(randomDoi()).withPublicationInstance(
                PublicationInstanceBuilder.randomPublicationInstance(AcademicChapter.class)).withPublishingContext(
                PublicationContextBuilder.randomPublicationContext(AcademicChapter.class)).build()
        );
    }

    private JsonNode randomJsonNode() {
        return attempt(() -> JsonUtils.dtoObjectMapper.readTree(randomJson())).orElseThrow();
    }
}