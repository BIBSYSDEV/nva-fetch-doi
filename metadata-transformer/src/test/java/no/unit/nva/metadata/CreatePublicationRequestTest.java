package no.unit.nva.metadata;

import org.junit.Ignore;

@Ignore
class CreatePublicationRequestTest {

//    private static final String RIGHTS_HOLDER = "My imagined rights holder";
//    private static final Set<String> FIELDS_TO_IGNORE = Set.of("importDetails");
//
//    @Test
//    void shouldRoundTripToJsonWithoutInformationLoss() throws JsonProcessingException {
//        CreatePublicationRequest originalRequest = sampleRequest();
//        var json = JsonUtils.dtoObjectMapper.writeValueAsString(originalRequest);
//        var fromJson = CreatePublicationRequest.fromJson(json);
//        assertThat(fromJson, is(equalTo(originalRequest)));
//    }
//
//    private CreatePublicationRequest sampleRequest() {
//        var sample = PublicationGenerator.randomPublication();
//        addReferenceToEntityDescription(sample.getEntityDescription());
//
//        CreatePublicationRequest request = new CreatePublicationRequest();
//        request.setAdditionalIdentifiers(sample.getAdditionalIdentifiers());
//        request.setContext(randomJsonNode());
//        request.setEntityDescription(sample.getEntityDescription());
//        request.setProjects(sample.getProjects());
//        request.setAssociatedArtifacts(sample.getAssociatedArtifacts());
//        request.setSubjects(sample.getSubjects());
//        request.setFundings(sample.getFundings());
//        request.setRightsHolder(RIGHTS_HOLDER);
//        assertThat(request, doesNotHaveEmptyValuesIgnoringFields(FIELDS_TO_IGNORE));
//        return request;
//    }
//
//    private void addReferenceToEntityDescription(EntityDescription entityDescription) {
//        entityDescription.setReference(
//            new Reference.Builder().withDoi(randomDoi()).withPublicationInstance(
//                PublicationInstanceBuilder.randomPublicationInstance(AcademicChapter.class)).withPublishingContext(
//                PublicationContextBuilder.randomPublicationContext(AcademicChapter.class)).build()
//        );
//    }
//
//    private JsonNode randomJsonNode() {
//        return attempt(() -> JsonUtils.dtoObjectMapper.readTree(randomJson())).orElseThrow();
//    }
}