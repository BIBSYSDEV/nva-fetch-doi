package no.unit.nva.doi.fetch;

import com.amazonaws.services.lambda.runtime.RequestStreamHandler;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

public class DependenciesTest {

    @Test
    public void allDependenciesAreAvailable() {
        Mockito.mock(RequestStreamHandler.class);

    }

}
