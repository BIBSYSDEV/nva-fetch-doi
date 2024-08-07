package no.unit.nva.metadata.service;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.configureFor;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;
import static j2html.TagCreator.body;
import static j2html.TagCreator.div;
import static j2html.TagCreator.head;
import static j2html.TagCreator.html;
import static j2html.TagCreator.meta;
import static j2html.TagCreator.span;
import static j2html.TagCreator.title;
import static java.util.Collections.emptyList;
import static java.util.Objects.nonNull;
import static no.unit.nva.commons.json.JsonUtils.dtoObjectMapper;
import static no.unit.nva.metadata.service.testdata.ContributorArgumentsProvider.CITATION_AUTHOR;
import static no.unit.nva.metadata.service.testdata.ContributorArgumentsProvider.DC_CONTRIBUTOR;
import static no.unit.nva.metadata.service.testdata.ContributorArgumentsProvider.DC_CREATOR;
import static no.unit.nva.testutils.RandomDataGenerator.randomInteger;
import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static no.unit.nva.testutils.RandomDataGenerator.randomUri;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.in;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.github.tomakehurst.wiremock.WireMockServer;
import j2html.tags.ContainerTag;
import j2html.tags.EmptyTag;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpHeaders;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiPredicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import no.sikt.nva.testing.http.WiremockHttpClient;
import no.unit.nva.doi.fetch.commons.publication.model.Contributor;
import no.unit.nva.doi.fetch.commons.publication.model.CreatePublicationRequest;
import no.unit.nva.doi.fetch.commons.publication.model.EntityDescription;
import no.unit.nva.doi.fetch.commons.publication.model.Identity;
import no.unit.nva.doi.fetch.commons.publication.model.PublicationContext;
import no.unit.nva.doi.fetch.commons.publication.model.PublicationDate;
import no.unit.nva.doi.fetch.commons.publication.model.PublicationInstance;
import no.unit.nva.doi.fetch.commons.publication.model.contexttypes.Book;
import no.unit.nva.doi.fetch.commons.publication.model.contexttypes.UnconfirmedJournal;
import no.unit.nva.metadata.service.testdata.ContributorArgumentsProvider;
import no.unit.nva.metadata.service.testdata.DcContentCaseArgumentsProvider;
import no.unit.nva.metadata.service.testdata.LanguageArgumentsProvider;
import no.unit.nva.metadata.service.testdata.MetaTagPair;
import no.unit.nva.metadata.service.testdata.MetaTagTitleProvider;
import no.unit.nva.metadata.service.testdata.ShortDoiUriArgumentsProvider;
import no.unit.nva.metadata.service.testdata.TypeInformationArgumentsProvider;
import no.unit.nva.metadata.service.testdata.UndefinedLanguageArgumentsProvider;
import no.unit.nva.metadata.service.testdata.ValidDateArgumentsProvider;
import no.unit.nva.metadata.service.testdata.ValidDoiFullUriArgumentsProvider;
import no.unit.nva.metadata.service.testdata.ValidDoiPseudoUrnArgumentsProvider;
import no.unit.nva.metadata.service.testdata.ValidDoiStringArgumentsProvider;
import no.unit.nva.metadata.type.Citation;
import no.unit.nva.metadata.type.DcTerms;
import nva.commons.core.paths.UriWrapper;
import nva.commons.logutils.LogUtils;
import nva.commons.logutils.TestAppender;
import org.hamcrest.collection.IsEmptyCollection;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.ArgumentsSource;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

public class MetadataServiceTest {

