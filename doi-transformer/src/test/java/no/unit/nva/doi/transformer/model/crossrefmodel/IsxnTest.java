package no.unit.nva.doi.transformer.model.crossrefmodel;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.StringContains.containsString;
import static org.junit.jupiter.api.Assertions.assertThrows;
import no.unit.nva.doi.transformer.model.crossrefmodel.Isxn.IsxnType;
import org.junit.jupiter.api.Test;

class IsxnTest {

    @Test
    public void getTypeThrowsExceptionWhenInputIsInvalid() {
        String someString = "randomString";
        RuntimeException exception = assertThrows(RuntimeException.class,
                                                          () -> IsxnType.getType(someString));
        assertThat(exception.getMessage(), containsString(someString));
    }
}