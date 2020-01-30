package no.unit.nva.doi.fetch.service;

import com.fasterxml.jackson.databind.JsonNode;
import no.unit.nva.doi.fetch.model.PublicationDate;
import no.unit.nva.doi.fetch.model.Summary;

import java.util.Optional;
import java.util.UUID;

public class PublicationConverter {

    public static final String ENTITY_DESCRIPTION_CONTRIBUTORS_0_NAME = "/entityDescription/contributors/0/name";
    public static final String ENTITY_DESCRIPTION_TITLES = "/entityDescription/titles/";
    public static final String ENTITY_DESCRIPTION_DATE_YEAR = "/entityDescription/date/year";
    public static final String ENTITY_DESCRIPTION_DATE_MONTH = "/entityDescription/date/month";
    public static final String ENTITY_DESCRIPTION_DATE_DAY = "/entityDescription/date/day";

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
                .withCreatorName(Optional.ofNullable(json.at(ENTITY_DESCRIPTION_CONTRIBUTORS_0_NAME).textValue())
                        .orElse(null))
                .withTitle(Optional.ofNullable(json.at(ENTITY_DESCRIPTION_TITLES).textValue()).orElse(null))
                .withDate(new PublicationDate.Builder()
                        .withYear(Optional.ofNullable(json.at(ENTITY_DESCRIPTION_DATE_YEAR).textValue())
                                .orElse(null))
                        .withMonth(Optional.ofNullable(json.at(ENTITY_DESCRIPTION_DATE_MONTH).textValue())
                                .orElse(null))
                        .withDay(Optional.ofNullable(json.at(ENTITY_DESCRIPTION_DATE_DAY).textValue())
                                .orElse(null))
                        .build())
                .build();
    }


}
