package no.unit.nva.doi.fetch.commons.publication.model.contexttypes;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeInfo.Id;
import java.net.URI;
import no.unit.nva.doi.fetch.commons.publication.model.PublicationContext;
import nva.commons.core.JacocoGenerated;

@JacocoGenerated
@JsonTypeInfo(use = Id.NAME, property = "type")
public record Anthology(URI id) implements PublicationContext {

}
