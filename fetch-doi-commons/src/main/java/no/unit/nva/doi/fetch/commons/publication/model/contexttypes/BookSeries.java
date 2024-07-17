package no.unit.nva.doi.fetch.commons.publication.model.contexttypes;

import com.fasterxml.jackson.annotation.JsonSubTypes;

@JsonSubTypes({
    @JsonSubTypes.Type(name = "Series", value = Series.class),
    @JsonSubTypes.Type(name = "UnconfirmedSeries", value = UnconfirmedSeries.class)
})
public interface BookSeries {

}
