package no.unit.nva.doi.fetch.commons.publication.model;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeInfo.Id;
import no.unit.nva.doi.fetch.commons.publication.model.instancetypes.AcademicArticle;
import no.unit.nva.doi.fetch.commons.publication.model.instancetypes.AcademicChapter;
import no.unit.nva.doi.fetch.commons.publication.model.instancetypes.AcademicMonograph;
import no.unit.nva.doi.fetch.commons.publication.model.instancetypes.BookAnthology;
import no.unit.nva.doi.fetch.commons.publication.model.instancetypes.BookMonograph;

@JsonTypeInfo(use = Id.NAME, property = "type")
@JsonSubTypes({
    @JsonSubTypes.Type(name = "AcademicArticle", value = AcademicArticle.class),
    @JsonSubTypes.Type(name = "AcademicMonograph", value = AcademicMonograph.class),
    @JsonSubTypes.Type(name = "BookMonograph", value = BookMonograph.class),
    @JsonSubTypes.Type(name = "AcademicChapter", value = AcademicChapter.class),
    @JsonSubTypes.Type(name = "BookAnthology", value = BookAnthology.class)
})
public interface PublicationInstance extends WithPages {

}
