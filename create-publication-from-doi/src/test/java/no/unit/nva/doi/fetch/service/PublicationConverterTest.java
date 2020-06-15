package no.unit.nva.doi.fetch.service;

import static nva.commons.utils.JsonUtils.objectMapper;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.fasterxml.jackson.databind.JsonNode;
import java.io.File;
import java.io.IOException;
import no.unit.nva.doi.fetch.model.Summary;
import org.junit.jupiter.api.Test;


public class PublicationConverterTest {

    public static final String TEST_FILE = "src/test/resources/example_publication.json";


    @Test
    public void test() throws IOException {
        JsonNode json = objectMapper.readTree(new File(TEST_FILE));
        PublicationConverter converter = new PublicationConverter();

        Summary summary = converter.toSummary(json);

        assertNotNull(summary.getIdentifier());
        assertNotNull(summary.getTitle());
    }
}
