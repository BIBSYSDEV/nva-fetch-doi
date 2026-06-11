package no.unit.nva.metadata.service.testdata;

import java.util.stream.Stream;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.ArgumentsProvider;

public class MetaTagTitleProvider implements ArgumentsProvider {
  @Override
  public Stream<? extends Arguments> provideArguments(ExtensionContext context) throws Exception {
    return Stream.of(
        Arguments.of("dcterms.title", "A fluffy duck"),
        Arguments.of("dc.title", "Ate cheese and drank wine"),
        Arguments.of("citation_title", "While fishermen hauled their catch"));
  }
}
