package no.sikt.nva.scopus;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;
import static no.sikt.nva.scopus.ScopusConstants.ADDITIONAL_IDENTIFIERS_SCOPUS_ID_SOURCE_NAME;
import static no.unit.nva.commons.json.JsonUtils.dtoObjectMapper;
import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static no.unit.nva.testutils.RandomDataGenerator.randomUri;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.equalToObject;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.startsWith;
import static org.hamcrest.Matchers.stringContainsInOrder;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.hamcrest.core.IsInstanceOf.instanceOf;
import static org.hamcrest.core.IsNot.not;
import static org.hamcrest.core.IsNull.nullValue;
import static org.hamcrest.core.StringContains.containsString;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.events.S3Event;
import com.amazonaws.services.lambda.runtime.events.models.s3.S3EventNotification.RequestParametersEntity;
import com.amazonaws.services.lambda.runtime.events.models.s3.S3EventNotification.ResponseElementsEntity;
import com.amazonaws.services.lambda.runtime.events.models.s3.S3EventNotification.S3BucketEntity;
import com.amazonaws.services.lambda.runtime.events.models.s3.S3EventNotification.S3Entity;
import com.amazonaws.services.lambda.runtime.events.models.s3.S3EventNotification.S3EventNotificationRecord;
import com.amazonaws.services.lambda.runtime.events.models.s3.S3EventNotification.S3ObjectEntity;
import com.amazonaws.services.lambda.runtime.events.models.s3.S3EventNotification.UserIdentityEntity;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.github.tomakehurst.wiremock.WireMockServer;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.http.HttpClient;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import no.scopus.generated.BibrecordTp;
import no.scopus.generated.CitationTitleTp;
import no.scopus.generated.DocTp;
import no.scopus.generated.HeadTp;
import no.scopus.generated.ItemTp;
import no.scopus.generated.ItemidTp;
import no.scopus.generated.OrigItemTp;
import no.scopus.generated.TitletextTp;
import no.scopus.generated.YesnoAtt;
import no.sikt.nva.scopus.test.utils.ScopusGenerator;
import no.sikt.nva.testing.http.WiremockHttpClient;
import no.unit.nva.doi.models.Doi;
import no.unit.nva.metadata.service.MetadataService;
import no.unit.nva.events.models.EventReference;
import no.unit.nva.metadata.CreatePublicationRequest;
import no.unit.nva.model.AdditionalIdentifier;
import no.unit.nva.model.contexttypes.*;
import no.unit.nva.s3.S3Driver;
import no.unit.nva.stubs.FakeEventBridgeClient;
import no.unit.nva.stubs.FakeS3Client;
import nva.commons.core.SingletonCollector;
import nva.commons.core.ioutils.IoUtils;
import nva.commons.core.paths.UnixPath;
import nva.commons.core.paths.UriWrapper;
import nva.commons.logutils.LogUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import software.amazon.awssdk.core.sync.ResponseTransformer;
import software.amazon.awssdk.services.eventbridge.model.PutEventsRequestEntry;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;

class ScopusHandlerTest {

    public static final Context CONTEXT = mock(Context.class);
    public static final RequestParametersEntity EMPTY_REQUEST_PARAMETERS = null;
    public static final ResponseElementsEntity EMPTY_RESPONSE_ELEMENTS = null;
    public static final UserIdentityEntity EMPTY_USER_IDENTITY = null;
    public static final long SOME_FILE_SIZE = 100L;
    public static final String HARD_CODED_JOURNAL_NAME_IN_RESOURCE_FILE = "Edinburgh Journal of Botany";
    public static final String E_ISSN_0000469852 = "1474-0036";
    public static final String START_OF_QUERY = "/?";
    private static final String EXPECTED_RESULTS_PATH = "expectedResults";
    private static final String IDENTITY_FIELD_NAME = "identity";
    private static final String NAME_FIELD_NAME = "name";
    private static final String SEQUENCE_FIELD_NAME = "sequence";
    private static final String SCOPUS_XML_0000469852 = "2-s2.0-0000469852.xml";
    private static final String SCOPUS_XML_0018132378 = "2-s2.0-0018132378.xml";
    private static final String EXPECTED_PUBLICATION_YEAR_IN_0018132378 = "1978";
    private static final String EXPECTED_PUBLICATION_DAY_IN_0018132378 = "01";
    private static final String EXPECTED_PUBLICATION_MONTH_IN_0018132378 = "01";
    private static final String AUTHOR_KEYWORD_NAME_SPACE = "<authorKeywordsTp";
    private static final String HARDCODED_KEYWORDS_0000469852 = "    <author-keyword xml:lang=\"eng\">\n"
            + "        <sup>64</sup>Cu\n"
            + "              </author-keyword>\n"
            + "    <author-keyword "
            + "xml:lang=\"eng\">excretion</author-keyword>\n"
            + "    <author-keyword "
            + "xml:lang=\"eng\">sheep</author-keyword>\n"
            + "</authorKeywordsTp>";
    private static final String HARDCODED_EXPECTED_KEYWORD_1_IN_0000469852 = "64Cu";
    private static final String HARDCODED_EXPECTED_KEYWORD_2_IN_0000469852 = "excretion";
    private static final String HARDCODED_EXPECTED_KEYWORD_3_IN_0000469852 = "sheep";
    private static final String XML_ENCODING_DECLARATION = "<?xml version=\"1.0\" encoding=\"UTF-8\" "
            + "standalone=\"yes\"?>";

