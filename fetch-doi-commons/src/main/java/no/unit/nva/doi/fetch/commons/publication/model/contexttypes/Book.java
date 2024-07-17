package no.unit.nva.doi.fetch.commons.publication.model.contexttypes;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeInfo.Id;
import java.util.List;
import no.unit.nva.doi.fetch.commons.publication.model.PublicationContext;
import no.unit.nva.doi.fetch.commons.publication.model.PublishingHouse;
import nva.commons.core.JacocoGenerated;

@JacocoGenerated
@JsonTypeInfo(use = Id.NAME, property = "type")
public record Book(BookSeries series, String seriesNumber, PublishingHouse publisher, List<String> isbnList)
    implements PublicationContext {

}
