package no.unit.nva.doi.fetch.commons.publication.model;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import no.unit.nva.doi.fetch.commons.publication.model.contexttypes.Book;
import no.unit.nva.doi.fetch.commons.publication.model.contexttypes.UnconfirmedJournal;

@JsonSubTypes({
    @JsonSubTypes.Type(name = "UnconfirmedJournal", value = UnconfirmedJournal.class),
    @JsonSubTypes.Type(name = "Book", value = Book.class)
})
public interface PublicationContext {

}
