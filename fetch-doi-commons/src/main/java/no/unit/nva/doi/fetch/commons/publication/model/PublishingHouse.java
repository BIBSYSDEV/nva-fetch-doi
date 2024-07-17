package no.unit.nva.doi.fetch.commons.publication.model;

import com.fasterxml.jackson.annotation.JsonSubTypes;

@JsonSubTypes({
    @JsonSubTypes.Type(name = "UnconfirmedPublisher", value = UnconfirmedPublisher.class)
})
public interface PublishingHouse {

}
