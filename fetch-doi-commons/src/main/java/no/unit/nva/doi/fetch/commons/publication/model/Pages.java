package no.unit.nva.doi.fetch.commons.publication.model;

import com.fasterxml.jackson.annotation.JsonSubTypes;

@JsonSubTypes({
    @JsonSubTypes.Type(name = "Range", value = Range.class),
    @JsonSubTypes.Type(names = "MonographPages", value = MonographPages.class)
})
public interface Pages {

}
