package no.unit.nva.doi;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;
import nva.commons.core.ioutils.IoUtils;
import org.junit.jupiter.api.Test;

public class DataciteClientTest {

    public static final String EXAMPLE_URL = "http://example.org";
    public static final String SAMPLE_RESPONSE_RESOURCE = "dataciteResponseSample.json";
    public static final String EMPTY_RESPONSE_RESOURCE = "emptyResponse";
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
        assertThat(metadata.getJson(), is(equalTo(MOCK_URL_CONTENT)));
    }

    @Test
    public void testValidResponseUrl() throws IOException, URISyntaxException {
        Path resourceAbsolutePath = Path.of(resourceAbsolutePathString(SAMPLE_RESPONSE_RESOURCE));
        DataciteClient dataciteClient = mock(DataciteClient.class);
        when(dataciteClient.readStringFromUrl(any(URL.class))).thenCallRealMethod();
        String actualContent = dataciteClient.readStringFromUrl(resourceAbsolutePath.toUri().toURL());
        String expected = IoUtils.stringFromResources(Path.of(SAMPLE_RESPONSE_RESOURCE));
        assertThat(actualContent, is(equalTo(expected)));

    }

    @Test
    public void testEmptyResponseUrl() throws IOException, URISyntaxException {
        Path resourceAbsolutePath = Path.of(resourceAbsolutePathString(EMPTY_RESPONSE_RESOURCE));
        DataciteClient dataciteClient = mock(DataciteClient.class);
        when(dataciteClient.readStringFromUrl(any(URL.class))).thenCallRealMethod();
        String stringFromUrl = dataciteClient.readStringFromUrl(resourceAbsolutePath.toUri().toURL());
        assertEquals(EMPTY_STRING, stringFromUrl);
    }

    private String resourceAbsolutePathString(String resource) throws URISyntaxException {
        URL url = Thread.currentThread().getContextClassLoader().getResource(resource);
        return new File(url.toURI()).toPath().toString(); //is there an easier way?
    }
}
