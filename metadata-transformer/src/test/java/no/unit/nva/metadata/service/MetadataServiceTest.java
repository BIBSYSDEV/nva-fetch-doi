package no.unit.nva.metadata.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.WireMockServer;
import no.unit.nva.metadata.domain.Metadata;
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

    private WireMockServer wireMockServer;

    @Test
    public void getMetadataReturnsSelectedMetadataFromURI()
            throws ExtractionException, IOException, URISyntaxException {
        String filename = "test.html";
        startMock(filename);
        var uriString = String.format(URI_TEMPLATE, wireMockServer.port(), filename);

        URI uri = URI.create(uriString);

        MetadataService metadataService = new MetadataService();
        String json = metadataService.getMetadataJson(uri);

        assertThat(json, is(notNullValue()));

        ObjectMapper objectMapper = new ObjectMapper();
        Metadata metadata = objectMapper.readValue(json, Metadata.class);

        assertThat(metadata.getTitle().get(VALUE), is(notNullValue()));
        assertThat(metadata.getCreators(), hasSize(2));
        assertThat(metadata.getSubjects(), hasSize(6));
        assertThat(metadata.getDescription().get(VALUE), is(notNullValue()));
        assertThat(metadata.getDate().get(VALUE), is(notNullValue()));
        assertThat(metadata.getLanguage().get(VALUE), is(notNullValue()));
        assertThat(metadata.getId(), is(notNullValue()));
        assertThat(metadata.getType(), is(notNullValue()));

        wireMockServer.stop();
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
