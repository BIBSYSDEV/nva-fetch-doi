package no.unit.nva.metadata.service.testdata;

import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.ArgumentsProvider;

import java.net.URI;
import java.util.stream.Stream;

public class UndefinedLanguageArgumentsProvider implements ArgumentsProvider {
    private static final URI LEXVO_UND = URI.create("https://lexvo.org/id/iso639-3/und");

    @Override
    public Stream<? extends Arguments> provideArguments(ExtensionContext context) {
        return Stream.of(
                Arguments.of("DC.language", null, LEXVO_UND),
                Arguments.of("DCTERMS.language", null, LEXVO_UND),
                Arguments.of("DC.language", "Welsh", LEXVO_UND),
                Arguments.of("DC.language", "", LEXVO_UND),
                Arguments.of("DC.language", "A long description DE", LEXVO_UND)
        );
    }
}
