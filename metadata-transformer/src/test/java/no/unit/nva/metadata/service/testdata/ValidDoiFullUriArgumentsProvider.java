package no.unit.nva.metadata.service.testdata;

import no.unit.nva.metadata.Citation;
import no.unit.nva.metadata.DcTerms;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.ArgumentsProvider;

import java.net.URI;
import java.util.stream.Stream;

public class ValidDoiFullUriArgumentsProvider implements ArgumentsProvider {

    private static final String DC_IDENTIFIER = DcTerms.IDENTIFIER.getMetaTagName();
    private static final String CITATION_DOI = Citation.DOI.getMetaTagName();

    @Override
    public Stream<? extends Arguments> provideArguments(ExtensionContext context) {
        return Stream.of(
                Arguments.of(DC_IDENTIFIER, "https://doi.org/10.1109/5.771073",
                URI.create("https://doi.org/10.1109/5.771073")),
                Arguments.of(CITATION_DOI, "https://doi.org/10.1109/5.771073",
                        URI.create("https://doi.org/10.1109/5.771073")),
                Arguments.of(DC_IDENTIFIER, "http://doi.org/10.1109/5.771073",
                        URI.create("https://doi.org/10.1109/5.771073")),
                Arguments.of(CITATION_DOI, "http://doi.org/10.1109/5.771073",
                        URI.create("https://doi.org/10.1109/5.771073")),
                Arguments.of(DC_IDENTIFIER, "https://dx.doi.org/10.1109/5.771073",
                        URI.create("https://doi.org/10.1109/5.771073")),
                Arguments.of(CITATION_DOI, "https://dx.doi.org/10.1109/5.771073",
                        URI.create("https://doi.org/10.1109/5.771073")),
                Arguments.of(DC_IDENTIFIER, "http://dx.doi.org/10.1109/5.771073",
                        URI.create("https://doi.org/10.1109/5.771073")),
                Arguments.of(CITATION_DOI, "http://dx.doi.org/10.1109/5.771073",
                        URI.create("https://doi.org/10.1109/5.771073"))
                );
    }
}
