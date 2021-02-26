package no.unit.nva.metadata.service;

import com.github.tomakehurst.wiremock.WireMockServer;
import j2html.tags.EmptyTag;
import no.unit.nva.api.CreatePublicationRequest;
import no.unit.nva.model.Contributor;
import no.unit.nva.model.EntityDescription;
import no.unit.nva.model.Identity;
import no.unit.nva.model.PublicationDate;
import no.unit.nva.model.exceptions.MalformedContributorException;
import nva.commons.core.ioutils.IoUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.configureFor;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static j2html.TagCreator.head;
import static j2html.TagCreator.html;
import static j2html.TagCreator.meta;
import static nva.commons.core.JsonUtils.objectMapper;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;

public class MetadataServiceTest {

    public static final String URI_TEMPLATE = "http://localhost:%d/article/%s";
    public static final String TEST_REVIEW_PAPER_HTML = "test_review_paper.html";
    public static final String TEST_REVIEW_PAPER_JSON = "test_review_paper.json";
    public static final String UPPER_CASE_DC_HTML = "upper_case_dc.html";
    public static final String UPPER_CASE_DC_JSON = "upper_case_dc.json";

    public static final String DC_CONTRIBUTOR = "dc.contributor";
    public static final String DC_CONTRIBUTOR_UPPER_CASE = "DC.contributor";
    public static final String DC_CREATOR = "dc.creator";
    public static final String DC_CREATOR_UPPER_CASE = "DC.creator";
    public static final String DC_DATE = "dc.date";
    public static final String DC_DATE_UPPER_CASE = "DC.date";
    public static final String DC_TITLE = "dc.title";
    public static final String DC_TITLE_UPPER_CASE = "DC.title";
    public static final String DCTERMS_ABSTRACT = "dcterms.abstract";
    public static final String DC_DESCRIPTION = "dc.description";
    public static final String DC_COVERAGE = "dc.coverage";
    public static final String DC_SUBJECT = "dc.subject";


    private WireMockServer wireMockServer;

    @Test
    public void getMetadataJsonReturnsCreatePublicationRequest()
            throws IOException {
        MetadataService metadataService = new MetadataService();
        URI uri = prepareWebServerAndReturnUriToMetadata(TEST_REVIEW_PAPER_HTML);
        Optional<CreatePublicationRequest> request = metadataService.getCreatePublicationRequest(uri);

        CreatePublicationRequest expected = objectMapper.readValue(
                IoUtils.inputStreamFromResources(TEST_REVIEW_PAPER_JSON),
                CreatePublicationRequest.class);
        assertThat(request.isPresent(), is(true));
        assertThat(request.get(), is(equalTo(expected)));

        wireMockServer.stop();
    }

    @Test
    public void getMetadataJsonFromUpperCaseTagMetadataReturnsCreatePublicationRequest()
            throws IOException {
        MetadataService metadataService = new MetadataService();
        URI uri = prepareWebServerAndReturnUriToMetadata(UPPER_CASE_DC_HTML);
        Optional<CreatePublicationRequest> request = metadataService.getCreatePublicationRequest(uri);

        CreatePublicationRequest expected = objectMapper.readValue(
                IoUtils.inputStreamFromResources(UPPER_CASE_DC_JSON),
                CreatePublicationRequest.class);
        assertThat(request.isPresent(), is(true));
        assertThat(request.get(), is(equalTo(expected)));

        wireMockServer.stop();
    }

    @ParameterizedTest
    @MethodSource({
            "provideMetadataForTags",
            "provideMetadataForAbstract",
            "provideMetadataForTitle",
            "provideMetadataForDate",
            "provideMetadataForContributors"
    })
    public void getCreatePublicationParsesHtmlAndReturnsMetadata(String html, CreatePublicationRequest expectedRequest)
            throws IOException {
        String filename = "article.html";
        URI uri = prepareWebServerAndReturnUriToMetadata(filename, html);
        MetadataService metadataService = new MetadataService();
        Optional<CreatePublicationRequest> request = metadataService.getCreatePublicationRequest(uri);

        assertThat(request.isPresent(), is(true));
        CreatePublicationRequest actual = request.get();
        actual.setContext(null);

        assertThat(actual, is(equalTo(expectedRequest)));

        wireMockServer.stop();
    }

    private static Stream<Arguments> provideMetadataForContributors() throws MalformedContributorException {
        String name = "Full name";
        CreatePublicationRequest request = requestWithContributorName(name);

        return Stream.of(
                generateMetadataHtml(Map.of(
                        DC_CONTRIBUTOR, name),
                        request),
                generateMetadataHtml(Map.of(
                        DC_CONTRIBUTOR_UPPER_CASE, name),
                        request),
                generateMetadataHtml(Map.of(
                        DC_CREATOR, name),
                        request),
                generateMetadataHtml(Map.of(
                        DC_CREATOR_UPPER_CASE, name),
                        request)
                );
    }

    private static CreatePublicationRequest requestWithContributorName(String name)
            throws MalformedContributorException {
        Identity identity = new Identity.Builder()
                .withName(name)
                .build();
        Contributor contributor = new Contributor.Builder()
                .withIdentity(identity)
                .build();
        EntityDescription entityDescription = new EntityDescription.Builder()
                .withContributors(List.of(contributor))
                .build();
        CreatePublicationRequest request = new CreatePublicationRequest();
        request.setEntityDescription(entityDescription);
        return request;
    }