    private static final String SCOPUS_XML_85114653695 = "2-s2.0-85114653695.xml";
    private static final String CONTRIBUTOR_1_NAME_IN_85114653695 = "Morra A.";
    private static final String CONTRIBUTOR_2_NAME_IN_85114653695 = "Escala-Garcia M.";
    private static final String CONTRIBUTOR_151_NAME_IN_85114653695 = "NBCS Collaborators";
    private static final int CONTRIBUTOR_1_SEQUENCE_NUMBER = 1;
    private static final int CONTRIBUTOR_2_SEQUENCE_NUMBER = 2;
    private static final int CONTRIBUTOR_151_SEQUENCE_NUMBER = 151;
    private static final String PUBLICATION_DAY_FIELD_NAME = "day";
    private static final String PUBLICATION_MONTH_FIELD_NAME = "month";
    private static final String PUBLICATION_YEAR_FIELD_NAME = "year";
    private static final String FILENAME_EXPECTED_ABSTRACT_IN_0000469852 = "expectedAbstract.txt";
    private static final String EXPECTED_ABSTRACT_NAME_SPACE = "<abstractTp";
    public static final char JOURNAL_SOURCETYPE_IDENTIFYING_CHAR = 'j';


    private FakeS3Client s3Client;
    private S3Driver s3Driver;
    private ScopusHandler scopusHandler;
    private WireMockServer httpServer;
    private URI serverUriJournal;
    private URI serverUriPublisher;
    private MetadataService metadataService;

    private ScopusGenerator scopusData;
    private HttpClient httpClient;
    private FakeEventBridgeClient eventBridgeClient;

    @BeforeEach
    public void init() {
        s3Client = new FakeS3Client();
        s3Driver = new S3Driver(s3Client, "ignoredValue");
        startWiremockServer();
        httpClient = WiremockHttpClient.create();
        metadataService = new MetadataService(httpClient, serverUriJournal, serverUriPublisher);
        eventBridgeClient = new FakeEventBridgeClient();
        scopusHandler = new ScopusHandler(s3Client, metadataService, eventBridgeClient);
        scopusData = new ScopusGenerator();
    }

    @AfterEach
    public void tearDown() {
        httpServer.stop();
    }

    @Test
    void shouldLogExceptionMessageWhenExceptionOccurs() {
        var s3Event = createS3Event(randomString());
        var expectedMessage = randomString();
        s3Client = new FakeS3ClientThrowingException(expectedMessage);
        scopusHandler = new ScopusHandler(s3Client, metadataService, eventBridgeClient);
        var appender = LogUtils.getTestingAppenderForRootLogger();
        assertThrows(RuntimeException.class, () -> scopusHandler.handleRequest(s3Event, CONTEXT));
        assertThat(appender.getMessages(), containsString(expectedMessage));
    }

    @Test
    void shouldExtractOnlyScopusIdentifierIgnoreAllOtherIdentifiersAndStoreItInPublication() throws IOException {
        var scopusIdentifiers = keepOnlyTheScopusIdentifiers();
        var s3Event = createNewScopusPublicationEvent();
        var createPublicationRequest = scopusHandler.handleRequest(s3Event, CONTEXT);
        var actualAdditionalIdentifiers = createPublicationRequest.getAdditionalIdentifiers();
        var expectedAdditionalIdentifier =
                convertScopusIdentifiersToNvaAdditionalIdentifiers(scopusIdentifiers);
        assertThat(actualAdditionalIdentifiers, containsInAnyOrder(expectedAdditionalIdentifier));
    }

