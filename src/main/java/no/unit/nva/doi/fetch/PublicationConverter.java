package no.unit.nva.doi.fetch;

import com.fasterxml.jackson.databind.JsonNode;
import no.unit.nva.doi.fetch.model.PublicationDate;
import no.unit.nva.doi.fetch.model.Summary;

import java.util.Optional;
import java.util.UUID;

public class PublicationConverter {

    /**
     * Convert Publication JSON to Summary.
     *
     * @param json  json
     * @param identifier    identifier
     * @return  summary
     */
    public Summary toSummary(JsonNode json, UUID identifier) {
        return new Summary.Builder()
                .withIdentifier(identifier)
                .withCreatorName(Optional.ofNullable(json.at("/entityDescription/contributors/0/name").textValue())
                        .orElse(null))
                .withTitle(Optional.ofNullable(json.at("/entityDescription/titles/").textValue()).orElse(null))
                .withDate(new PublicationDate.Builder()
                        .withYear(Optional.ofNullable(json.at("/entityDescription/date/year").textValue())
                                .orElse(null))
                        .withMonth(Optional.ofNullable(json.at("/entityDescription/date/month").textValue())
                                .orElse(null))
                        .withDay(Optional.ofNullable(json.at("/entityDescription/date/day").textValue())
                                .orElse(null))
                        .build())
                .build();
    }


}
