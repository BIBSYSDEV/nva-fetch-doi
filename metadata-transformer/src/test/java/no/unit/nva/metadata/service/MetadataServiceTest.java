package no.unit.nva.metadata.service;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.configureFor;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static j2html.TagCreator.head;
import static j2html.TagCreator.html;
import static j2html.TagCreator.meta;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import com.github.tomakehurst.wiremock.WireMockServer;
import j2html.tags.EmptyTag;
import java.io.IOException;
import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;
import no.unit.nva.api.CreatePublicationRequest;
import no.unit.nva.model.Contributor;
import no.unit.nva.model.EntityDescription;
import no.unit.nva.model.Identity;
import no.unit.nva.model.PublicationDate;
import no.unit.nva.model.Reference;
import no.unit.nva.model.exceptions.MalformedContributorException;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

public class MetadataServiceTest {

    public static final String URI_TEMPLATE = "http://localhost:%d/article/%s";
    public static final String DC_CONTRIBUTOR = "dc.contributor";
    public static final String DC_CONTRIBUTOR_UPPER_CASE = "DC.contributor";
    public static final String DC_CREATOR = "dc.creator";
    public static final String DC_CREATOR_UPPER_CASE = "DC.creator";
    public static final String DC_DATE = "dc.date";
    public static final String DC_DATE_UPPER_CASE = "DC.date";
    public static final String DC_TITLE = "dc.title";
    public static final String DC_TITLE_UPPER_CASE = "DC.title";
    public static final String DCTERMS_ABSTRACT = "dcterms.abstract";
    public static final String DCTERMS_ABSTRACT_UPPER_CASE = "DCTERMS.abstract";
    public static final String DC_DESCRIPTION = "dc.description";
    public static final String DC_DESCRIPTION_UPPER_CASE = "DC.description";
    public static final String DC_COVERAGE = "dc.coverage";
    public static final String DC_COVERAGE_UPPER_CASE = "DC.coverage";
    public static final String DC_SUBJECT = "dc.subject";
    public static final String DC_SUBJECT_UPPER_CASE = "DC.subject";
    public static final String DC_IDENTIFIER = "dc.identifier";
    public static final String DC_IDENTIFIER_UPPER_CASE = "DC.identifier";
    public static final String DC_LANGUAGE = "dc.language";
    public static final String DC_LANGUAGE_UPPER_CASE = "DC.language";
    public static final String CITATION_TITLE = "citation_title";

    private WireMockServer wireMockServer;

    @ParameterizedTest(name = "#{index} - {0}")
    @MethodSource({
        "provideMetadataForTags",
        "provideMetadataForAbstract",
        "provideMetadataForTitle",
        "provideMetadataForDate",
        "provideMetadataForContributors",
        "provideMetadataForIdentifier",
        "provideMetadataForLanguage"
    })
    public void getCreatePublicationParsesHtmlAndReturnsMetadata(String testDescription, String html,
                                                                 CreatePublicationRequest expectedRequest)
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
            generateTestHtml("dc.contributor maps to contributor name in createRequest",
                Map.of(DC_CONTRIBUTOR, name), request),
            generateTestHtml("DC.contributor maps to contributor name in createRequest",
                Map.of(DC_CONTRIBUTOR_UPPER_CASE, name), request),
            generateTestHtml("dc.creator maps to contributor name in createRequest",
                Map.of(DC_CREATOR, name), request),
            generateTestHtml("DC.creator maps to contributor name in createRequest",
                Map.of(DC_CREATOR_UPPER_CASE, name), request)
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
        CreatePublicationRequest request = createRequestWithDate(date);

        PublicationDate yearOnlyDate = new PublicationDate.Builder()
            .withYear(year)
            .build();
        CreatePublicationRequest yearOnlyRequest = createRequestWithDate(yearOnlyDate);

        String dateString = String.join("-", year, month, day);