    public static final String URI_TEMPLATE = "http://localhost:%d/article/%s";
    public static final String DC_TITLE = "DC.title";
    public static final String DC_TITLE_LOWERCASE = "dc.title";
    public static final String DCTERMS_ABSTRACT = "DCTERMS.abstract";
    public static final String DCTERMS_ABSTRACT_LOWERCASE = "dcterms.abstract";
    public static final String DC_DESCRIPTION = "DC.description";
    public static final String CITATION_TITLE = "citation_title";
    public static final String DATE_SEPARATOR = "-";
    public static final String ARTICLE_HTML = "article.html";
    public static final String YEAR_ONLY = "2001";
    public static final String FULL_DATE = "2001-12-19";
    public static final String DC_DATE = "dc.date";
    public static final String DC_MISSPELT = "DC.lnaguage";
    public static final String IRRELEVANT = "Not important";
    public static final String VALID_DATE = "2017";
    public static final int MOVED_PERMANENTLY = 301;
    public static final String LOCATION = "location";
    public static final String DC_IDENTIFIER = DcTerms.IDENTIFIER.getMetaTagName();
    public static final String CITATION_DOI = Citation.DOI.getMetaTagName();
    public static final String FAKE_TITLE = "A wonderful html head title";
    public static final String INVALID_ISXN = "2002";
    public static final String FIRST_ISBN_ISBN10_VARIANT = "1627050116";
    public static final String FIRST_ISBN_ISBN13_VARIANT = "9781627050111";
    public static final String SECOND_ISBN_ISBN10_VARIANT = "1627050124";
    public static final String SECOND_ISBN_ISBN13_VARIANT = "9781627050128";
    public static final String ONLINE_ISSN = "2052-2916";
    public static final String PRINT_ISSN = "0969-0700";
    private static final TestAppender logger = LogUtils.getTestingAppenderForRootLogger();
    private WireMockServer wireMockServer;

    private URI serverUriJournal;
    private URI serverUriPublisher;
    private HttpClient httpClient;

    @BeforeEach
    public void initialize() {
        wireMockServer = new WireMockServer(options().dynamicPort());
        wireMockServer.start();
        this.serverUriJournal = URI.create(wireMockServer.baseUrl() + "/journal");
        this.serverUriPublisher = URI.create(wireMockServer.baseUrl() + "/publisher");
        this.httpClient = WiremockHttpClient.create();
    }

    @AfterEach
    public void tearDown() {
        wireMockServer.stop();
    }

    @Test
    public void shouldReturnJournalIdFromPublicationChannelsByGivenJournalName() {
        var expectedJournalName = randomString();
        var expectedYear = randomInteger();
        var queryUri = createExpectedQueryUriForJournalName(expectedJournalName, String.valueOf(expectedYear));
        var expectedJournalUri = mockedPublicationChannelsReturnsUri(queryUri);
        var metadataService = new MetadataService(httpClient, serverUriJournal, serverUriPublisher);
        var actualId = metadataService.fetchJournalIdFromPublicationChannel(
                expectedJournalName, null, null, expectedYear)
                           .orElseThrow();
        assertThat(actualId, is(expectedJournalUri));
    }

    @Test
    public void shouldReturnPublisherIdFromPublicationChannelsByGivenPublisherName() {
        var expectedPublisherName = randomString();
        var queryUri = createExpectedQueryUriForPublisherWithName(expectedPublisherName);
        var expectedPublisherUri = mockedPublicationChannelsReturnsUri(queryUri);
        var metadataService = new MetadataService(httpClient, serverUriJournal, serverUriPublisher);
        var actualPublisherUri = metadataService.fetchPublisherIdFromPublicationChannel(expectedPublisherName);
        assertThat(actualPublisherUri, is(expectedPublisherUri));
    }

    @ParameterizedTest(name = "#{index} - {0}")
    @MethodSource({
        "provideMetadataWithLowercasePrefixes",
        "provideMetadataForAbstract"
    })
    void getCreatePublicationParsesHtmlAndReturnsMetadata(String ignored,
                                                          String html,
                                                          CreatePublicationRequest expectedRequest) {

        var uri = prepareWebServerAndReturnUriToMetadata(html);
        var metadataService = new MetadataService(httpClient, serverUriJournal, serverUriPublisher);
        var request = metadataService.generateCreatePublicationRequest(uri);

        var actual = request.orElseThrow();

        var actualTitle = actual.getEntityDescription().getMainTitle();
        var expectedTitle = expectedRequest.getEntityDescription().getMainTitle();
        assertThat(actualTitle, equalTo(expectedTitle));

        var actualAbstract = actual.getEntityDescription().getMainAbstract();
        var expectedAbstract = expectedRequest.getEntityDescription().getMainAbstract();
        assertThat(actualAbstract, equalTo(expectedAbstract));
    }

