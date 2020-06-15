package no.unit.nva.doi;


import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.net.URL;
import java.nio.file.Paths;
import org.junit.jupiter.api.Test;

public class DataciteClientTest {

    public static final String EXAMPLE_URL = "http://example.org";
    public static final String DATACITE_RESPONSE_FILE = "src/test/resources/dataciteResponse.json";
    public static final String EMPTY_RESPONSE_FILE = "src/test/resources/emptyResponse";

    @Test
    public void testMockUrl() throws IOException {
        DataciteClient dataciteClient = mock(DataciteClient.class);
        when(dataciteClient.createRequestUrl(anyString(), any(DataciteContentType.class))).thenCallRealMethod();
        when(dataciteClient.fetchMetadata(anyString(), any(DataciteContentType.class))).thenCallRealMethod();
        when(dataciteClient.readStringFromUrl(any(URL.class))).thenReturn(new String());

        MetadataAndContentLocation metadata = dataciteClient
            .fetchMetadata(EXAMPLE_URL, DataciteContentType.CITEPROC_JSON);

        assertNotNull(metadata);
    }

    @Test
    public void testValidResponseUrl() throws IOException {
        DataciteClient dataciteClient = mock(DataciteClient.class);
        when(dataciteClient.readStringFromUrl(any(URL.class))).thenCallRealMethod();
        String stringFromUrl = dataciteClient
            .readStringFromUrl(Paths.get(DATACITE_RESPONSE_FILE).toUri().toURL());
        assertNotNull(stringFromUrl);
    }

    @Test
    public void testEmptyResponseUrl() throws IOException {
        DataciteClient dataciteClient = mock(DataciteClient.class);
        when(dataciteClient.readStringFromUrl(any(URL.class))).thenCallRealMethod();
        String stringFromUrl = dataciteClient.readStringFromUrl(Paths.get(EMPTY_RESPONSE_FILE).toUri().toURL());
        assertEquals(new String(), stringFromUrl);
    }
}
