package no.unit.nva.doi.fetch.commons.publication.model;

import static no.unit.nva.commons.json.JsonUtils.dtoObjectMapper;
import static no.unit.nva.doi.fetch.commons.publication.model.VerificationStatus.NOT_VERIFIED;
import static no.unit.nva.doi.fetch.commons.publication.model.VerificationStatus.VERIFIED;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.core.Is.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import com.fasterxml.jackson.core.JsonProcessingException;
import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import no.unit.nva.commons.json.JsonUtils;
import no.unit.nva.doi.fetch.commons.publication.model.contexttypes.Book;
import no.unit.nva.doi.fetch.commons.publication.model.contexttypes.UnconfirmedSeries;
import no.unit.nva.doi.fetch.commons.publication.model.instancetypes.AcademicMonograph;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.EnumSource.Mode;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.params.shadow.com.univocity.parsers.annotations.BooleanString;

public class ModelSerializationTest {

    public static final String VERIFICATION_STATUS_FIELD = "verificationStatus";

    @Test
    void shouldSerializeDeserializeWithoutLossOfInformation() throws JsonProcessingException {
        var randomUri = URI.create("https://random.uri");
        var languageMap = Map.of("en", "English text");
        var contributor = new Contributor.Builder()
                              .withIdentity(new Identity(randomUri, "Random name", "Personal",
                                                         "myOrcid"))
                              .withRole(new Role("Creator"))
                              .withSequence(10)
                              .withAffiliations(List.of(new UnconfirmedOrganization("Test")))
                              .build();

        var bookContext = new Book(new UnconfirmedSeries("title", "issn", "onlineIssn"),
                                   "123", new UnconfirmedPublisher("name"),
                                   List.of("isbn"));

        var academicMonograph = new AcademicMonograph(new Range("1", "10"));

        var reference = new Reference.Builder()
                            .withDoi(randomUri)
                            .withPublicationContext(bookContext)
                            .withPublicationInstance(academicMonograph)
                            .build();

        var entityDescription = new EntityDescription.Builder()
                                    .withMainTitle("Main title")
                                    .withDescription("Description")
                                    .withLanguage(randomUri)
                                    .withContributors(List.of(contributor))
                                    .withAlternativeTitles(languageMap)
                                    .withAlternativeAbstracts(languageMap)
                                    .withMetadataSource(randomUri)
                                    .withTags(List.of("tag"))
                                    .withPublicationDate(new PublicationDate("2024", "07", "17"))
                                    .withReference(reference)
                                    .build();
        var createPublicationRequest = new CreatePublicationRequest.Builder()
                                           .withEntityDescription(entityDescription)
                                           .build();

        var json = dtoObjectMapper.writeValueAsString(createPublicationRequest);

        var deserialized = dtoObjectMapper.readValue(json, CreatePublicationRequest.class);

        assertThat(deserialized, is(equalTo(createPublicationRequest)));
    }

    @ParameterizedTest
    @EnumSource(value = VerificationStatus.class, mode = Mode.INCLUDE)
    void shouldSerializeVerificationStatusCorrectly(VerificationStatus verificationStatus)
        throws JsonProcessingException {
        var identity = new Identity();
        identity.setVerificationStatus(verificationStatus);

        var json = dtoObjectMapper.writeValueAsString(identity);

        assertEquals(verificationStatus.getValue(),
            dtoObjectMapper.readTree(json).get(VERIFICATION_STATUS_FIELD).textValue());
    }

    @ParameterizedTest()
    @ValueSource(booleans = {true, false})
    void shouldCreateVerificationStatusFromBoolean(boolean input) {
        var result = VerificationStatus.fromBoolean(input);
        var expected = input ? VERIFIED : NOT_VERIFIED;

        assertEquals(expected, result);
    }
}
