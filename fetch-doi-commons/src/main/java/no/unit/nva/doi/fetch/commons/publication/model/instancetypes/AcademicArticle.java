package no.unit.nva.doi.fetch.commons.publication.model.instancetypes;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeInfo.Id;
import no.unit.nva.doi.fetch.commons.publication.model.Pages;
import no.unit.nva.doi.fetch.commons.publication.model.PublicationInstance;
import nva.commons.core.JacocoGenerated;

@JacocoGenerated
@JsonTypeInfo(use = Id.NAME, property = "type")
public record AcademicArticle(Pages pages, String volume, String issue)
    implements PublicationInstance {

}