    @Test
    void getCreatePublicationRequestAddsTagsWhenTagsArePresent() {
        List<MetaTagPair> metaTags = List.of(new MetaTagPair("dcterms.coverage", "Coverage"),
                                             new MetaTagPair("dcterms.temporal", "Temporal"),
                                             new MetaTagPair("dcterms.spatial", "Spatial"),
                                             new MetaTagPair("dc.subject", "Subject"));
        List<String> actual = getCreatePublicationRequest(metaTags).getEntityDescription().getTags();
        String[] expected = List.of("Coverage", "Temporal", "Spatial", "Subject").toArray(String[]::new);
        assertThat(actual, containsInAnyOrder(expected));
    }

    @ParameterizedTest(name = "getCreatePublication ignores case of {0}")
    @ArgumentsSource(DcContentCaseArgumentsProvider.class)
    void getCreatePublicationReturnsValueWhenContentPrefixHasAnyCase(String tagAttribute,
                                                                     String value) throws IOException,
                                                                                          InterruptedException {
        Optional<CreatePublicationRequest> actual = getCreatePublicationRequestResponse(tagAttribute, value);
        assertTrue(actual.isPresent());
    }

    @ParameterizedTest(name = "getCreatePublication returns date when date attribute {0} with value {1}")
    @ArgumentsSource(ValidDateArgumentsProvider.class)
    void getCreatePublicationReturnsDateWhenDateVariantIsPresent(String tagAttribute, String date)
        throws IOException, InterruptedException {

        var actual = getCreatePublicationRequest(tagAttribute, date).getEntityDescription().getPublicationDate();
        var expected = getCreatePublicationRequestWithDateOnly(date).getEntityDescription().getPublicationDate();

        assertThat(actual, is(equalTo(expected)));
    }

    @Test
    void getCreatePublicationReturnsMostCompletePublicationDateWhenMultipleCandidatesArePresent() {

        List<MetaTagPair> metaDates = List.of(new MetaTagPair(DC_DATE, YEAR_ONLY),
                                              new MetaTagPair(DC_DATE, FULL_DATE));

        var actualPublicationDate = getCreatePublicationRequest(metaDates)
                                        .getEntityDescription()
                                        .getPublicationDate();

        var expectedPublicationDate = getCreatePublicationRequestWithDateOnly(FULL_DATE)
                                          .getEntityDescription()
                                          .getPublicationDate();
        assertThat(actualPublicationDate, is(equalTo(expectedPublicationDate)));
    }

    @ParameterizedTest(name = "Bad date {0} is ignored in preference for valid date")
    @ValueSource(strings = {"", "20111-02-01", "2011-033-11", "2010-01-011", "20100101", "First of Sept. 2010"})
    void getCreatePublicationReturnsValidDateWhenValidAndInvalidCandidatesAreAvailable(String nonsense) {
        var metaDates = List.of(new MetaTagPair(DC_DATE, VALID_DATE),
                                new MetaTagPair(DC_DATE, nonsense));

        var actualPublicationDate = getCreatePublicationRequest(metaDates)
                                        .getEntityDescription()
                                        .getPublicationDate();

        var expectedPublicationDate = getCreatePublicationRequestWithDateOnly(VALID_DATE)
                                          .getEntityDescription()
                                          .getPublicationDate();

        assertThat(actualPublicationDate, is(equalTo(expectedPublicationDate)));
    }

    @ParameterizedTest(name = "getCreatePublication returns Lexvo URI when input is {0} and {1}")
    @ArgumentsSource(LanguageArgumentsProvider.class)
    void getCreatePublicationReturnsLexvoUriWhenInputIsValidLanguage(String attribute,
                                                                     String language,
                                                                     URI expectedUri) throws IOException,
                                                                                             InterruptedException {
        var actualLanguage = getCreatePublicationRequest(attribute, language).getEntityDescription().getLanguage();
        var expectedLanguage = createPublicationRequestWithLanguageOnly(expectedUri).getEntityDescription()
                                   .getLanguage();
        assertThat(actualLanguage, is(equalTo(expectedLanguage)));
    }

    @ParameterizedTest(name = "Invalid language code {1} in {0} is mapped to {2}")
    @ArgumentsSource(UndefinedLanguageArgumentsProvider.class)
    void getCreatePublicationReturnsLexvoUndUriWhenInputIsInvalidLanguage(String attribute,
                                                                          String code,
                                                                          URI expectedUri) throws IOException,
                                                                                                  InterruptedException {
        var actualLanguage = getCreatePublicationRequest(attribute, code).getEntityDescription().getLanguage();
        var expectedLanguage = createPublicationRequestWithLanguageOnly(expectedUri).getEntityDescription()
                                   .getLanguage();

        assertThat(actualLanguage, is(equalTo(expectedLanguage)));
    }

