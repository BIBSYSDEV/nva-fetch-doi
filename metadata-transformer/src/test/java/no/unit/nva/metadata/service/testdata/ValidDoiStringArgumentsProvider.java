package no.unit.nva.metadata.service.testdata;

import no.unit.nva.metadata.type.Citation;
import no.unit.nva.metadata.type.DcTerms;
import no.unit.nva.metadata.type.RawMetaTag;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.ArgumentsProvider;

import java.net.URI;
import java.util.stream.Stream;

public class ValidDoiStringArgumentsProvider implements ArgumentsProvider {
    private static final String DC_IDENTIFIER = DcTerms.IDENTIFIER.getMetaTagName();
    public static final String CITATION_DOI = Citation.DOI.getMetaTagName();
    private static final String DOI = RawMetaTag.DOI.getMetaTagName();

    @Override
    public Stream<? extends Arguments> provideArguments(ExtensionContext context) {
        return Stream.of(
                Arguments.of(DC_IDENTIFIER, "10.1109/5.771073", URI.create("https://doi.org/10.1109/5.771073")),
                Arguments.of(CITATION_DOI, "10.1109/5.771073", URI.create("https://doi.org/10.1109/5.771073")),
                Arguments.of(DOI, "10.1109/5.771073", URI.create("https://doi.org/10.1109/5.771073"))
        );
    }
}
