package no.unit.nva.doi.transformer.utils;

import no.bibsys.aws.tools.IoUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;

class BareProxyClientTest {

    private static final Path BARE_PROXY_SAMPLE_PATH = Paths.get("bareproxysample.json");
    private static final String SAMPLE_ARPID = "https://api.dev.nva.aws.unit.no/person/1600776277420";
    private static final String SAMPLE_ORCID = "https://sandbox.orcid.org/0000-0002-8617-3281";
    private BareProxyClient bareProxyClient;

    @BeforeEach
    void before() throws IOException {
        HttpClient httpClient = mockHttpClientWithNonEmptyResponse();
        bareProxyClient = new BareProxyClient(httpClient);
    }

    private HttpClient mockHttpClientWithNonEmptyResponse() throws IOException {
        String responseBody = IoUtils.resourceAsString(BARE_PROXY_SAMPLE_PATH);
        HttpResponseStatus200<String> response = new HttpResponseStatus200<>(responseBody);
        return new MockHttpClient<>(response);
    }

    @Test
    @DisplayName("fetchAuthorityDataForOrcid returns an Optional apr identifier for an existing Orcid (URL)")
    public void fetchAuthorityDataForOrcidReturnAnOptionalIdentifierForAnExistingOrcid()
            throws IOException {
        Optional<String> result = bareProxyClient.lookupArpidForOrcid(SAMPLE_ORCID);
        String expected = SAMPLE_ARPID;
        assertThat(result.isPresent(), is(true));
        assertThat(result.get(), is(equalTo(expected)));
    }


}