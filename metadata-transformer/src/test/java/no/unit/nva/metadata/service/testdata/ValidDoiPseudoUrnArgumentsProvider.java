package no.unit.nva.metadata.service.testdata;

import no.unit.nva.metadata.type.Citation;
import no.unit.nva.metadata.type.DcTerms;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.ArgumentsProvider;

import java.net.URI;
import java.util.stream.Stream;

public class ValidDoiPseudoUrnArgumentsProvider implements ArgumentsProvider {

    private static final String DC_IDENTIFIER = DcTerms.IDENTIFIER.getMetaTagName();
    private static final String CITATION_DOI = Citation.DOI.getMetaTagName();

    @Override
    public Stream<? extends Arguments> provideArguments(ExtensionContext context) {
        return Stream.of(
                Arguments.of(DC_IDENTIFIER, "doi:10.1109/5.771073", URI.create("https://doi.org/10.1109/5.771073")),
                Arguments.of(CITATION_DOI, "doi:10.1109/5.771073", URI.create("https://doi.org/10.1109/5.771073")),
                Arguments.of(DC_IDENTIFIER, "doc:10.1109/5.771073", URI.create("https://doi.org/10.1109/5.771073")),
                Arguments.of(CITATION_DOI, "doc:10.1109/5.771073", URI.create("https://doi.org/10.1109/5.771073")),
                Arguments.of(DC_IDENTIFIER, "DOI:10.1109/5.771073", URI.create("https://doi.org/10.1109/5.771073")),
                Arguments.of(CITATION_DOI, "DOI:10.1109/5.771073", URI.create("https://doi.org/10.1109/5.771073")),
                Arguments.of(DC_IDENTIFIER, "DOC:10.1109/5.771073", URI.create("https://doi.org/10.1109/5.771073")),
                Arguments.of(CITATION_DOI, "DOC:10.1109/5.771073", URI.create("https://doi.org/10.1109/5.771073"))
        );
    }
}
