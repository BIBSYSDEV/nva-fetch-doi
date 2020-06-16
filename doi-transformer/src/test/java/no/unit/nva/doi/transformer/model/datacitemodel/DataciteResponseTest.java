package no.unit.nva.doi.transformer.model.datacitemodel;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.File;
import java.io.IOException;
import nva.commons.utils.JsonUtils;
import org.junit.jupiter.api.Test;

public class DataciteResponseTest {

    private ObjectMapper objectMapper = JsonUtils.objectMapper;

    @Test
    public void test() throws IOException {

        DataciteResponse dataciteResponse = objectMapper.readValue(
            new File("src/test/resources/datacite_response.json"), DataciteResponse.class);

        assertNotNull(dataciteResponse);
    }
}
