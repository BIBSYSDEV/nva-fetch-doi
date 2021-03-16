package no.unit.nva.metadata.service;

import com.github.tomakehurst.wiremock.WireMockServer;
import j2html.tags.EmptyTag;
import no.unit.nva.api.CreatePublicationRequest;
import no.unit.nva.metadata.service.testdata.DateArgumentsProvider;
import no.unit.nva.metadata.service.testdata.DcContentCaseArgumentsProvider;
import no.unit.nva.metadata.service.testdata.LanguageArgumentsProvider;
import no.unit.nva.metadata.service.testdata.MetaTagPair;
import no.unit.nva.metadata.service.testdata.UndefinedLanguageArgumentsProvider;
import no.unit.nva.model.Contributor;
import no.unit.nva.model.EntityDescription;
import no.unit.nva.model.Identity;
import no.unit.nva.model.PublicationDate;
import no.unit.nva.model.Reference;
import no.unit.nva.model.exceptions.MalformedContributorException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.ArgumentsSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.IOException;
import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

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
import static org.junit.jupiter.api.Assertions.assertTrue;

public class MetadataServiceTest {

    public static final String URI_TEMPLATE = "http://localhost:%d/article/%s";
    public static final String DC_CONTRIBUTOR = "DC.contributor";
    public static final String DC_CREATOR = "DC.creator";
    public static final String DC_TITLE = "DC.title";
    public static final String DC_TITLE_LOWERCASE = "dc.title";
    public static final String DCTERMS_ABSTRACT = "DCTERMS.abstract";
    public static final String DCTERMS_ABSTRACT_LOWERCASE = "dcterms.abstract";
    public static final String DC_DESCRIPTION = "DC.description";
    public static final String DC_COVERAGE = "DC.coverage";
    public static final String DC_SUBJECT = "DC.subject";
    public static final String DC_IDENTIFIER = "DC.identifier";
    public static final String CITATION_TITLE = "citation_title";
    public static final String DATE_SEPARATOR = "-";
    public static final String ARTICLE_HTML = "article.html";
    public static final String YEAR_ONLY = "2001";
    public static final String FULL_DATE = "2001-12-19";
    public static final String DC_DATE = "dc.date";
    public static final String DC_MISSPELT = "DC.lnaguage";
    public static final String IRRELEVANT = "Not important";

    private WireMockServer wireMockServer;

    @AfterEach
    void tearDown() {
        wireMockServer.stop();
    }

    @ParameterizedTest(name = "#{index} - {0}")
    @MethodSource({
        "provideMetadataWithLowercasePrefixes",
        "provideMetadataForTags",
        "provideMetadataForAbstract",
        "provideMetadataForTitle",
        "provideMetadataForContributors",
        "provideMetadataForIdentifier"
    })
    public void getCreatePublicationParsesHtmlAndReturnsMetadata(String testDescription, String html,
                                                                 CreatePublicationRequest expectedRequest)
        throws IOException {
        URI uri = prepareWebServerAndReturnUriToMetadata(ARTICLE_HTML, html);
        MetadataService metadataService = new MetadataService();
        Optional<CreatePublicationRequest> request = metadataService.getCreatePublicationRequest(uri);

        CreatePublicationRequest actual = request.orElseThrow();
        actual.setContext(null);

        assertThat(actual, is(equalTo(expectedRequest)));
    }

    @ParameterizedTest(name = "getCreatePublication ignores case of {0}")
    @ArgumentsSource(DcContentCaseArgumentsProvider.class)
    void getCreatePublicationReturnsValueWhenContentPrefixHasAnyCase(String tagAttribute,
                                                                     String value) throws IOException {
        Optional<CreatePublicationRequest> actual = getCreatePublicationRequestResponse(tagAttribute, value);
        assertTrue(actual.isPresent());
    }

    @ParameterizedTest(name = "getCreatePublication returns date when date attribute {0} with value {1}")
    @ArgumentsSource(DateArgumentsProvider.class)
    void getCreatePublicationReturnsDateWhenDateVariantIsPresent(String tagAttribute, String date) throws IOException {

        CreatePublicationRequest actual = getCreatePublicationRequest(tagAttribute, date);
        CreatePublicationRequest expectedRequest = getCreatePublicationRequestWithDateOnly(date);
        assertThat(actual, is(equalTo(expectedRequest)));
    }

