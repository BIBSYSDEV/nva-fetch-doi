package no.unit.nva.metadata.service.testdata;

import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.ArgumentsProvider;

import java.util.List;
import java.util.stream.Stream;

public class ContributorArgumentsSource implements ArgumentsProvider {
    public static final String DC_CONTRIBUTOR = "DC.contributor";
    public static final String DC_CREATOR = "DC.creator";
    public static final String CITATION_AUTHOR = "citation_author";

    @Override
    public Stream<? extends Arguments> provideArguments(ExtensionContext context) {
        return Stream.of(
                Arguments.of(List.of(new MetaTagPair(DC_CONTRIBUTOR, "Broderson, Hille"))),
                Arguments.of(List.of(new MetaTagPair(DC_CONTRIBUTOR, "Hilleson, Kitve"),
                        new MetaTagPair(DC_CONTRIBUTOR, "Mäinbrat, Bergtraut"))),
                Arguments.of(List.of(new MetaTagPair(DC_CREATOR, "Broderson, Hille"))),
                Arguments.of(List.of(new MetaTagPair(DC_CREATOR, "Hilleson, Kitve"),
                        new MetaTagPair(DC_CREATOR, "Mäinbrat, Bergtraut"))),
                Arguments.of(List.of(new MetaTagPair(CITATION_AUTHOR, "Hilleson, Kitve"),
                        new MetaTagPair(CITATION_AUTHOR, "Mäinbrat, Bergtraut"))),
                Arguments.of(List.of(new MetaTagPair(DC_CREATOR, "Hilleson, Kitve"),
                        new MetaTagPair(DC_CREATOR, "Mäinbrat, Bergtraut"),
                        new MetaTagPair(CITATION_AUTHOR, "Hilleson, Kitve"),
                        new MetaTagPair(CITATION_AUTHOR, "Mäinbrat, Bergtraut")))
        );
    }
}

