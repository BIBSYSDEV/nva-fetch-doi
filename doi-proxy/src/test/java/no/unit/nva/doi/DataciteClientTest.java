package no.unit.nva.doi;


import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.Path;
import java.nio.file.Paths;
import nva.commons.utils.IoUtils;
import org.junit.jupiter.api.Test;

public class DataciteClientTest {

    public static final String EXAMPLE_URL = "http://example.org";
    public static final String DATACITE_RESPONSE_FILE_RESOURCE = "dataciteResponse.json";
    public static final String EMPTY_RESPONSE_FILE_RESOURCE = "emptyResponse";
    public static final String MOCK_URL_CONTENT = "Some content for the example URL";
    public static final String EMPTY_STRING = "";

    @Test
    public void fetchMetadataReturnsUrlContentForSomeUrl() throws IOException {
        DataciteClient dataciteClient = mock(DataciteClient.class);
        when(dataciteClient.createRequestUrl(anyString(), any(DataciteContentType.class))).thenCallRealMethod();
        when(dataciteClient.fetchMetadata(anyString(), any(DataciteContentType.class))).thenCallRealMethod();
        when(dataciteClient.readStringFromUrl(any(URL.class))).thenReturn(MOCK_URL_CONTENT);

        MetadataAndContentLocation metadata = dataciteClient
            .fetchMetadata(EXAMPLE_URL, DataciteContentType.CITEPROC_JSON);

        assertNotNull(metadata);
        assertThat(metadata.getJson(),is(equalTo(MOCK_URL_CONTENT)));

    }

    @Test
    public void testValidResponseUrl() throws IOException {
        Path resourceAbsolutePath = Path.of(resourceAbsolutePathString(DATACITE_RESPONSE_FILE_RESOURCE));
        DataciteClient dataciteClient = mock(DataciteClient.class);
        when(dataciteClient.readStringFromUrl(any(URL.class))).thenCallRealMethod();
        String actualContent = dataciteClient.readStringFromUrl(resourceAbsolutePath.toUri().toURL());
        String expected= IoUtils.stringFromResources(Path.of(DATACITE_RESPONSE_FILE_RESOURCE));
        assertThat(actualContent,is(equalTo(expected)));
    }

    @Test
    public void testEmptyResponseUrl() throws IOException {
        Path resourceAbsolutePath = Path.of(resourceAbsolutePathString(EMPTY_RESPONSE_FILE_RESOURCE));

        DataciteClient dataciteClient = mock(DataciteClient.class);
        when(dataciteClient.readStringFromUrl(any(URL.class))).thenCallRealMethod();
        String stringFromUrl = dataciteClient.readStringFromUrl(resourceAbsolutePath.toUri().toURL());
        assertEquals(EMPTY_STRING, stringFromUrl);
    }

    private String resourceAbsolutePathString(String resource) {
        return Thread.currentThread().getContextClassLoader().getResource(resource).getPath();
    }
}
