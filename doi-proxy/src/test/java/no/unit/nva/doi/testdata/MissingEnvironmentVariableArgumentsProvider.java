package no.unit.nva.doi.testdata;

import no.unit.nva.doi.CrossRefClient;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.ArgumentsProvider;

import java.util.List;
import java.util.stream.Stream;

import static java.util.Collections.emptyList;
import static no.unit.nva.doi.CrossRefClient.CROSSREFPLUSAPITOKEN_KEY_ENV;
import static no.unit.nva.doi.CrossRefClient.CROSSREFPLUSAPITOKEN_NAME_ENV;

public class MissingEnvironmentVariableArgumentsProvider implements ArgumentsProvider {
    @Override
    public Stream<? extends Arguments> provideArguments(ExtensionContext context) throws Exception {
        return Stream.of(
                Arguments.of(RuntimeException.class, List.of(CROSSREFPLUSAPITOKEN_NAME_ENV,
                        CROSSREFPLUSAPITOKEN_KEY_ENV), emptyList()),
                Arguments.of(RuntimeException.class, List.of(CROSSREFPLUSAPITOKEN_NAME_ENV),
                        List.of(CROSSREFPLUSAPITOKEN_KEY_ENV)),
                Arguments.of(RuntimeException.class, List.of(CROSSREFPLUSAPITOKEN_KEY_ENV),
                        List.of(CROSSREFPLUSAPITOKEN_NAME_ENV))
        );
    }
}
