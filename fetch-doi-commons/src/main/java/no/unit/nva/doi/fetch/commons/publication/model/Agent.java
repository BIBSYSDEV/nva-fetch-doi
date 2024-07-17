package no.unit.nva.doi.fetch.commons.publication.model;

import com.fasterxml.jackson.annotation.JsonSubTypes;

@JsonSubTypes({
    @JsonSubTypes.Type(name = "UnconfirmedOrganization", value = UnconfirmedOrganization.class)
})
public interface Agent {

}
