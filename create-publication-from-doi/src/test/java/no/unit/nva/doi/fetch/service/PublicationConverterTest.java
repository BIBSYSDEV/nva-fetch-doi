package no.unit.nva.doi.fetch.service;

import static no.unit.nva.doi.fetch.RestApiConfig.objectMapper;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.fasterxml.jackson.databind.JsonNode;
import java.io.IOException;
import java.nio.file.Path;
import no.unit.nva.doi.fetch.model.Summary;
import nva.commons.core.ioutils.IoUtils;
import org.junit.jupiter.api.Test;


public class PublicationConverterTest {

    public static final Path TEST_FILE = Path.of("example_publication.json");


    @Test
    public void publicationConverterReturnsSummaryForValidJsonObject() throws IOException {
        JsonNode json = objectMapper.readTree(IoUtils.inputStreamFromResources(TEST_FILE));
        PublicationConverter converter = new PublicationConverter();

        Summary summary = converter.toSummary(json);

        assertNotNull(summary.getIdentifier());
        assertNotNull(summary.getTitle());
    }
}
