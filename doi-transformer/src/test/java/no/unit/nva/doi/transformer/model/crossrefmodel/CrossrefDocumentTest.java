package no.unit.nva.doi.transformer.model.crossrefmodel;

import static no.unit.nva.hamcrest.DoesNotHaveNullOrEmptyFields.doesNotHaveNullOrEmptyFields;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.nio.file.Path;
import nva.commons.utils.IoUtils;
import nva.commons.utils.JsonUtils;
import org.junit.jupiter.api.Test;

public class CrossrefDocumentTest {

    private final ObjectMapper objectMapper = JsonUtils.objectMapper;
    private static final Path CROSSREF_RESOURCE = Path.of("crossref.json");

    @Test
    public void testSettersAndGetters() throws IOException {

        String resourceString = IoUtils.stringFromResources(CROSSREF_RESOURCE);
        CrossrefApiResponse crossRefResponse = objectMapper.readValue(resourceString, CrossrefApiResponse.class);
        String serializedObject = objectMapper.writeValueAsString(crossRefResponse);
        CrossrefApiResponse deserializedObject = objectMapper.readValue(serializedObject, CrossrefApiResponse.class);
        //cannot test deep equality on those classes. we compare therefore strings.
        String serializedAgain = objectMapper.writeValueAsString(deserializedObject);

        assertThat(crossRefResponse, doesNotHaveNullOrEmptyFields());
        assertThat(serializedAgain, is(equalTo(serializedObject)));
    }
}
