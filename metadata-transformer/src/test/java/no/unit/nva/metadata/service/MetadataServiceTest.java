package no.unit.nva.metadata.service;

import com.github.tomakehurst.wiremock.WireMockServer;
import no.unit.nva.api.CreatePublicationRequest;
import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.stream.Collectors;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.configureFor;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static nva.commons.core.JsonUtils.objectMapper;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;

public class MetadataServiceTest {

    public static final String URI_TEMPLATE = "http://localhost:%d/article/%s";
    public static final String TEST_REVIEW_PAPER_HTML = "test_review_paper.html";
    public static final String TEST_REVIEW_PAPER_JSON = "src/test/resources/test_review_paper.json";
    public static final String UPPER_CASE_DC_HTML = "upper_case_dc.html";
    public static final String UPPER_CASE_DC_JSON = "src/test/resources/upper_case_dc.json";


    private WireMockServer wireMockServer;

    @Test
    public void getMetadataJsonReturnsCreatePublicationRequest()
            throws IOException {
        MetadataService metadataService = new MetadataService();
        URI uri = prepareWebServerAndReturnUriToMetdata(TEST_REVIEW_PAPER_HTML);
        Optional<CreatePublicationRequest> request = metadataService.getCreatePublicationRequest(uri);

        File file = new File(TEST_REVIEW_PAPER_JSON);

        CreatePublicationRequest expected = objectMapper.readValue(file, CreatePublicationRequest.class);
        assertThat(request.get(), is(equalTo(expected)));

        wireMockServer.stop();
    }

    @Test
    public void getMetadataJsonFromUpperCaseTagMetadataReturnsCreatePublicationRequest()
            throws IOException {
        MetadataService metadataService = new MetadataService();
        URI uri = prepareWebServerAndReturnUriToMetdata(UPPER_CASE_DC_HTML);
        Optional<CreatePublicationRequest> request = metadataService.getCreatePublicationRequest(uri);

        File file = new File(UPPER_CASE_DC_JSON);

        CreatePublicationRequest expected = objectMapper.readValue(file, CreatePublicationRequest.class);
        assertThat(request.get(), is(equalTo(expected)));

        wireMockServer.stop();
    }

    private URI prepareWebServerAndReturnUriToMetdata(String filename) {
        startMock(filename);
        var uriString = String.format(URI_TEMPLATE, wireMockServer.port(), filename);
        return URI.create(uriString);
    }

    private void startMock(String filename) {
        wireMockServer = new WireMockServer();
        wireMockServer.start();
        var body = getBody("/" + filename);
        configureFor("localhost", wireMockServer.port());
        stubFor(get(urlEqualTo("/article/" + filename))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "text/html")
                        .withBody(body)));
    }

    private String getBody(String filename) {
        var inputStream = getClass().getResourceAsStream(filename);
        return new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8)).lines()
                .collect(Collectors.joining("\n"));
    }

}
