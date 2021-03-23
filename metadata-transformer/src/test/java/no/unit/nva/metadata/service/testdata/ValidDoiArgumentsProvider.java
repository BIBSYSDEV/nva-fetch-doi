package no.unit.nva.metadata.service.testdata;

import no.unit.nva.metadata.Citation;
import no.unit.nva.metadata.DcTerms;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.ArgumentsProvider;

import java.net.URI;
import java.util.stream.Stream;

public class ValidDoiArgumentsProvider implements ArgumentsProvider {

    @Override
    public Stream<? extends Arguments> provideArguments(ExtensionContext context) {
        return Stream.of(
                Arguments.of(DcTerms.IDENTIFIER.getDcLocalName(), "https://doi.org/10.1109/5.771073",
                        URI.create("https://doi.org/10.1109/5.771073")),
                Arguments.of(Citation.DOI.getProperty(), "https://doi.org/10.1109/5.771073",
                        URI.create("https://doi.org/10.1109/5.771073")),
                Arguments.of(DcTerms.IDENTIFIER.getDcLocalName(), "http://doi.org/10.1109/5.771073",
                        URI.create("https://doi.org/10.1109/5.771073")),
                Arguments.of(Citation.DOI.getProperty(), "http://doi.org/10.1109/5.771073",
                        URI.create("https://doi.org/10.1109/5.771073")),
                Arguments.of(DcTerms.IDENTIFIER.getDcLocalName(), "https://dx.doi.org/10.1109/5.771073",
                        URI.create("https://doi.org/10.1109/5.771073")),
                Arguments.of(Citation.DOI.getProperty(), "https://dx.doi.org/10.1109/5.771073",
                        URI.create("https://doi.org/10.1109/5.771073")),
                Arguments.of(DcTerms.IDENTIFIER.getDcLocalName(), "http://dx.doi.org/10.1109/5.771073",
                        URI.create("https://doi.org/10.1109/5.771073")),
                Arguments.of(Citation.DOI.getProperty(), "http://dx.doi.org/10.1109/5.771073",
                        URI.create("https://doi.org/10.1109/5.771073")),
                Arguments.of(DcTerms.IDENTIFIER.getDcLocalName(), "10.1109/5.771073",
                        URI.create("https://doi.org/10.1109/5.771073")),
                Arguments.of(Citation.DOI.getProperty(), "10.1109/5.771073",
                        URI.create("https://doi.org/10.1109/5.771073")),
                Arguments.of(DcTerms.IDENTIFIER.getDcLocalName(), "doi:10.1109/5.771073",
                        URI.create("https://doi.org/10.1109/5.771073")),
                Arguments.of(Citation.DOI.getProperty(), "doi:10.1109/5.771073",
                        URI.create("https://doi.org/10.1109/5.771073")),
                Arguments.of(DcTerms.IDENTIFIER.getDcLocalName(), "doc:10.1109/5.771073",
                        URI.create("https://doi.org/10.1109/5.771073")),
                Arguments.of(Citation.DOI.getProperty(), "doc:10.1109/5.771073",
                        URI.create("https://doi.org/10.1109/5.771073")),
                Arguments.of(DcTerms.IDENTIFIER.getDcLocalName(), "DOI:10.1109/5.771073",
                        URI.create("https://doi.org/10.1109/5.771073")),
                Arguments.of(Citation.DOI.getProperty(), "DOI:10.1109/5.771073",
                        URI.create("https://doi.org/10.1109/5.771073")),
                Arguments.of(DcTerms.IDENTIFIER.getDcLocalName(), "DOC:10.1109/5.771073",
                        URI.create("https://doi.org/10.1109/5.771073")),
                Arguments.of(Citation.DOI.getProperty(), "DOC:10.1109/5.771073",
                        URI.create("https://doi.org/10.1109/5.771073")),
                Arguments.of(DcTerms.IDENTIFIER.getDcLocalName(), "https://doi.org/bwfc",
                        URI.create("https://doi.org/10.7774/CEVR.2016.5.1.1")),
                Arguments.of(Citation.DOI.getProperty(), "https://doi.org/bwfc",
                        URI.create("https://doi.org/10.7774/CEVR.2016.5.1.1")),
                Arguments.of(DcTerms.IDENTIFIER.getDcLocalName(), "https://doi.org/bwfc/",
                        URI.create("https://doi.org/10.7774/CEVR.2016.5.1.1")),
                Arguments.of(Citation.DOI.getProperty(), "https://doi.org/bwfc/",
                        URI.create("https://doi.org/10.7774/CEVR.2016.5.1.1")),
                Arguments.of(DcTerms.IDENTIFIER.getDcLocalName(), "http://doi.org/bwfc",
                        URI.create("https://doi.org/10.7774/CEVR.2016.5.1.1")),
                Arguments.of(Citation.DOI.getProperty(), "http://doi.org/bwfc",
                        URI.create("https://doi.org/10.7774/CEVR.2016.5.1.1")),
                Arguments.of(DcTerms.IDENTIFIER.getDcLocalName(), "http://doi.org/bwfc/",
                        URI.create("https://doi.org/10.7774/CEVR.2016.5.1.1")),
                Arguments.of(Citation.DOI.getProperty(), "http://doi.org/bwfc/",
                        URI.create("https://doi.org/10.7774/CEVR.2016.5.1.1"))
        );
    }
}
