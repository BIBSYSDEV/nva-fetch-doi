package no.unit.nva.doi.transformer.model.external;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.File;
import java.io.IOException;
import no.unit.nva.doi.transformer.MainHandler;
import no.unit.nva.doi.transformer.model.internal.external.DataciteResponse;
import org.junit.Assert;
import org.junit.Test;

public class DataciteResponseTest {

    private ObjectMapper objectMapper = MainHandler.createObjectMapper();

    @Test
    public void test() throws IOException {

        DataciteResponse dataciteResponse = objectMapper.readValue(
            new File("src/test/resources/datacite_response.json"), DataciteResponse.class);

        Assert.assertNotNull(dataciteResponse);
    }
}