    @Test
    void shouldExtractDoiAndPlaceItInsideReferenceObject() throws IOException {
        var uri = s3Driver.insertFile(UnixPath.of(randomString()), scopusData.toXml());
        var expectedURI = Doi.fromDoiIdentifier(scopusData.getDocument().getMeta().getDoi()).getUri();
        var s3Event = createNewScopusPublicationEvent();
        var createPublicationRequest = scopusHandler.handleRequest(s3Event, CONTEXT);
        assertThat(createPublicationRequest.getEntityDescription().getReference().getDoi(), equalToObject(expectedURI));
    }

    @Test
    void shouldReturnCreatePublicationRequestWithMainTitle() throws IOException {
        var uri = s3Driver.insertFile(UnixPath.of(randomString()), scopusData.toXml());
        var s3Event = createS3Event(uri);
        var titleObject = extractTitle();
        var titleObjectAsXmlString = ScopusGenerator.toXml(titleObject);
        var createPublicationRequest = scopusHandler.handleRequest(s3Event, CONTEXT);
        var actualMainTitle = createPublicationRequest.getEntityDescription().getMainTitle();
        assertThat(actualMainTitle, is(equalTo(titleObjectAsXmlString)));
    }

    @Test
    void shouldExtractContributorsNamesAndSequenceNumberCorrectly() throws IOException {
        var scopusFile = IoUtils.stringFromResources(Path.of(SCOPUS_XML_85114653695));
        var uri = s3Driver.insertFile(randomS3Path(), scopusFile);
        var s3Event = createS3Event(uri);
        var createPublicationRequest = scopusHandler.handleRequest(s3Event, CONTEXT);
        var actualContributors = createPublicationRequest.getEntityDescription().getContributors();
        assertThat(actualContributors, hasItem(allOf(
                hasProperty(IDENTITY_FIELD_NAME,
                        hasProperty(NAME_FIELD_NAME, is(CONTRIBUTOR_1_NAME_IN_85114653695))),
                hasProperty(SEQUENCE_FIELD_NAME, is(CONTRIBUTOR_1_SEQUENCE_NUMBER)))));
        assertThat(actualContributors, hasItem(allOf(
                hasProperty(IDENTITY_FIELD_NAME,
                        hasProperty(NAME_FIELD_NAME, is(CONTRIBUTOR_2_NAME_IN_85114653695))),
                hasProperty(SEQUENCE_FIELD_NAME, is(CONTRIBUTOR_2_SEQUENCE_NUMBER)))));
        assertThat(actualContributors, hasItem(allOf(
                hasProperty(IDENTITY_FIELD_NAME,
                        hasProperty(NAME_FIELD_NAME, is(CONTRIBUTOR_151_NAME_IN_85114653695))),
                hasProperty(SEQUENCE_FIELD_NAME, is(CONTRIBUTOR_151_SEQUENCE_NUMBER)))));
    }

    @Test
    void shouldExtractAuthorKeywordsAsXML() throws IOException {
        var scopusFile = IoUtils.stringFromResources(Path.of(SCOPUS_XML_0018132378));
        var uri = s3Driver.insertFile(randomS3Path(), scopusFile);
        var s3Event = createS3Event(uri);
        var createPublicationRequest = scopusHandler.handleRequest(s3Event, CONTEXT);
        var actualKeywords = createPublicationRequest.getAuthorKeywordsXmlFormat();
        assertThat(actualKeywords, stringContainsInOrder(
                XML_ENCODING_DECLARATION,
                AUTHOR_KEYWORD_NAME_SPACE,
                HARDCODED_KEYWORDS_0000469852));
    }

