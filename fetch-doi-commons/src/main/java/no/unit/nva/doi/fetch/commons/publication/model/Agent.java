package no.unit.nva.doi.fetch.commons.publication.model;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes({
    @JsonSubTypes.Type(name = "UnconfirmedOrganization", value = UnconfirmedOrganization.class)
})
public interface Agent {

}
