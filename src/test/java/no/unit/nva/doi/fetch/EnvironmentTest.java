package no.unit.nva.doi.fetch;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import java.util.Optional;
import org.junit.Rule;
import org.junit.Test;
import org.junit.contrib.java.lang.system.EnvironmentVariables;

public class EnvironmentTest {

    public static final String TEST_ENV = "TEST";
    public static final String TEST_VAL = "test";
    @Rule
    public final EnvironmentVariables environmentVariables
        = new EnvironmentVariables();

    @Test
    public void testEnv() {
        environmentVariables.set(TEST_ENV, TEST_VAL);
        Environment environment = new Environment();
        Optional<String> test = environment.get(TEST_ENV);
        assertEquals(TEST_VAL, test.get());
    }

    @Test
    public void testNoEnv() {
        Environment environment = new Environment();
        Optional<String> test = environment.get(TEST_ENV);
        assertFalse(test.isPresent());
    }
}