    @Test
    void shouldReturnCreatePublicationRequestWithUnconfirmedPublicationContextWhenEventWithS3UriThatPointsToScopusXml()
            throws IOException {
        var scopusFile = IoUtils.stringFromResources(Path.of("2-s2.0-0000469852.xml"));
        var uri = s3Driver.insertFile(randomS3Path(), scopusFile);
        var s3Event = createS3Event(uri);
        var createPublicationRequest = scopusHandler.handleRequest(s3Event, CONTEXT);
        var actualPublicationContext = createPublicationRequest.getEntityDescription().getReference()
                .getPublicationContext();
        assertThat(actualPublicationContext, instanceOf(UnconfirmedJournal.class));
        var actualJournalName = ((UnconfirmedJournal) actualPublicationContext).getTitle();
        assertThat(actualJournalName, is(HARD_CODED_JOURNAL_NAME_IN_RESOURCE_FILE));
    }

    @Test
    void shouldReturnCreatePublicationRequestWithUnconfirmedPublicationContextWhenEventS3UriScopusXmlWithValidIssn()
            throws IOException {
        var scopusFile = IoUtils.stringFromResources(Path.of("2-s2.0-0000469852.xml"));
        scopusFile = scopusFile.replace("<issn type=\"print\">09604286</issn>",
                "<issn type=\"print\">0960-4286</issn>");
        var uri = s3Driver.insertFile(randomS3Path(), scopusFile);
        var s3Event = createS3Event(uri);
        var createPublicationRequest = scopusHandler.handleRequest(s3Event, CONTEXT);
        var actualPublicationContext = createPublicationRequest.getEntityDescription().getReference()
                .getPublicationContext();
        assertThat(actualPublicationContext, instanceOf(UnconfirmedJournal.class));
        var actualJournalName = ((UnconfirmedJournal) actualPublicationContext).getTitle();
        assertThat(actualJournalName, is(HARD_CODED_JOURNAL_NAME_IN_RESOURCE_FILE));
    }

    @Test
    void shouldReturnCreatePublicationRequestWithUnconfirmedPublicationContextWhenEventS3UriScopusXmlWithInValidIssn()
            throws IOException {
        var scopusFile = IoUtils.stringFromResources(Path.of("2-s2.0-0000469852.xml"));
        scopusFile = scopusFile.replace("<issn type=\"print\">09604286</issn>",
                "<issn type=\"print\">096042</issn>");
        var uri = s3Driver.insertFile(randomS3Path(), scopusFile);
        var s3Event = createS3Event(uri);
        Executable action = () -> scopusHandler.handleRequest(s3Event, CONTEXT);
        var exception = assertThrows(RuntimeException.class, action);
        var expectedMessage = "no.unit.nva.model.exceptions.InvalidIssnException: The ISSN";
        var actualMessage = exception.getMessage();
        assertThat(actualMessage, startsWith(expectedMessage));
    }

    @Test
    void shouldReturnDefaultPublicationContextWhenEventWithS3UriThatPointsToScopusXmlWithoutPrintIssn()
            throws IOException {
        var scopusFile = IoUtils.stringFromResources(Path.of("2-s2.0-0000469852.xml"));
        scopusFile = scopusFile.replace("<issn type=\"print\">09604286</issn>", "");
        var uri = s3Driver.insertFile(randomS3Path(), scopusFile);
        var s3Event = createS3Event(uri);
        var createPublicationRequest = scopusHandler.handleRequest(s3Event, CONTEXT);
        var actualPublicationContext = createPublicationRequest.getEntityDescription().getReference()
                .getPublicationContext();
        assertThat(actualPublicationContext, instanceOf(UnconfirmedJournal.class));
        var actualJournalName = ((UnconfirmedJournal) actualPublicationContext).getTitle();
        assertThat(actualJournalName, is(HARD_CODED_JOURNAL_NAME_IN_RESOURCE_FILE));
    }

    @Test
    void shouldReturnPublicationContextBookWithUnconfirmedPublisherWhenEventWithS3UriThatPointsToScopusXmlWithSrctypeB()
            throws IOException {
        scopusData.getDocument().getMeta().setSrctype("b");
        var uri = s3Driver.insertFile(UnixPath.of(randomString()), scopusData.toXml());
        var s3Event = createS3Event(uri);
        var createPublicationRequest = scopusHandler.handleRequest(s3Event, CONTEXT);
        var actualPublicationContext = createPublicationRequest.getEntityDescription().getReference()
                .getPublicationContext();
        assertThat(actualPublicationContext, instanceOf(Book.class));
        var actualPublisher = ((Book) actualPublicationContext).getPublisher();
        assertThat(actualPublisher, instanceOf(UnconfirmedPublisher.class));
        String expectedPublishername = scopusData.getDocument().getItem().getItem().getBibrecord().getHead().getSource()
                .getPublisher().get(0).getPublishername();
        String actualPublisherName = ((UnconfirmedPublisher) actualPublisher).getName();
        assertThat(actualPublisherName, is(expectedPublishername));
    }

