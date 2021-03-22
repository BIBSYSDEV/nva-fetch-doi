package no.unit.nva.metadata.service.testdata;

import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.ArgumentsProvider;

import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

public class ValidDoiArgumentsProvider implements ArgumentsProvider {

    @Override
    public Stream<? extends Arguments> provideArguments(ExtensionContext context) {
        return Stream.of(
                Arguments.of("https://doi.org/10.1109/5.771073", URI.create("https://doi.org/10.1109/5.771073")),
                Arguments.of("http://doi.org/10.1109/5.771073", URI.create("https://doi.org/10.1109/5.771073")),
                Arguments.of("https://dx.doi.org/10.1109/5.771073", URI.create("https://doi.org/10.1109/5.771073")),
                Arguments.of("http://dx.doi.org/10.1109/5.771073", URI.create("https://doi.org/10.1109/5.771073")),
                Arguments.of("10.1109/5.771073", URI.create("https://doi.org/10.1109/5.771073")),
                Arguments.of("doi:10.1109/5.771073", URI.create("https://doi.org/10.1109/5.771073")),
                Arguments.of("doc:10.1109/5.771073", URI.create("https://doi.org/10.1109/5.771073")),
                Arguments.of("DOI:10.1109/5.771073", URI.create("https://doi.org/10.1109/5.771073")),
                Arguments.of("DOC:10.1109/5.771073", URI.create("https://doi.org/10.1109/5.771073")),
                Arguments.of("https://doi.org/bwfc", URI.create("https://doi.org/10.7774/CEVR.2016.5.1.1")),
                Arguments.of("https://doi.org/bwfc/", URI.create("https://doi.org/10.7774/CEVR.2016.5.1.1")),
                Arguments.of("http://doi.org/bwfc", URI.create("https://doi.org/10.7774/CEVR.2016.5.1.1")),
                Arguments.of("http://doi.org/bwfc/", URI.create("https://doi.org/10.7774/CEVR.2016.5.1.1"))
        );
    }
}