    @Test
    void getCreatePublicationReturnsNoValueWhenDcTermsElementIsUnknown() throws IOException, InterruptedException {
        Optional<CreatePublicationRequest> request = getCreatePublicationRequestResponse(DC_MISSPELT, IRRELEVANT);
        assertTrue(request.isEmpty());
    }

    @ParameterizedTest
    @ArgumentsSource(ValidDoiStringArgumentsProvider.class)
    void getCreatePublicationRequestReturnsHttpsDoiWhenInputIsDoiString(String metaTagName,
                                                                        String metaTagContent,
                                                                        URI expected)
        throws IOException, InterruptedException {
        CreatePublicationRequest createPublicationRequest = getCreatePublicationRequest(metaTagName, metaTagContent);
        URI actual = createPublicationRequest.getEntityDescription().getReference().getDoi();
        assertThat(actual, equalTo(expected));
    }

    @ParameterizedTest(name = "HTTPS DOI extracted from META tag name {0} and content DOI pseudo-URN {1}")
    @ArgumentsSource(ValidDoiPseudoUrnArgumentsProvider.class)
    void getCreatePublicationRequestReturnsHttpsDoiWhenInputIsPseudoUrnOrPlainDoi(String metaTagName,
                                                                                  String metaTagContent,
                                                                                  URI expected)
        throws IOException, InterruptedException {
        CreatePublicationRequest createPublicationRequest = getCreatePublicationRequest(metaTagName, metaTagContent);
        URI actual = createPublicationRequest.getEntityDescription().getReference().getDoi();
        assertThat(actual, equalTo(expected));
    }

    @ParameterizedTest(name = "HTTPS DOI extracted from META tag name {0} and content HTTP DOI {1}")
    @ArgumentsSource(ValidDoiFullUriArgumentsProvider.class)
    void getCreatePublicationRequestReturnsHttpsDoiWhenInputIncludesValidHttpOrHttpsDoi(String metaTagName,
                                                                                        String metaTagContent,
                                                                                        URI expected)
        throws IOException, InterruptedException {
        CreatePublicationRequest createPublicationRequest = getCreatePublicationRequest(metaTagName, metaTagContent);
        URI actual = createPublicationRequest.getEntityDescription().getReference().getDoi();
        assertThat(actual, equalTo(expected));
    }

    @ParameterizedTest(name = "HTTPS DOI extracted from META tag name {0} and content ShortDoi {0}")
    @ArgumentsSource(ShortDoiUriArgumentsProvider.class)
    void getCreatePublicationRequestReturnsHttpsDoiWhenInputIncludesValidShortDoi(String metaTagName,
                                                                                  String metaTagContent,
                                                                                  URI expected)
        throws IOException, InterruptedException {
        CreatePublicationRequest createPublicationRequest = getCreatePublicationRequest(metaTagName, metaTagContent,
                                                                                        expected.toString());
        URI actual = createPublicationRequest.getEntityDescription().getReference().getDoi();
        assertThat(actual, equalTo(expected));
    }

    @Test
    void getCreatePublicationRequestReturnsSingleHttpsDoiWhenInputContainsManyValidDois() {
        List<MetaTagPair> doimetaTagPairs = List.of(
            new MetaTagPair(DC_IDENTIFIER, "https://doi.org/10.1109/5.771073"),
            new MetaTagPair(DC_IDENTIFIER, "http://doi.org/10.1109/5.771073"),
            new MetaTagPair(DC_IDENTIFIER, "https://dx.doi.org/10.1109/5.771073"),
            new MetaTagPair(DC_IDENTIFIER, "http://dx.doi.org/10.1109/5.771073"),
            new MetaTagPair(DC_IDENTIFIER, "10.1109/5.771073"),
            new MetaTagPair(DC_IDENTIFIER, "doi:10.1109/5.771073"),
            new MetaTagPair(CITATION_DOI, "https://doi.org/10.1109/5.771073"),
            new MetaTagPair(CITATION_DOI, "https://doi.org/10.1109/5.771073"),
            new MetaTagPair(CITATION_DOI, "http://doi.org/10.1109/5.771073"),
            new MetaTagPair(CITATION_DOI, "https://dx.doi.org/10.1109/5.771073"),
            new MetaTagPair(CITATION_DOI, "http://dx.doi.org/10.1109/5.771073"),
            new MetaTagPair(CITATION_DOI, "10.1109/5.771073"),
            new MetaTagPair(CITATION_DOI, "doi:10.1109/5.771073")
        );
        CreatePublicationRequest createPublicationRequest = getCreatePublicationRequest(doimetaTagPairs);
        URI expected = URI.create("https://doi.org/10.1109/5.771073");
        URI actual = createPublicationRequest.getEntityDescription().getReference().getDoi();
        assertThat(actual, is(not(instanceOf(List.class))));
        assertThat(actual, equalTo(expected));
    }

