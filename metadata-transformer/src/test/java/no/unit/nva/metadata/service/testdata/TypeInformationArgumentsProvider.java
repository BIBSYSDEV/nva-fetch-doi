package no.unit.nva.metadata.service.testdata;

import java.util.stream.Stream;
import no.unit.nva.doi.fetch.commons.publication.model.contexttypes.Book;
import no.unit.nva.doi.fetch.commons.publication.model.contexttypes.UnconfirmedJournal;
import no.unit.nva.doi.fetch.commons.publication.model.instancetypes.AcademicArticle;
import no.unit.nva.doi.fetch.commons.publication.model.instancetypes.AcademicMonograph;
import no.unit.nva.metadata.type.Citation;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.ArgumentsProvider;

public class TypeInformationArgumentsProvider implements ArgumentsProvider {

    public static final String VALID_ISBN_INDICATING_BOOK = "9783110646610";
    public static final String VALID_ISSN_INDICATING_JOURNAL = "0886-4780";

    @Override
    public Stream<? extends Arguments> provideArguments(ExtensionContext context) throws Exception {
        return Stream.of(
                Arguments.of(
                        Citation.ISBN.getMetaTagName(),
                        VALID_ISBN_INDICATING_BOOK,
                        Book.class,
                        AcademicMonograph.class
                ),
                Arguments.of(
                        Citation.ISSN.getMetaTagName(),
                        VALID_ISSN_INDICATING_JOURNAL,
                        UnconfirmedJournal.class,
                        AcademicArticle.class
                )
        );
    }
}
