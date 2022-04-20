package no.unit.nva.doi.fetch.model;

import com.fasterxml.jackson.core.JsonProcessingException;
import no.sikt.nva.doi.fetch.jsonconfig.Json;
import no.unit.nva.identifiers.SortableIdentifier;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class SummaryTest {

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

        Summary mappedSummary = Json.readValue(Json.writeValueAsString(summary), Summary.class);

        Assertions.assertNotNull(mappedSummary);
    }
}
