package no.unit.nva.doi.transformer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import java.util.Optional;
import org.junit.Rule;
import org.junit.Test;
import org.junit.contrib.java.lang.system.EnvironmentVariables;

public class EnvironmentTest {

    @Rule
    public final EnvironmentVariables environmentVariables = new EnvironmentVariables();

    @Test
    public void testEnv() {
        environmentVariables.set("TEST", "test");
        Environment environment = new Environment();
        Optional<String> test = environment.get("TEST");
        assertEquals("test", test.get());
    }

    @Test
    public void testNoEnv() {
        Environment environment = new Environment();
        Optional<String> test = environment.get("TEST");
        assertFalse(test.isPresent());
    }
}