    @Test
    @DisplayName("getCreatePublication accepts multiple dates, but only one publication date is returned")
    void getCreatePublicationReturnsMostCompletePublicationDateWhenMultipleCandidatesArePresent() throws IOException {

        List<MetaTagPair> metaDates = List.of(new MetaTagPair(DC_DATE, YEAR_ONLY),
                new MetaTagPair(DC_DATE, FULL_DATE));

        CreatePublicationRequest actual = getCreatePublicationRequest(metaDates);

        CreatePublicationRequest expectedRequest = getCreatePublicationRequestWithDateOnly(FULL_DATE);
        assertThat(actual, is(equalTo(expectedRequest)));
    }

    @ParameterizedTest(name = "Dates that are shorter are accepted when bad date is {0}")
    @ValueSource(strings = {"20111-02-01", "2011-033-11", "2010-01-011", "20100101", "First of Sept. 2010"})
    void getCreatePublicationReturnsLongestDateWhenLongerNonsenseCandidatesAreAvailable(String nonsense)
            throws IOException {
        List<MetaTagPair> metaDates = List.of(new MetaTagPair(DC_DATE, YEAR_ONLY),
                new MetaTagPair(DC_DATE, nonsense));

        CreatePublicationRequest actual = getCreatePublicationRequest(metaDates);

        CreatePublicationRequest expectedRequest = getCreatePublicationRequestWithDateOnly(YEAR_ONLY);
        assertThat(actual, is(equalTo(expectedRequest)));
    }

    @ParameterizedTest(name = "getCreatePublication returns Lexvo URI when input is {0} and {1}")
    @ArgumentsSource(LanguageArgumentsProvider.class)
    void getCreatePublicationReturnsLexvoUriWhenInputIsValidLanguage(String attribute,
                                                                     String language,
                                                                     URI expectedUri) throws IOException {
        CreatePublicationRequest actual = getCreatePublicationRequest(attribute, language);
        CreatePublicationRequest expectedRequest = createPublicationRequestWithLanguageOnly(expectedUri);
        assertThat(actual, is(equalTo(expectedRequest)));
    }

    @ParameterizedTest(name = "Invalid language code {1} in {0} is mapped to {2}")
    @ArgumentsSource(UndefinedLanguageArgumentsProvider.class)
    void getCreatePublicationReturnsLexvoUndUriWhenInputIsInvalidLanguage(String attribute,
                                                                          String code,
                                                                          URI expectedUri) throws IOException {
        CreatePublicationRequest actual = getCreatePublicationRequest(attribute, code);
        CreatePublicationRequest expectedRequest = createPublicationRequestWithLanguageOnly(expectedUri);
        assertThat(actual, is(equalTo(expectedRequest)));
    }

    @Test
    void getCreatePublicationReturnsNoValueWhenDcTermsElementIsUnknown() throws IOException {
        Optional<CreatePublicationRequest> request = getCreatePublicationRequestResponse(DC_MISSPELT, IRRELEVANT);
        assertTrue(request.isEmpty());
    }

    private CreatePublicationRequest getCreatePublicationRequest(List<MetaTagPair> metaDates) throws IOException {
        String html = createHtml(metaDates);
        URI uri = prepareWebServerAndReturnUriToMetadata(ARTICLE_HTML, html);
        MetadataService metadataService = new MetadataService();
        Optional<CreatePublicationRequest> request = metadataService.getCreatePublicationRequest(uri);
        CreatePublicationRequest actual = request.orElseThrow();
        actual.setContext(null);
        return actual;
    }

    private CreatePublicationRequest getCreatePublicationRequest(String attribute, String value) throws IOException {
        Optional<CreatePublicationRequest> request = getCreatePublicationRequestResponse(attribute, value);
        CreatePublicationRequest actual = request.orElseThrow();
        actual.setContext(null);
        return actual;
    }

    private Optional<CreatePublicationRequest> getCreatePublicationRequestResponse(String attribute, String value)
            throws IOException {
        String html = createHtml(new MetaTagPair(attribute, value));
        URI uri = prepareWebServerAndReturnUriToMetadata(ARTICLE_HTML, html);
        MetadataService metadataService = new MetadataService();
        return metadataService.getCreatePublicationRequest(uri);
    }

    private CreatePublicationRequest createPublicationRequestWithLanguageOnly(URI uri) {
        EntityDescription entityDescription = new EntityDescription.Builder()
                .withLanguage(uri)
                .build();
        CreatePublicationRequest expectedRequest = new CreatePublicationRequest();
        expectedRequest.setEntityDescription(entityDescription);
        return expectedRequest;
    }

    private CreatePublicationRequest getCreatePublicationRequestWithDateOnly(String date) {
        EntityDescription entityDescription = new EntityDescription.Builder()
                .withDate(createPublicationDate(date))
                .build();
        CreatePublicationRequest expectedRequest = new CreatePublicationRequest();
        expectedRequest.setEntityDescription(entityDescription);
        return expectedRequest;
    }

