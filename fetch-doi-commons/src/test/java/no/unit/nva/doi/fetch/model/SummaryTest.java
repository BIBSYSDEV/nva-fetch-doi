package no.unit.nva.doi.fetch.model;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.UUID;
import no.unit.nva.identifiers.SortableIdentifier;
import nva.commons.core.JsonUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class SummaryTest {

    private ObjectMapper objectMapper = JsonUtils.objectMapper;

    @Test
    public void canMapSummary() throws JsonProcessingException {
        PublicationDate date = new PublicationDate.Builder()
            .withDay("08")
            .withMonth("06")
            .withYear("2020")
            .build();

        Summary summary = new Summary.Builder()
            .withCreatorName("creator name")
            .withIdentifier(SortableIdentifier.next())
            .withTitle("title")
            .withDate(date)
            .build();

        Summary mappedSummary = objectMapper.readValue(objectMapper.writeValueAsString(summary), Summary.class);

        Assertions.assertNotNull(mappedSummary);
    }
}
