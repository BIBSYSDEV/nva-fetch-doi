package no.unit.nva.metadata.service.testdata;

import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.ArgumentsProvider;

import java.util.stream.Stream;

public class DcContentCaseArgumentsProvider implements ArgumentsProvider {
    @Override
    public Stream<? extends Arguments> provideArguments(ExtensionContext context) {
        return Stream.of(
                Arguments.of("dc.language", "de"),
                Arguments.of("DC.language", "de"),
                Arguments.of("Dc.language", "de"),
                Arguments.of("dC.language", "de"),
                Arguments.of("dc.Language", "de"),
                Arguments.of("DCTERMS.language", "de"),
                Arguments.of("dcterms.language", "de"),
                Arguments.of("DcTerms.language", "de"),
                Arguments.of("DcTERMS.language", "de"),
                Arguments.of("dcTerms.language", "de"),
                Arguments.of("dcterms.Language", "de"),
                Arguments.of("dcterms.LANGUAGE", "de")
        );
    }
}
