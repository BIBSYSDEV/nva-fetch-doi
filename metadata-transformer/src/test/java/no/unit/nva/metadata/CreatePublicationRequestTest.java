package no.unit.nva.metadata;

import static no.unit.nva.hamcrest.DoesNotHaveEmptyValues.doesNotHaveEmptyValues;
import static no.unit.nva.testutils.RandomDataGenerator.randomJson;
import static nva.commons.core.attempt.Try.attempt;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import com.fasterxml.jackson.databind.JsonNode;
import no.unit.nva.commons.json.JsonUtils;
import no.unit.nva.model.testing.PublicationGenerator;
import org.junit.jupiter.api.Test;

class CreatePublicationRequestTest {

    @Test
    void shouldRoundTripToJsonWithoutInformationLoss() {
        CreatePublicationRequest orignalRequest = sampleRequest();
        var json = orignalRequest.toJsonString();
        var fromJson = CreatePublicationRequest.fromJson(json);
        assertThat(fromJson, is(equalTo(orignalRequest)));
    }

    private CreatePublicationRequest sampleRequest() {
        var sample = PublicationGenerator.randomPublication();
        CreatePublicationRequest request = new CreatePublicationRequest();
        request.setAdditionalIdentifiers(sample.getAdditionalIdentifiers());
        request.setContext(randomJsonNode());
        request.setEntityDescription(sample.getEntityDescription());
        request.setProjects(sample.getProjects());
        request.setFileSet(sample.getFileSet());
        request.setSubjects(sample.getSubjects());
        assertThat(request, doesNotHaveEmptyValues());
        return request;
    }

    private JsonNode randomJsonNode() {
        return attempt(() -> JsonUtils.dtoObjectMapper.readTree(randomJson())).orElseThrow();
    }
}