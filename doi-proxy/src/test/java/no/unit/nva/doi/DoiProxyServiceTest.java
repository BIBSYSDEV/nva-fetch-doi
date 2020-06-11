package no.unit.nva.doi;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.net.URISyntaxException;
import no.unit.nva.doi.fetch.exceptions.MetadataNotFoundException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

public class DoiProxyServiceTest {

    public static final String EXAMPLE_DOI_URL = "https://doi.org/10.1000/182";
    private CrossRefClient crossRefClient;
    private DataciteClient dataciteClient;

    @BeforeEach
    public void init() {
        crossRefClient = Mockito.mock(CrossRefClient.class);
        dataciteClient = Mockito.mock(DataciteClient.class);
    }

    @Test
    public void lookupDoiMetadataReturnsMetadataOnValidDoi() throws MetadataNotFoundException, IOException, URISyntaxException {
        DoiProxyService doiProxyService = new DoiProxyService(crossRefClient, dataciteClient);
        when(dataciteClient.fetchMetadata(anyString(), any())).thenReturn(new MetadataAndContentLocation("", ""));

        MetadataAndContentLocation metadataAndContentLocation = doiProxyService.lookupDoiMetadata(EXAMPLE_DOI_URL,
            DataciteContentType.DATACITE_JSON);

        assertNotNull(metadataAndContentLocation);
        assertNotNull(metadataAndContentLocation.getContentHeader());
        assertNotNull(metadataAndContentLocation.getJson());
    }

}
