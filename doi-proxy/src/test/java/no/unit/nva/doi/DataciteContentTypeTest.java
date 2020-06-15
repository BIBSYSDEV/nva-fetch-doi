package no.unit.nva.doi;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

public class DataciteContentTypeTest {

    public static final String VALID_ENUM_NAME = DataciteContentType.DATACITE_JSON.getContentType();
    public static final String INVALID_ENUM_NAME = "Invalid enum name";

    @Test
    public void testLookupDataciteContentType() {

        DataciteContentType dataciteContentType = DataciteContentType.lookup(VALID_ENUM_NAME);
        assertEquals(dataciteContentType, DataciteContentType.DATACITE_JSON);
    }

    @Test
    public void testLookupDataciteContentTypeWithInvalidName() {

        Throwable exception = assertThrows(RuntimeException.class, () -> {
            DataciteContentType.lookup(INVALID_ENUM_NAME);
        });

        assertTrue(exception.getMessage().contains("Datacite Content Type not found for"));
    }
}
