package no.unit.nva.doi.transformer.model.datacitemodel;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import java.io.IOException;
import java.nio.file.Path;
import no.sikt.nva.doi.fetch.jsonconfig.Json;
import nva.commons.core.ioutils.IoUtils;
import org.junit.jupiter.api.Test;

public class DataciteResponseTest {

    private static final Path DATACITE_RESPONSE = Path.of("datacite_response.json");

    @Test
    void testSettersAndGetters() throws IOException {

        DataciteResponse dataciteResponse = Json.readValue(
            IoUtils.stringFromResources(DATACITE_RESPONSE), DataciteResponse.class);

        assertNotNull(dataciteResponse);
    }
}
