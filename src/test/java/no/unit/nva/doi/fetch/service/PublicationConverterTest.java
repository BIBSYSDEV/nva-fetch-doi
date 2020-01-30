package no.unit.nva.doi.fetch.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import no.unit.nva.doi.fetch.MainHandler;
import no.unit.nva.doi.fetch.model.Summary;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

public class PublicationConverterTest {

    private ObjectMapper objectMapper = MainHandler.createObjectMapper();

    @Test
    public void test() throws IOException {
        JsonNode json = objectMapper.readTree(new File("src/test/resources/example_publication.json"));
        PublicationConverter converter = new PublicationConverter();

        Summary summary = converter.toSummary(json, UUID.randomUUID());

        String summaryJson = objectMapper.writeValueAsString(summary);

        System.out.println(summaryJson);
    }

}
