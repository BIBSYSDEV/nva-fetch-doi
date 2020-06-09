package no.unit.nva.doi.fetch.model;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.UUID;
import no.unit.nva.doi.fetch.ObjectMapperConfig;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class SummaryTest {

    private ObjectMapper objectMapper = ObjectMapperConfig.createObjectMapper();

    @Test
    public void canMapSummary() throws JsonProcessingException {
        PublicationDate date = new PublicationDate();
        date.setDay("08");
        date.setMonth("06");
        date.setYear("2020");

        Summary summary = new Summary();
        summary.setCreatorName("creator name");
        summary.setIdentifier(UUID.randomUUID());
        summary.setTitle("title");
        summary.setDate(date);

        Summary mappedSummary = objectMapper.readValue(objectMapper.writeValueAsString(summary), Summary.class);

        Assertions.assertNotNull(mappedSummary);
    }
}