    @ParameterizedTest
    @ArgumentsSource(ContributorArgumentsProvider.class)
    void getCreatePublicationRequestReturnsContributorWhenInputIsValidContributor(List<MetaTagPair> tags) {
        CreatePublicationRequest request = getCreatePublicationRequest(tags);
        Object[] expected = tags.stream()
                                .filter(this::isNameProperty)
                                .map(MetaTagPair::getContent)
                                .distinct()
                                .toArray();
        List<String> actual = request.getEntityDescription().getContributors().stream()
                                  .map(Contributor::identity)
                                  .map(Identity::getName)
                                  .collect(Collectors.toList());
        assertThat(actual, containsInAnyOrder(expected));
    }

    @Test
    void getCreatePublicationRequestReturnsTitleWhenInputContainsOnlyHtmlHeadTitle() {
        CreatePublicationRequest request = getCreatePublicationRequest();
        String actual = request.getEntityDescription().getMainTitle();
        assertThat(actual, equalTo(FAKE_TITLE));
    }

    @ParameterizedTest(name = "MetaTag {0} is assessed as title")
    @ArgumentsSource(MetaTagTitleProvider.class)
    void getCreatePublicationRequestReturnsTitleWhenInputContainsMetaTitle(String metaTagName, String metaTagContent)
        throws IOException, InterruptedException {
        CreatePublicationRequest request =
            getCreatePublicationRequest(metaTagName, metaTagContent);
        String actual = request.getEntityDescription().getMainTitle();
        assertThat(actual, equalTo(metaTagContent));
    }

    @ParameterizedTest
    @CsvSource({"long, longer, longest, longest", "longest, long, longer, longest"})
    void getCreatePublicationRequestReturnsLongestTitleWhenInputContainsMultipleTitles(String first,
                                                                                       String second,
                                                                                       String third,
                                                                                       String expected) {
        String shortAuthorName = createAuthorShorterThanShortestTitle(first, second, third);
        List<MetaTagPair> metaTags = List.of(new MetaTagPair(DC_TITLE, first), new MetaTagPair(CITATION_TITLE, second),
                                             new MetaTagPair(CITATION_AUTHOR, shortAuthorName));
        CreatePublicationRequest createPublicationRequest = getCreatePublicationRequest(third, metaTags);
        String actual = createPublicationRequest.getEntityDescription().getMainTitle();
        List<String> authorNames = createPublicationRequest.getEntityDescription()
                                       .getContributors()
                                       .stream()
                                       .map(Contributor::identity)
                                       .map(Identity::getName)
                                       .collect(Collectors.toList());
        assertThat(authorNames, hasItem(shortAuthorName));
        assertThat(actual, equalTo(expected));
    }

    @ParameterizedTest
    @ArgumentsSource(TypeInformationArgumentsProvider.class)
    void getCreatePublicationRequestReturnsTypeWhenInputIndicatesType(String metaTagName,
                                                                      String isxnImplyingContentType,
                                                                      Class<?> expectedContext,
                                                                      Class<?> expectedInstance) {
        CreatePublicationRequest createPublicationRequest = getCreatePublicationRequest(List.of(
            new MetaTagPair(CITATION_DOI, "10.0000/aaaa"),
            new MetaTagPair(metaTagName, isxnImplyingContentType)));

        PublicationContext actualContext = createPublicationRequest.getEntityDescription()
                                               .getReference().getPublicationContext();
        assertThat(actualContext, instanceOf(expectedContext));

        verifyMetaTagContentInPublicationContext(isxnImplyingContentType, actualContext);

        PublicationInstance actualInstance = createPublicationRequest.getEntityDescription()
                                                 .getReference().getPublicationInstance();
        assertThat(actualInstance, instanceOf(expectedInstance));
    }

