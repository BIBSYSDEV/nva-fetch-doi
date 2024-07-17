package no.unit.nva.doi.fetch.commons.publication.model;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import no.unit.nva.doi.fetch.commons.publication.model.instancetypes.AcademicArticle;
import no.unit.nva.doi.fetch.commons.publication.model.instancetypes.AcademicMonograph;
import no.unit.nva.doi.fetch.commons.publication.model.instancetypes.BookMonograph;

@JsonSubTypes({
    @JsonSubTypes.Type(name = "AcademicArticle", value = AcademicArticle.class),
    @JsonSubTypes.Type(name = "AcademicMonograph", value = AcademicMonograph.class),
    @JsonSubTypes.Type(name = "BookMonograph", value = BookMonograph.class)
})
public interface PublicationInstance extends WithPages {

}