    @Test
    void shouldReturnPublicationContextBookWithConfirmedPublisherWhenEventWithS3UriThatPointsToScopusXmlWithSrctypeB()
            throws IOException {
        scopusData.getDocument().getMeta().setSrctype("b");
        var expectedPublisherName = randomString();
        scopusData.getDocument().getItem().getItem().getBibrecord().getHead().getSource().getPublisher().get(0)
                .setPublishername(expectedPublisherName);
        var uri = s3Driver.insertFile(UnixPath.of(randomString()), scopusData.toXml());
        var s3Event = createS3Event(uri);
        var queryUri = createExpectedQueryUriForPublisherWithName(expectedPublisherName);
        var expectedPublisherUri = mockedPublicationChannelsReturnsUri(queryUri);
        var createPublicationRequest = scopusHandler.handleRequest(s3Event, CONTEXT);
        var actualPublicationContext = createPublicationRequest.getEntityDescription().getReference()
                .getPublicationContext();
        assertThat(actualPublicationContext, instanceOf(Book.class));
        var actualPublisher = ((Book) actualPublicationContext).getPublisher();
        assertThat(actualPublisher, instanceOf(Publisher.class));
        var actualPublisherId = ((Publisher) actualPublisher).getId();
        assertThat(actualPublisherId, is(expectedPublisherUri));
    }

    @Test
    void shouldReturnCreatePublicationRequestWithJournalWhenEventWithS3UriThatPointsToScopusXmlWhereSourceTitleIsInNsd()
            throws IOException {
        var scopusFile = IoUtils.stringFromResources(Path.of("2-s2.0-0000469852.xml"));

        var queryUri = createExpectedQueryUriForJournalWithEIssn(E_ISSN_0000469852, "2010");
        var expectedJournalUri = mockedPublicationChannelsReturnsUri(queryUri);

        scopusFile = scopusFile.replace("<xocs:pub-year>1993</xocs:pub-year>",
                "<xocs:pub-year>2010</xocs:pub-year>");
        var uri = s3Driver.insertFile(UnixPath.of(randomString()), scopusFile);
        var s3Event = createS3Event(uri);
        var createPublicationRequest = scopusHandler.handleRequest(s3Event, CONTEXT);
        var actualPublicationContext = createPublicationRequest.getEntityDescription().getReference()
                .getPublicationContext();
        assertThat(actualPublicationContext, instanceOf(Journal.class));
        var actualJournalName = ((Journal) actualPublicationContext).getId();
        assertThat(actualJournalName, is(expectedJournalUri));
    }

    @Test
    void shouldReturnDefaultPublicationContextWhenEventWithS3UriThatPointsToScopusXmlWithoutKnownSourceType()
            throws IOException {
        var scopusFile = IoUtils.stringFromResources(Path.of("2-s2.0-0000469852.xml"));
        var randomChar = getRandomCharBesidesUnwantedChar(JOURNAL_SOURCETYPE_IDENTIFYING_CHAR);
        scopusFile = scopusFile.replace("<xocs:srctype>j</xocs:srctype>", "<xocs:srctype>" + randomChar
                + "</xocs:srctype>");
        var uri = s3Driver.insertFile(randomS3Path(), scopusFile);
        var s3Event = createS3Event(uri);
        var createPublicationRequest = scopusHandler.handleRequest(s3Event, CONTEXT);
        var actualPublicationContext = createPublicationRequest.getEntityDescription().getReference()
                .getPublicationContext();
        assertThat(actualPublicationContext, is(ScopusConstants.EMPTY_PUBLICATION_CONTEXT));
    }