    @DisplayName("getCreatePublicationRequest consumes multiple ISBNs adding distinct, converting ISBN-10 to ISBN-13")
    @Test
    void getCreatePublicationRequestReturnsMultipleIsbnsWhenMultipleIsbnsArePresent() {
        CreatePublicationRequest createPublicationRequest = getCreatePublicationRequest(List.of(
            new MetaTagPair(Citation.ISBN.getMetaTagName(), FIRST_ISBN_ISBN10_VARIANT),
            new MetaTagPair(Citation.ISBN.getMetaTagName(), SECOND_ISBN_ISBN10_VARIANT),
            new MetaTagPair(Citation.ISBN.getMetaTagName(), FIRST_ISBN_ISBN13_VARIANT),
            new MetaTagPair(Citation.ISBN.getMetaTagName(), SECOND_ISBN_ISBN13_VARIANT)));

        List<String> actual = ((Book) createPublicationRequest.getEntityDescription()
                                          .getReference().getPublicationContext()).isbnList();
        String[] expected = List.of(FIRST_ISBN_ISBN13_VARIANT, SECOND_ISBN_ISBN13_VARIANT)
                                .toArray(String[]::new);

        assertThat(actual, containsInAnyOrder(expected));
    }

    @DisplayName("getCreatePublicationRequest consumes ignores invalid isbns")
    @Test
    void getCreatePublicationRequestIgnoresInvalidIsbns() {
        CreatePublicationRequest createPublicationRequest = getCreatePublicationRequest(List.of(
            new MetaTagPair(Citation.ISBN.getMetaTagName(), INVALID_ISXN)));

        List<String> actual = ((Book) createPublicationRequest.getEntityDescription()
                                          .getReference().getPublicationContext()).isbnList();
        assertThat(actual, IsEmptyCollection.empty());
    }

    @Test
    void getCreatePublicationRequestReturnsSingleIssnWhenMultipleCandidatesArePresent() {
        CreatePublicationRequest createPublicationRequest = getCreatePublicationRequest(List.of(
            new MetaTagPair(Citation.ISSN.getMetaTagName(), ONLINE_ISSN),
            new MetaTagPair(Citation.ISSN.getMetaTagName(), PRINT_ISSN),
            new MetaTagPair(Citation.ISSN.getMetaTagName(), ONLINE_ISSN),
            new MetaTagPair(Citation.ISSN.getMetaTagName(), PRINT_ISSN)));

        String actual = ((UnconfirmedJournal) createPublicationRequest.getEntityDescription()
                                                  .getReference().getPublicationContext()).onlineIssn();
        String[] expected = List.of(ONLINE_ISSN, PRINT_ISSN)
                                .toArray(String[]::new);

        assertThat(actual, is(in(expected)));
    }

    @ParameterizedTest
    @ValueSource(strings = {"dcterms.dateAccepted", "dcterms.dateCopyrighted", "dcterms.dateSubmitted"})
    void getCreatePublicationRequestReturnsOptionalEmptyWhenInputIsValidButUnmappedDate(String property)
        throws IOException, InterruptedException {
        Optional<CreatePublicationRequest> createPublicationRequest =
            getCreatePublicationRequestResponse(property, "2002");
        assertTrue(createPublicationRequest.isEmpty());
    }

    @ParameterizedTest(name = "Non-literals are filtered for {0}")
    @ValueSource(strings = {"dc.abstract", "dc.description", "dc.creator", "citation_isbn", "citation_issn",
        "dc.language", "dc.subject", "dc.title"})
    void getCreatePublicationRequestReturnsOptionalEmptyWhenExpectedInputIsStringButInputIsUri(String property) {
        Optional<CreatePublicationRequest> createPublicationRequest =
            getCreatePublicationRequestResponseWithRdfSource(property);
        assertTrue(createPublicationRequest.isEmpty());
    }

