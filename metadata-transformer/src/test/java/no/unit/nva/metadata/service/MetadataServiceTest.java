package no.unit.nva.metadata.service;

import com.github.tomakehurst.wiremock.WireMockServer;
import no.unit.nva.api.CreatePublicationRequest;
import no.unit.nva.model.EntityDescription;
import no.unit.nva.model.PublicationDate;
import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
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
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

public class MetadataServiceTest {

    public static final String URI_TEMPLATE = "http://localhost:%d/article/%s";
    public static final String TEST_REVIEW_PAPER_HTML = "test_review_paper.html";
    public static final String UPPER_CASE_DC_HTML = "upper_case_dc.html";


    private WireMockServer wireMockServer;

    @Test
    public void getMetadataJsonReturnsCreatePublicationRequest()
            throws IOException {
        MetadataService metadataService = new MetadataService();
        URI uri = prepareWebServerAndReturnUriToMetdata(TEST_REVIEW_PAPER_HTML);
        Optional<CreatePublicationRequest> request = metadataService.getCreatePublicationRequest(uri);

        assertKnownMetadataMappingsFromReviewPaperHtml(request.get());

        wireMockServer.stop();
    }

    private void assertKnownMetadataMappingsFromReviewPaperHtml(CreatePublicationRequest request) {
        assertThat(request.getEntityDescription(), is(notNullValue()));

        EntityDescription entityDescription = request.getEntityDescription();
        assertThat(entityDescription.getMainTitle(), is(notNullValue()));
        assertThat(entityDescription.getDescription(), is(notNullValue()));
        assertThat(entityDescription.getTags(), hasSize(3));
        assertThat(entityDescription.getContributors(), hasSize(2));
        assertThat(entityDescription.getDate(), is(notNullValue()));

        PublicationDate date = entityDescription.getDate();
        assertThat(date.getYear(), is(notNullValue()));
    }

    @Test
    public void getMetadataJsonFromUpperCaseTagMetadataReturnsCreatePublicationRequest()
            throws IOException {
        MetadataService metadataService = new MetadataService();
        URI uri = prepareWebServerAndReturnUriToMetdata(UPPER_CASE_DC_HTML);
        Optional<CreatePublicationRequest> request = metadataService.getCreatePublicationRequest(uri);

        assertKnownMetadataMappingsFromUpperCaseDcHtml(request.get());

        wireMockServer.stop();
    }

    private void assertKnownMetadataMappingsFromUpperCaseDcHtml(CreatePublicationRequest request) {
        assertThat(request.getEntityDescription(), is(notNullValue()));

        EntityDescription entityDescription = request.getEntityDescription();
        assertThat(entityDescription.getMainTitle(), is(notNullValue()));
        assertThat(entityDescription.getDescription(), is(notNullValue()));
        assertThat(entityDescription.getTags(), hasSize(1));
        assertThat(entityDescription.getContributors(), hasSize(2));
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
