package no.unit.nva.doi.transformer.model.crossrefmodel;

import static no.unit.nva.hamcrest.DoesNotHaveNullOrEmptyFields.doesNotHaveNullOrEmptyFields;
import static org.hamcrest.MatcherAssert.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.nio.file.Path;
import nva.commons.utils.IoUtils;
import nva.commons.utils.JsonUtils;
import org.junit.jupiter.api.Test;

public class CrossrefDocumentTest {

    private final ObjectMapper objectMapper = JsonUtils.objectMapper;
    private final static Path CROSSREF_RESOURCE= Path.of("crossref.json");


    @Test
    public void testSettersAndGetters() throws IOException {

        String resourceString =IoUtils.stringFromResources(CROSSREF_RESOURCE);
        CrossrefApiResponse crossRefResponse = objectMapper.readValue(resourceString, CrossrefApiResponse.class);

        assertThat(crossRefResponse,doesNotHaveNullOrEmptyFields());


    }

}