        return Stream.of(
            generateTestHtml("dc.date with year, month and day maps to publication date in createRequest",
                Map.of(DC_DATE, dateString), request),
            generateTestHtml("DC.date with year, month and day maps to publication date createRequest",
                Map.of(DC_DATE_UPPER_CASE, dateString), request),
            generateTestHtml("dc.date with only year maps to publication date createRequest",
                Map.of(DC_DATE, year), yearOnlyRequest),
            generateTestHtml("DC.date with only year maps to publication date createRequest",
                Map.of(DC_DATE_UPPER_CASE, year), yearOnlyRequest)
        );
    }

    private static CreatePublicationRequest createRequestWithDate(PublicationDate date) {
        EntityDescription entityDescription = new EntityDescription.Builder()
            .withDate(date)
            .build();
        CreatePublicationRequest request = new CreatePublicationRequest();
        request.setEntityDescription(entityDescription);
        return request;
    }

    private static Stream<Arguments> provideMetadataForTitle() {
        String title = "Title";
        String longerTitle = "This is a longer title";
        CreatePublicationRequest request = createRequestWithTitle(title);
        CreatePublicationRequest longerTitleRequest = createRequestWithTitle(longerTitle);

        return Stream.of(
            generateTestHtml("dc.title maps to title in createRequest",
                Map.of(DC_TITLE, title), request),
            generateTestHtml("DC.title maps to title in createRequest",
                Map.of(DC_TITLE_UPPER_CASE, title), request),
            generateTestHtml("citation.title maps to title in createRequest",
                Map.of(CITATION_TITLE, title), request),
            generateTestHtml("The longer title takes precedence and maps to title in createRequest",
                Map.of(DC_TITLE, title, CITATION_TITLE, longerTitle), longerTitleRequest)
        );
    }

    private static CreatePublicationRequest createRequestWithTitle(String title) {
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
        CreatePublicationRequest request = createRequestWithTags(List.of(subject, coverage));

        return Stream.of(
            generateTestHtml("dc.coverage and dc.subject maps to respective tags in createRequest",
                Map.of(DC_COVERAGE, coverage, DC_SUBJECT, subject), request),
            generateTestHtml("DC.coverage and DC.subject maps to respective tags in createRequest",
                Map.of(DC_COVERAGE_UPPER_CASE, coverage, DC_SUBJECT_UPPER_CASE, subject), request)
        );
    }

    private static CreatePublicationRequest createRequestWithTags(List<String> tags) {
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

        CreatePublicationRequest request = createRequestWithDescriptionAndOrAbstract(description, abstractString);
        CreatePublicationRequest abstractOnlyRequest =
            createRequestWithDescriptionAndOrAbstract(null, abstractString);

        return Stream.of(
            generateTestHtml("dcterms.abstract and dc.description maps to respective tags in createRequest",
                Map.of(DCTERMS_ABSTRACT, abstractString, DC_DESCRIPTION, description), request),
            generateTestHtml("DCTERMS.abstract and DC.description maps to respective tags in createRequest",
                Map.of(DCTERMS_ABSTRACT_UPPER_CASE, abstractString, DC_DESCRIPTION_UPPER_CASE, description), request),
            generateTestHtml("Without dcterms.abstract maps dc.description to abstract in createRequest",
                Map.of(DC_DESCRIPTION, abstractString), abstractOnlyRequest),
            generateTestHtml("Without dc.description, dcterms.abstract maps to abstract in createRequest",
                Map.of(DCTERMS_ABSTRACT, abstractString), abstractOnlyRequest)
        );
    }

    private static Stream<Arguments> provideMetadataForIdentifier() {
        String doiWithPrefix = "doi:10.1/url";
        String doiWithoutPrefix = "10.1/url";
        String notADoi = "identifier/not/a/doi";
        String expectedDoi = "https://doi.org/10.1/url";
        String dummyTitle = "To avoid NPE in JsonLdProcessor";

        CreatePublicationRequest request = createRequestWithIdentifier(URI.create(expectedDoi), dummyTitle);
        CreatePublicationRequest emptyRequest = createRequestWithIdentifier(null, dummyTitle);

        return Stream.of(
            generateTestHtml("dc.identifier with doi prefix maps to doi URI in createRequest",
                Map.of(DC_TITLE, dummyTitle, DC_IDENTIFIER, doiWithPrefix), request),
            generateTestHtml("DC.identifier without doi prefix maps to doi URI in createRequest",
                Map.of(DC_TITLE, dummyTitle, DC_IDENTIFIER_UPPER_CASE, doiWithoutPrefix), request),
            generateTestHtml("DC.identifier as doi URI maps to doi URI in createRequest",
                Map.of(DC_TITLE, dummyTitle, DC_IDENTIFIER_UPPER_CASE, expectedDoi), request),
            generateTestHtml("dc.identifier with no doi maps to empty createRequest",
                Map.of(DC_TITLE, dummyTitle, DC_IDENTIFIER, notADoi), emptyRequest)
        );
    }

    private static Stream<Arguments> provideMetadataForLanguage() {
        String language = "de";
        String uppercaseLanguage = "DE";
        String notALanguage = "00";
        String expectedLanguage = "https://lexvo.org/id/iso639-3/deu";
        String underterminedLanguage = "https://lexvo.org/id/iso639-3/und";
        CreatePublicationRequest request = requestWithLanguage(URI.create(expectedLanguage));
        CreatePublicationRequest underterminedRequest = requestWithLanguage(URI.create(underterminedLanguage));
        return Stream.of(
            generateTestHtml("dc.language with BCP-47 code maps to ISO639-3 code in createRequest",
                Map.of(DC_LANGUAGE, language), request),
            generateTestHtml("DC.language with BCP-47 code maps to ISO639-3 code in createRequest",
                Map.of(DC_LANGUAGE_UPPER_CASE, uppercaseLanguage), request),
            generateTestHtml("dc.language with invalid code maps to 'und' ISO639-3 code in createRequest",
                Map.of(DC_LANGUAGE, notALanguage), underterminedRequest),
            generateTestHtml("DC.language with empty code maps to 'und' ISO639-3 code in createRequest",
                Map.of(DC_LANGUAGE_UPPER_CASE, ""), underterminedRequest),
            generateTestHtml("dc.language with no value maps to 'und' ISO639-3 code in createRequest",
                DC_LANGUAGE, null, underterminedRequest)
        );
    }

    private static CreatePublicationRequest createRequestWithIdentifier(URI identifier, String title) {
        EntityDescription entityDescription = new EntityDescription.Builder()
            .withMainTitle(title)
            .build();
        if (identifier != null) {
            Reference reference = new Reference.Builder()
                .withDoi(identifier)
                .build();
            entityDescription.setReference(reference);
        }
        CreatePublicationRequest request = new CreatePublicationRequest();
        request.setEntityDescription(entityDescription);
        return request;
    }

    private static CreatePublicationRequest createRequestWithDescriptionAndOrAbstract(
        String description, String abstractString) {
        EntityDescription entityDescription = new EntityDescription.Builder()
            .withDescription(description)
            .withAbstract(abstractString)
            .build();
        CreatePublicationRequest request = new CreatePublicationRequest();
        request.setEntityDescription(entityDescription);
        return request;
    }

    private static CreatePublicationRequest requestWithLanguage(URI language) {
        EntityDescription entityDescription = new EntityDescription.Builder()
            .withLanguage(language)
            .build();
        CreatePublicationRequest request = new CreatePublicationRequest();
        request.setEntityDescription(entityDescription);
        return request;
    }

    private static Arguments generateTestHtml(String testDescription, String name, String content,
                                              CreatePublicationRequest expected) {
        return Arguments.of(testDescription, html(head(meta().withName(name).withContent(content))).renderFormatted(),
            expected);
    }

    private static Arguments generateTestHtml(String testDescription, Map<String, String> metadata,
                                              CreatePublicationRequest expected) {
        return Arguments.of(testDescription, createHtml(metadata), expected);
    }

    private static String createHtml(Map<String, String> metadata) {
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
}