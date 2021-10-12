package no.unit.nva.doi.transformer.model.datacitemodel;

import static no.unit.nva.doi.transformer.DoiTransformerConfig.objectMapper;
import static no.unit.nva.hamcrest.DoesNotHaveNullOrEmptyFields.doesNotHaveNullOrEmptyFields;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.nio.file.Path;
import nva.commons.core.ioutils.IoUtils;
import nva.commons.core.JsonUtils;
import org.junit.jupiter.api.Test;

public class DataciteResponseTest {

    private static final Path DATACITE_RESPONSE = Path.of("datacite_response.json");

    @Test
    void testSettersAndGetters() throws IOException {

        DataciteResponse dataciteResponse = objectMapper.readValue(
            IoUtils.stringFromResources(DATACITE_RESPONSE), DataciteResponse.class);

        assertNotNull(dataciteResponse);
        assertThat(dataciteResponse, doesNotHaveNullOrEmptyFields());
    }
}
