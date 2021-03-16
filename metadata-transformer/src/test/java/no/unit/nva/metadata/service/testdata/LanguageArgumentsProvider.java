package no.unit.nva.metadata.service.testdata;

import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.ArgumentsProvider;

import java.net.URI;
import java.util.stream.Stream;

public class LanguageArgumentsProvider implements ArgumentsProvider {

    public static final URI LEXVO_DEU = URI.create("https://lexvo.org/id/iso639-3/deu");
    public static final String DE = "de";
    public static final String DEU = "deu";

    @Override
    public Stream<? extends Arguments> provideArguments(ExtensionContext context) {
        return Stream.of(
                Arguments.of("DC.language", DE, LEXVO_DEU),
                Arguments.of("DC.language", DEU, LEXVO_DEU),
                Arguments.of("DCTERMS.language", DE, LEXVO_DEU),
                Arguments.of("citation_language", DE, LEXVO_DEU)
        );
    }
}
