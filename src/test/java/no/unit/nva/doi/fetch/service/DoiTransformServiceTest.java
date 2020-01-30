package no.unit.nva.doi.fetch.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import no.unit.nva.doi.fetch.MainHandler;
import org.junit.Ignore;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;

public class DoiTransformServiceTest {

    private ObjectMapper objectMapper = MainHandler.createObjectMapper();

    @Test
    @Ignore
    public void test() throws IOException, URISyntaxException {

        DoiTransformService doiTransformService = new DoiTransformService();

        JsonNode json = objectMapper.readTree(new File("src/test/resources/example_publication.json"));

        JsonNode transform = doiTransformService.transform(json, "https://api.dev.nva.aws.unit.no", "");
    }

}