    @Test
    void shouldExtractAuthorKeyWordsAsPlainText() throws IOException {
        var scopusFile = IoUtils.stringFromResources(Path.of(SCOPUS_XML_0018132378));
        var uri = s3Driver.insertFile(randomS3Path(), scopusFile);
        var s3Event = createS3Event(uri);
        var createPublicationRequest = scopusHandler.handleRequest(s3Event, CONTEXT);
        var actualPlaintextKeyWords = createPublicationRequest.getEntityDescription().getTags();
        assertThat(actualPlaintextKeyWords, allOf(
                hasItem(HARDCODED_EXPECTED_KEYWORD_1_IN_0000469852),
                hasItem(HARDCODED_EXPECTED_KEYWORD_2_IN_0000469852),
                hasItem(HARDCODED_EXPECTED_KEYWORD_3_IN_0000469852)));
    }

    @Test
    void shouldExtractPublicationDate() throws IOException {
        var scopusFile = IoUtils.stringFromResources(Path.of(SCOPUS_XML_0018132378));
        var uri = s3Driver.insertFile(randomS3Path(), scopusFile);
        var s3Event = createS3Event(uri);
        var createPublicationRequest = scopusHandler.handleRequest(s3Event, CONTEXT);
        var actualPublicationDate = createPublicationRequest.getEntityDescription().getDate();
        assertThat(actualPublicationDate, allOf(
                hasProperty(PUBLICATION_DAY_FIELD_NAME, is(EXPECTED_PUBLICATION_DAY_IN_0018132378)),
                hasProperty(PUBLICATION_MONTH_FIELD_NAME, is(EXPECTED_PUBLICATION_MONTH_IN_0018132378)),
                hasProperty(PUBLICATION_YEAR_FIELD_NAME, is(EXPECTED_PUBLICATION_YEAR_IN_0018132378))));
    }

    @Test
    void shouldExtractMainAbstractAsXML() throws IOException {
        var scopusFile = IoUtils.stringFromResources(Path.of(SCOPUS_XML_0000469852));
        var uri = s3Driver.insertFile(randomS3Path(), scopusFile);
        var s3Event = createS3Event(uri);
        var createPublicationRequest = scopusHandler.handleRequest(s3Event, CONTEXT);
        var actualMainAbstract = createPublicationRequest.getEntityDescription().getAbstract();
        var expectedAbstract =
                IoUtils.stringFromResources(Path.of(EXPECTED_RESULTS_PATH, FILENAME_EXPECTED_ABSTRACT_IN_0000469852));
        assertThat(actualMainAbstract, stringContainsInOrder(XML_ENCODING_DECLARATION,
                EXPECTED_ABSTRACT_NAME_SPACE,
                expectedAbstract));
    }

    @Test
    void shouldEmitMessageToEventReferenceContainingS3UriPointingToNewCreatePublicationRequest() throws IOException {
        var event = createNewScopusPublicationEvent();
        var expectedRequest = scopusHandler.handleRequest(event, CONTEXT);
        var emittedEvent = fetchEmittedEvent();
        var createPublicationRequestS3Path = new UriWrapper(emittedEvent.getUri()).toS3bucketPath();
        var createPublicationRequestJson = s3Driver.getFile(createPublicationRequestS3Path);
        var request = CreatePublicationRequest.fromJson(createPublicationRequestJson);
        assertThat(request, is(not(nullValue())));
        assertThat(request, is(equalTo(expectedRequest)));
    }

    private EventReference fetchEmittedEvent() {
        return eventBridgeClient.getRequestEntries().stream()
            .map(PutEventsRequestEntry::detail)
            .map(EventReference::fromJson)
            .collect(SingletonCollector.collect());
    }

    private S3Event createNewScopusPublicationEvent() throws IOException {
        var uri = s3Driver.insertFile(randomS3Path(), scopusData.toXml());
        return createS3Event(uri);
    }

    private UnixPath randomS3Path() {
        return UnixPath.of(randomString());
    }

    private TitletextTp extractTitle() {
        return Optional.of(scopusData.getDocument())
                .map(DocTp::getItem)
                .map(ItemTp::getItem)
                .map(OrigItemTp::getBibrecord)
                .map(BibrecordTp::getHead)
                .map(HeadTp::getCitationTitle)
                .map(CitationTitleTp::getTitletext)
                .stream()
                .flatMap(Collection::stream)
                .filter(t -> YesnoAtt.Y.equals(t.getOriginal()))
                .collect(SingletonCollector.collect());
    }

    //nå er det bare 'j' som kommer inn som param, men jeg tror vi kan bruke metoden om igjen med andre sourceTypes
    private char getRandomCharBesidesUnwantedChar(char unwantedChar) {
        var randomChar = unwantedChar;
        do {
            randomChar = randomString().toLowerCase().charAt(0);
        } while (randomChar == unwantedChar);
        return randomChar;
    }

