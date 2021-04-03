package no.unit.nva.metadata.service.testdata;

import no.unit.nva.metadata.type.Citation;
import no.unit.nva.model.contexttypes.Book;
import no.unit.nva.model.contexttypes.Journal;
import no.unit.nva.model.instancetypes.book.BookMonograph;
import no.unit.nva.model.instancetypes.journal.JournalArticle;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.ArgumentsProvider;

import java.util.stream.Stream;

public class TypeInformationArgumentsProvider implements ArgumentsProvider {
    @Override
    public Stream<? extends Arguments> provideArguments(ExtensionContext context) throws Exception {
        return Stream.of(Arguments.of(Citation.ISBN.getMetaTagName(), "9783110646610", Book.class, BookMonograph.class),
                Arguments.of(Citation.ISSN.getMetaTagName(), "0886-4780", Journal.class, JournalArticle.class));
    }
}