    @Test
    void getCreatePublicationRequestReturnsOptionalEmptyWhenExpectedInputIsInvalidIsxn()
        throws IOException, InterruptedException {
        Optional<CreatePublicationRequest> createPublicationRequest =
            getCreatePublicationRequestResponse("citation_issn", INVALID_ISXN);
        assertTrue(createPublicationRequest.isEmpty());
        assertThat(logger.getMessages(), containsString("Could not extract type metadata from statement "));
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

    private static CreatePublicationRequest createRequestWithTitle(String title) {
        EntityDescription entityDescription = new EntityDescription.Builder()
                                                  .withMainTitle(title)
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

    private static CreatePublicationRequest createRequestWithDescriptionAndOrAbstract(
        String description, String abstractString) {
        EntityDescription entityDescription = new EntityDescription.Builder()
                                                  .withDescription(description)
                                                  .withMainAbstract(abstractString)
                                                  .build();
        CreatePublicationRequest request = new CreatePublicationRequest();
        request.setEntityDescription(entityDescription);
        return request;
    }

    private static Arguments generateTestHtml(String testDescription, Map<String, String> metadata,
                                              CreatePublicationRequest expected) {
        return Arguments.of(testDescription, createHtml(metadata), expected);
    }

    private static String createHtml(MetaTagPair tagPair) {
        return createHtml(null, List.of(tagPair));
    }

    private static String createHtml(String htmlTitle, List<MetaTagPair> metaTags) {
        ContainerTag head = head();
        EmptyTag[] headTags = metaTags.stream().map(MetadataServiceTest::getMetaTag).toArray(EmptyTag[]::new);
        if (headTags.length > 0) {
            head.with(headTags);
        }

        if (nonNull(htmlTitle)) {
            head.with(title(htmlTitle));
        }
        ContainerTag htmlBody = body("hello");
        return html(head, htmlBody).renderFormatted();
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

    private static String createRdfaHtml(String property) {
        return html(
            div(span().attr("property", property)
                    .attr("resource", "https://example.org/pool-tables")
            ).attr("xmlns:dc", "http://purl.org/dc/elements/1.1/")
                .attr("about", "https://example.org/something")
        ).renderFormatted();
    }

    private static EmptyTag getMetaTag(MetaTagPair tagPair) {
        return meta().withName(tagPair.getName()).withContent(tagPair.getContent());
    }

    private static EmptyTag getMetaTag(Map<String, String> metadata, String content) {
        return meta().withName(content).withContent(metadata.get(content));
    }

    private String createAuthorShorterThanShortestTitle(String... titles) {
        return Arrays.stream(titles).reduce(this::shortestString)
                   .map(this::leftHalf)
                   .orElseThrow();
    }

    private String leftHalf(String title) {
        return title.substring(0, title.length() / 2);
    }

    private String shortestString(String left, String right) {
        return (left.length() < right.length()) ? left : right;
    }

    private void verifyMetaTagContentInPublicationContext(String metaTagContent, PublicationContext context) {
        if (context instanceof UnconfirmedJournal) {
            assertThat(((UnconfirmedJournal) context).onlineIssn(), equalTo(metaTagContent));
        } else {
            assertThat(((Book) context).isbnList(), contains(metaTagContent));
        }
    }

    private boolean isNameProperty(MetaTagPair tagPair) {
        return DC_CONTRIBUTOR.equals(tagPair.getName())
               || DC_CREATOR.equals(tagPair.getName())
               || CITATION_AUTHOR.equals(tagPair.getName());
    }

    private CreatePublicationRequest getCreatePublicationRequest() {
        return getCreatePublicationRequest(FAKE_TITLE, emptyList());
    }

    private CreatePublicationRequest getCreatePublicationRequest(List<MetaTagPair> tagPairs) {
        return getCreatePublicationRequest(null, tagPairs);
    }

    private CreatePublicationRequest getCreatePublicationRequest(String htmlTitle, List<MetaTagPair> metaTags) {
        String html = createHtml(htmlTitle, metaTags);
        URI uri = prepareWebServerAndReturnUriToMetadata(html);
        MetadataService metadataService = new MetadataService();
        Optional<CreatePublicationRequest> request = metadataService.generateCreatePublicationRequest(uri);
        return request.orElseThrow();
    }

    private CreatePublicationRequest getCreatePublicationRequest(String attribute, String value)
        throws IOException, InterruptedException {
        Optional<CreatePublicationRequest> request = getCreatePublicationRequestResponse(attribute, value);
        return request.orElseThrow();
    }

    private CreatePublicationRequest getCreatePublicationRequest(String attribute, String value, String expected)
        throws IOException, InterruptedException {
        Optional<CreatePublicationRequest> request = getCreatePublicationRequestResponse(attribute, value, expected);
        return request.orElseThrow();
    }

    private Optional<CreatePublicationRequest> getCreatePublicationRequestResponse(String attribute,
                                                                                   String value,
                                                                                   String expected)
        throws IOException, InterruptedException {
        URI uri = prepareWebServerAndReturnUriToMetadata(createHtml(new MetaTagPair(attribute, value)));
        MetadataService metadataService = nonNull(expected)
                                              ? new MetadataService(setUpMockingForShortDoi(expected))
                                              : new MetadataService();
        return metadataService.generateCreatePublicationRequest(uri);
    }

    private Optional<CreatePublicationRequest> getCreatePublicationRequestResponse(String attribute, String value)
        throws IOException, InterruptedException {
        return getCreatePublicationRequestResponse(attribute, value, null);
    }

    private Optional<CreatePublicationRequest> getCreatePublicationRequestResponseWithRdfSource(String property) {
        URI uri = prepareWebServerAndReturnUriToMetadata(createRdfaHtml(property));
        MetadataService metadataService = new MetadataService();
        return metadataService.generateCreatePublicationRequest(uri);
    }

    @SuppressWarnings("unchecked")
    private HttpClient setUpMockingForShortDoi(String expected) throws IOException, InterruptedException {
        HttpClient mockHttpClient = mock(HttpClient.class);
        HttpResponse<Void> response = mock(HttpResponse.class);
        when(response.statusCode()).thenReturn(MOVED_PERMANENTLY);
        HttpHeaders headers = HttpHeaders.of(Map.of(LOCATION, List.of(expected)), new TestBipredicate());
        when(response.headers()).thenReturn(headers);
        when(mockHttpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class))).thenReturn(response);
        return mockHttpClient;
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
                                                  .withPublicationDate(createPublicationDate(date))
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

        return new PublicationDate(year, month, day);
    }

    private URI createExpectedQueryUriForJournalName(String name, String year) {
        return UriWrapper.fromUri(serverUriPublisher)
                   .addQueryParameter(MetadataService.QUERY_PARAM_QUERY, name)
                   .addQueryParameter(MetadataService.QUERY_PARAM_YEAR, year)
                   .getUri();
    }

    private URI createExpectedQueryUriForPublisherWithName(String name) {
        return UriWrapper.fromUri(serverUriPublisher)
                   .addQueryParameter(MetadataService.QUERY_PARAM_QUERY, name)
                   .getUri();
    }

    private URI mockedPublicationChannelsReturnsUri(URI queryUri) {
        configureFor("localhost", wireMockServer.port());
        var uri = randomUri();
        ArrayNode publicationChannelsResponseBody = createPublicationChannelsResponseWithUri(uri);
        stubFor(get("/publisher?" + queryUri
                                        .getQuery())
                    .willReturn(aResponse().withBody(publicationChannelsResponseBody
                                                         .toPrettyString()).withStatus(HttpURLConnection.HTTP_OK)));
        stubFor(get("/journal?" + queryUri
                                      .getQuery())
                    .willReturn(aResponse().withBody(publicationChannelsResponseBody
                                                         .toPrettyString()).withStatus(HttpURLConnection.HTTP_OK)));
        return uri;
    }

    private ArrayNode createPublicationChannelsResponseWithUri(URI uri) {
        var publicationChannelsResponseBodyElement = dtoObjectMapper.createObjectNode();
        publicationChannelsResponseBodyElement.put("id", uri.toString());

        var publicationChannelsResponseBody = dtoObjectMapper.createArrayNode();
        publicationChannelsResponseBody.add(publicationChannelsResponseBodyElement);

        return publicationChannelsResponseBody;
    }

    private URI prepareWebServerAndReturnUriToMetadata(String body) {
        startMock(body);
        var uriString = String.format(URI_TEMPLATE, wireMockServer.port(), ARTICLE_HTML);
        return URI.create(uriString);
    }

    private void startMock(String body) {
        configureFor("localhost", wireMockServer.port());
        stubFor(get(urlEqualTo("/article/" + MetadataServiceTest.ARTICLE_HTML))
                    .willReturn(aResponse()
                                    .withHeader("Content-Type", "text/html")
                                    .withBody(body)));
    }

    private static class TestBipredicate implements BiPredicate<String, String> {

        @Override
        public boolean test(String s, String s2) {
            return true;
        }
    }
}
