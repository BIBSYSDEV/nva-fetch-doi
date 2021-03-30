package no.unit.nva.metadata.service.testdata;

import org.eclipse.rdf4j.model.vocabulary.DC;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.ArgumentsProvider;

import java.util.stream.Stream;

public class MetaTagTitleProvider implements ArgumentsProvider {
    @Override
    public Stream<? extends Arguments> provideArguments(ExtensionContext context) throws Exception {
        return Stream.of(
                Arguments.of("dcterms.title", "A fluffy duck"),
                Arguments.of("dc.title", "Ate cheese and drank wine"),
                Arguments.of("citation_title", "While fishermen hauled their catch")
        );
    }
}