    private URI createExpectedQueryUriForJournalWithEIssn(String electronicIssn, String year) {
        return new UriWrapper(serverUriJournal)
                .addQueryParameter("query", electronicIssn)
                .addQueryParameter("year", year)
                .getUri();
    }

    private URI createExpectedQueryUriForPublisherWithName(String name) {
        return new UriWrapper(serverUriPublisher)
                .addQueryParameter("query", name)
                .getUri();
    }

    private URI mockedPublicationChannelsReturnsUri(URI queryUri) {
        var uri = randomUri();
        ArrayNode publicationChannelsResponseBody = createPublicationChannelsResponseWithUri(uri);
        stubFor(get(START_OF_QUERY + queryUri
                .getQuery())
                .willReturn(aResponse().withBody(publicationChannelsResponseBody
                        .toPrettyString()).withStatus(HttpURLConnection.HTTP_OK)));
        return uri;
    }

    private void startWiremockServer() {
        httpServer = new WireMockServer(options().dynamicHttpsPort());
        httpServer.start();
        serverUriJournal = URI.create(httpServer.baseUrl());
        serverUriPublisher = URI.create(httpServer.baseUrl());
    }

    private ArrayNode createPublicationChannelsResponseWithUri(URI uri) {
        var publicationChannelsResponseBodyElement = dtoObjectMapper.createObjectNode();
        publicationChannelsResponseBodyElement.put("id", uri.toString());

        var publicationChannelsResponseBody = dtoObjectMapper.createArrayNode();
        publicationChannelsResponseBody.add(publicationChannelsResponseBodyElement);

        return publicationChannelsResponseBody;
    }

    private AdditionalIdentifier[] convertScopusIdentifiersToNvaAdditionalIdentifiers(
            List<ItemidTp> scopusIdentifiers) {
        return scopusIdentifiers
                .stream()
                .map(idtp -> new AdditionalIdentifier(ADDITIONAL_IDENTIFIERS_SCOPUS_ID_SOURCE_NAME, idtp.getValue()))
                .collect(Collectors.toList())
                .toArray(AdditionalIdentifier[]::new);
    }

    private List<ItemidTp> keepOnlyTheScopusIdentifiers() {
        return scopusData.getDocument()
                .getItem()
                .getItem()
                .getBibrecord()
                .getItemInfo()
                .getItemidlist()
                .getItemid()
                .stream()
                .filter(identifier -> identifier.getIdtype()
                        .equalsIgnoreCase(ScopusConstants.SCOPUS_ITEM_IDENTIFIER_SCP_FIELD_NAME))
                .collect(Collectors.toList());
    }

    private S3Event createS3Event(String expectedObjectKey) {
        var eventNotification = new S3EventNotificationRecord(randomString(),
                randomString(),
                randomString(),
                randomDate(),
                randomString(),
                EMPTY_REQUEST_PARAMETERS,
                EMPTY_RESPONSE_ELEMENTS,
                createS3Entity(expectedObjectKey),
                EMPTY_USER_IDENTITY);
        return new S3Event(List.of(eventNotification));
    }

    private S3Event createS3Event(URI uri) {
        return createS3Event(new UriWrapper(uri).toS3bucketPath().toString());
    }

    private String randomDate() {
        return Instant.now().toString();
    }

    private S3Entity createS3Entity(String expectedObjectKey) {
        var bucket = new S3BucketEntity(randomString(), EMPTY_USER_IDENTITY, randomString());
        var object = new S3ObjectEntity(expectedObjectKey, SOME_FILE_SIZE, randomString(), randomString(),
                randomString());
        var schemaVersion = randomString();
        return new S3Entity(randomString(), bucket, object, schemaVersion);
    }

    private static class FakeS3ClientThrowingException extends FakeS3Client {

        private final String expectedErrorMessage;

        public FakeS3ClientThrowingException(String expectedErrorMessage) {
            super();
            this.expectedErrorMessage = expectedErrorMessage;
        }

        @Override
        public <ReturnT> ReturnT getObject(GetObjectRequest getObjectRequest,
                                           ResponseTransformer<GetObjectResponse, ReturnT> responseTransformer) {
            throw new RuntimeException(expectedErrorMessage);
        }
    }
}