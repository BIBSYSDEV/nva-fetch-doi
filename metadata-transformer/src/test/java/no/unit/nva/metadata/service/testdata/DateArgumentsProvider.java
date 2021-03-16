package no.unit.nva.metadata.service.testdata;

import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.ArgumentsProvider;

import java.util.stream.Stream;

public class DateArgumentsProvider implements ArgumentsProvider {

    @Override
    public Stream<? extends Arguments> provideArguments(ExtensionContext context) {
        return Stream.of(
                Arguments.of("DC.date", "2001"),
                Arguments.of("DC.date", "2001-01-01"),
                Arguments.of("dc.date", "2001"),
                Arguments.of("dc.date", "2001-01-01"),
                Arguments.of("DCTERMS.date", "2001"),
                Arguments.of("DCTERMS.date", "2001-01-01"),
                Arguments.of("dcterms.date", "2001"),
                Arguments.of("dcterms.date", "2001-01-01"),
                Arguments.of("citation_publication_date", "2001"),
                Arguments.of("citation_publication_date", "2001-01-01"),
                Arguments.of("citation_cover_date", "2001"),
                Arguments.of("citation_cover_date", "2001-01-01"),
                Arguments.of("citation_date", "2001"),
                Arguments.of("citation_date", "2001-01-01")
        );
    }
}
