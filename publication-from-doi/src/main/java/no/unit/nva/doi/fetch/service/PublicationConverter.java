package no.unit.nva.doi.fetch.service;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.Optional;
import no.unit.nva.doi.fetch.model.PublicationDate;
import no.unit.nva.doi.fetch.model.Summary;
import no.unit.nva.identifiers.SortableIdentifier;

public class PublicationConverter {

    public static final String IDENTIFIER = "/identifier";
    public static final String ENTITY_DESCRIPTION_CONTRIBUTORS_0_NAME = "/entityDescription/contributors/0/name";
    public static final String ENTITY_DESCRIPTION_MAIN_TITLE = "/entityDescription/mainTitle";
    public static final String ENTITY_DESCRIPTION_DATE_YEAR = "/entityDescription/date/year";
    public static final String ENTITY_DESCRIPTION_DATE_MONTH = "/entityDescription/date/month";
    public static final String ENTITY_DESCRIPTION_DATE_DAY = "/entityDescription/date/day";

    /**
     * Convert Publication JSON to Summary.
     *
     * @param json json
     * @return summary
     */
    public Summary toSummary(JsonNode json) {
        return new Summary.Builder().withIdentifier(newIdentifier(json))
                   .withCreatorName(
                       Optional.ofNullable(json.at(ENTITY_DESCRIPTION_CONTRIBUTORS_0_NAME).textValue())
                           .orElse(null)).withTitle(
                Optional.ofNullable(json.at(ENTITY_DESCRIPTION_MAIN_TITLE).textValue()).orElse(null)).withDate(
                new PublicationDate.Builder()
                    .withYear(Optional.ofNullable(json.at(ENTITY_DESCRIPTION_DATE_YEAR).textValue()).orElse(null))
                    .withMonth(Optional.ofNullable(json.at(ENTITY_DESCRIPTION_DATE_MONTH).textValue()).orElse(null))
                    .withDay(Optional.ofNullable(json.at(ENTITY_DESCRIPTION_DATE_DAY).textValue()).orElse(null))
                    .build()).build();
    }

    private SortableIdentifier newIdentifier(JsonNode node) {
        String identifierString = Optional.of(node)
                                      .map(json -> json.at(IDENTIFIER))
                                      .map(JsonNode::textValue)
                                      .orElse(SortableIdentifier.next().toString());

        return new SortableIdentifier(identifierString);
    }
}