    private PublicationDate createPublicationDate(String date) {
        String[] dateParts = date.split(DATE_SEPARATOR);
        String year = dateParts[0];
        String month = dateParts.length > 1 ? dateParts[1] : null;
        String day = dateParts.length > 2 ? dateParts[2] : null;

        return new PublicationDate.Builder()
                .withYear(year)
                .withMonth(month)
                .withDay(day)
                .build();
    }

    private static Stream<Arguments> provideMetadataWithLowercasePrefixes() {
        String title = "Title";
        String abstractString = "Abstract";
        CreatePublicationRequest request = createRequestWithTitle(title);
        CreatePublicationRequest abstractOnlyRequest = createRequestWithDescriptionAndOrAbstract(null, abstractString);

        return Stream.of(
            generateTestHtml("Lowercase dc.title still maps to mainTitle in createRequest",
                Map.of(DC_TITLE_LOWERCASE, title), request),
            generateTestHtml("Lowercase dcterms.abstract still maps to abstract in createRequest",
                Map.of(DCTERMS_ABSTRACT_LOWERCASE, abstractString), abstractOnlyRequest)
        );
    }

    private static Stream<Arguments> provideMetadataForContributors() throws MalformedContributorException {
        String name = "Full name";
        CreatePublicationRequest request = requestWithContributorName(name);

        return Stream.of(
            generateTestHtml("DC.contributor maps to contributor name in createRequest",
                Map.of(DC_CONTRIBUTOR, name), request),
            generateTestHtml("DC.creator maps to contributor name in createRequest",
                Map.of(DC_CREATOR, name), request)
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

    private static Stream<Arguments> provideMetadataForTitle() {
        String title = "Title";
        String longerTitle = "This is a longer title";
        CreatePublicationRequest request = createRequestWithTitle(title);
        CreatePublicationRequest longerTitleRequest = createRequestWithTitle(longerTitle);

        return Stream.of(
            generateTestHtml("DC.title maps to mainTitle in createRequest",
                Map.of(DC_TITLE, title), request),
            generateTestHtml("citation.title maps to mainTitle in createRequest",
                Map.of(CITATION_TITLE, title), request),
            generateTestHtml("The longer title takes precedence and maps to mainTitle in createRequest",
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
            generateTestHtml("DC.coverage and DC.subject maps to respective tags in createRequest",
                Map.of(DC_COVERAGE, coverage, DC_SUBJECT, subject), request)
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
            generateTestHtml("DCTERMS.abstract and DC.description maps to respective tags in createRequest",
                Map.of(DCTERMS_ABSTRACT, abstractString, DC_DESCRIPTION, description), request),
            generateTestHtml("Without DCTERMS.abstract maps DC.description to abstract in createRequest",
                Map.of(DC_DESCRIPTION, abstractString), abstractOnlyRequest),
            generateTestHtml("Without DC.description, DCTERMS.abstract maps to abstract in createRequest",
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
            generateTestHtml("DC.identifier with doi prefix maps to doi URI in createRequest",
                Map.of(DC_TITLE, dummyTitle, DC_IDENTIFIER, doiWithPrefix), request),
            generateTestHtml("DC.identifier without doi prefix maps to doi URI in createRequest",
                Map.of(DC_TITLE, dummyTitle, DC_IDENTIFIER, doiWithoutPrefix), request),
            generateTestHtml("DC.identifier as doi URI maps to doi URI in createRequest",
                Map.of(DC_TITLE, dummyTitle, DC_IDENTIFIER, expectedDoi), request),
            generateTestHtml("DC.identifier with no doi maps to empty createRequest",
                Map.of(DC_TITLE, dummyTitle, DC_IDENTIFIER, notADoi), emptyRequest)
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

    private static Arguments generateTestHtml(String testDescription, Map<String, String> metadata,
                                              CreatePublicationRequest expected) {
        return Arguments.of(testDescription, createHtml(metadata), expected);
    }

    private String createHtml(MetaTagPair tagPair) {
        return html(head(getMetaTag(tagPair))).renderFormatted();
    }

    private static String createHtml(List<MetaTagPair> tagPairs) {
        return html(
                head(
                        tagPairs.stream()
                                .map(MetadataServiceTest::getMetaTag)
                                .toArray(EmptyTag[]::new)
                )
        ).renderFormatted();
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

    private static EmptyTag getMetaTag(MetaTagPair tagPair) {
        return meta().withName(tagPair.getName()).withContent(tagPair.getContent());
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