    private static Stream<Arguments> provideMetadataForDate() {
        String year = "2021";
        String month = "02";
        String day = "22";

        PublicationDate date = new PublicationDate.Builder()
                .withYear(year)
                .withMonth(month)
                .withDay(day)
                .build();
        CreatePublicationRequest request = requestWithDate(date);

        PublicationDate yearOnlyDate = new PublicationDate.Builder()
                .withYear(year)
                .build();
        CreatePublicationRequest yearOnlyRequest = requestWithDate(yearOnlyDate);

        String dateString = String.join("-", year, month, day);

        return Stream.of(
                generateMetadataHtml(Map.of(
                        DC_DATE, dateString),
                        request),
                generateMetadataHtml(Map.of(
                        DC_DATE_UPPER_CASE, dateString),
                        request),
                generateMetadataHtml(Map.of(
                        DC_DATE, year),
                        yearOnlyRequest),
                generateMetadataHtml(Map.of(
                        DC_DATE_UPPER_CASE, year),
                        yearOnlyRequest)
                );
    }

    private static CreatePublicationRequest requestWithDate(PublicationDate date) {
        EntityDescription entityDescription = new EntityDescription.Builder()
                .withDate(date)
                .build();
        CreatePublicationRequest request = new CreatePublicationRequest();
        request.setEntityDescription(entityDescription);
        return request;
    }

    private static Stream<Arguments> provideMetadataForTitle() {
        String title = "Title";
        CreatePublicationRequest request = requestWithTitle(title);

        return Stream.of(
                generateMetadataHtml(Map.of(
                        DC_TITLE, title),
                        request),
                generateMetadataHtml(Map.of(
                        DC_TITLE_UPPER_CASE, title),
                        request)
                );
    }

    private static CreatePublicationRequest requestWithTitle(String title) {
        EntityDescription entityDescription = new EntityDescription.Builder()
                .withMainTitle(title)
                .build();
        CreatePublicationRequest request = new CreatePublicationRequest();
        request.setEntityDescription(entityDescription);
        return request;
    }

    private static Stream<Arguments> provideMetadataForTags() {
        String coverage = "Coverage";
        String subject = "Subject";
        CreatePublicationRequest request = requestWithTags(List.of(subject, coverage));

        return Stream.of(
                generateMetadataHtml(Map.of(
                        DC_COVERAGE, coverage,
                        DC_SUBJECT, subject),
                        request)
                );
    }

    private static CreatePublicationRequest requestWithTags(List<String> tags) {
        EntityDescription entityDescription = new EntityDescription.Builder()
                .withTags(tags)
                .build();
        CreatePublicationRequest request = new CreatePublicationRequest();
        request.setEntityDescription(entityDescription);
        return request;
    }

    private static Stream<Arguments> provideMetadataForAbstract() {
        String description = "Description";
        String abstractString = "Abstract";

        CreatePublicationRequest request = requestWithDescriptionAndOrAbstract(description, abstractString);

        CreatePublicationRequest abstractOnlyRequest =
                requestWithDescriptionAndOrAbstract(null, abstractString);

        return Stream.of(
                generateMetadataHtml(Map.of(
                        DCTERMS_ABSTRACT, abstractString,
                        DC_DESCRIPTION, description),
                        request),
                generateMetadataHtml(Map.of(
                        DC_DESCRIPTION, abstractString),
                        abstractOnlyRequest),
                generateMetadataHtml(Map.of(
                        DCTERMS_ABSTRACT, abstractString),
                        abstractOnlyRequest)
                 );
    }

    private static CreatePublicationRequest requestWithDescriptionAndOrAbstract(
            String description, String abstractString) {
        EntityDescription entityDescription = new EntityDescription.Builder()
                .withDescription(description)
                .withAbstract(abstractString)
                .build();
        CreatePublicationRequest request = new CreatePublicationRequest();
        request.setEntityDescription(entityDescription);
        return request;
    }

    private static Arguments generateMetadataHtml(Map<String,String> metadata, CreatePublicationRequest expected) {
        return Arguments.of(createHtml(metadata), expected);
    }

    private static String createHtml(Map<String,String> metadata) {
        return html(
                head(
                    metadata.keySet().stream()
                            .map(content -> getMetaTag(metadata, content))
                            .toArray(EmptyTag[]::new)
                )
        ).renderFormatted();
    }

    private static EmptyTag getMetaTag(Map<String, String> metadata, String content) {
        return meta().withName(content).withContent(metadata.get(content));
    }

    private URI prepareWebServerAndReturnUriToMetadata(String filename) {
        var body = getBody("/" + filename);
        return prepareWebServerAndReturnUriToMetadata(filename, body);
    }

    private URI prepareWebServerAndReturnUriToMetadata(String filename, String body) {
        startMock(filename, body);
        var uriString = String.format(URI_TEMPLATE, wireMockServer.port(), filename);
        return URI.create(uriString);
    }

    private void startMock(String filename, String body) {
        wireMockServer = new WireMockServer();
        wireMockServer.start();

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
