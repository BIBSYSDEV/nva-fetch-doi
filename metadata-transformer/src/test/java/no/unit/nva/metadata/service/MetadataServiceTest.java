package no.unit.nva.metadata.service;

import com.github.tomakehurst.wiremock.WireMockServer;
import no.unit.nva.api.CreatePublicationRequest;
import no.unit.nva.metadata.MetadataConverter;
import no.unit.nva.model.EntityDescription;
import no.unit.nva.model.Publication;
import no.unit.nva.model.PublicationDate;
import org.apache.any23.extractor.ExtractionException;
import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
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
    public static final String VALUE = "value";
    public static final String TEST_REVIEW_PAPER_HTML = "test_review_paper.html";

    private WireMockServer wireMockServer;

    @Test
    public void getMetadataJsonReturnsCreatePublicationRequest() throws ExtractionException, IOException, URISyntaxException {
        String json = createFramedPublicationJsonld();
        CreatePublicationRequest request = MetadataConverter.fromJsonLd(json);

        assertThat(request.getEntityDescription(), is(notNullValue()));

        EntityDescription entityDescription = request.getEntityDescription();
        assertThat(entityDescription.getMainTitle(), is(notNullValue()));
        assertThat(entityDescription.getDescription(), is(notNullValue()));
        assertThat(entityDescription.getTags(), hasSize(3));
        assertThat(entityDescription.getContributors(), hasSize(2));
        assertThat(entityDescription.getDate(), is(notNullValue()));

        PublicationDate date = entityDescription.getDate();
        assertThat(date.getYear(), is(notNullValue()));

        wireMockServer.stop();
    }

    private String createFramedPublicationJsonld() throws IOException, ExtractionException, URISyntaxException {
        String filename = TEST_REVIEW_PAPER_HTML;
        startMock(filename);
        var uriString = String.format(URI_TEMPLATE, wireMockServer.port(), filename);

        URI uri = URI.create(uriString);

        MetadataService metadataService = new MetadataService();
        String json = metadataService.getMetadataJson(uri);

        assertThat(json, is(notNullValue()));
        return json;
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
