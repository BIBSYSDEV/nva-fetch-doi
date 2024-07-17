package no.unit.nva.doi.fetch.commons.publication.model.instancetypes;

import no.unit.nva.doi.fetch.commons.publication.model.MonographPages;
import no.unit.nva.doi.fetch.commons.publication.model.PublicationInstance;
import nva.commons.core.JacocoGenerated;

@JacocoGenerated
public record BookMonograph(String contentType, MonographPages pages) implements PublicationInstance {

}
