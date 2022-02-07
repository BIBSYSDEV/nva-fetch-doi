package no.sikt.nva.scopus;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.anyUrl;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;
import static no.sikt.nva.scopus.ScopusConstants.ADDITIONAL_IDENTIFIERS_SCOPUS_ID_SOURCE_NAME;
import static no.sikt.nva.scopus.ScopusConstants.DOI_OPEN_URL_FORMAT;
import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static no.unit.nva.testutils.RandomDataGenerator.randomUri;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalToObject;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.startsWith;
import static org.hamcrest.Matchers.stringContainsInOrder;
import static org.hamcrest.core.IsInstanceOf.instanceOf;
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
import java.util.List;
import no.sikt.nva.testing.http.WiremockHttpClient;
import no.unit.nva.commons.json.JsonUtils;
import no.unit.nva.metadata.CreatePublicationRequest;
import no.unit.nva.metadata.service.MetadataService;
import no.unit.nva.model.AdditionalIdentifier;
import no.unit.nva.model.contexttypes.Journal;
import no.unit.nva.model.contexttypes.UnconfirmedJournal;
import no.unit.nva.s3.S3Driver;
import no.unit.nva.stubs.FakeS3Client;
import nva.commons.core.ioutils.IoUtils;
import nva.commons.core.paths.UnixPath;
import nva.commons.core.paths.UriWrapper;
import nva.commons.logutils.LogUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.core.sync.ResponseTransformer;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;

class ScopusHandlerTest {

    public static final Context CONTEXT = mock(Context.class);
    public static final RequestParametersEntity EMPTY_REQUEST_PARAMETERS = null;
    public static final ResponseElementsEntity EMPTY_RESPONSE_ELEMENTS = null;
    public static final UserIdentityEntity EMPTY_USER_IDENTITY = null;
    public static final long SOME_FILE_SIZE = 100L;
    public static final String HARD_CODED_JOURNAL_NAME_IN_RESOURCE_FILE = "Edinburgh Journal of Botany";
    public static final URI HARD_CODED_JOURNAL_URI =
        URI.create("https://api.dev.nva.aws.unit.no/publication-channels/journal/440074/2010");
    private static final String EXPECTED_RESULTS_PATH = "expectedResults";
    private static final String IDENTITY_FIELD_NAME = "identity";
    private static final String NAME_FIELD_NAME = "name";
    private static final String SEQUENCE_FIELD_NAME = "sequence";
    private static final String SCOPUS_XML_0000469852 = "2-s2.0-0000469852.xml";
    private static final String SCP_ID_IN_0000469852 = "0000469852";
    private static final String DOI_IN_0000469852 = "10.1017/S0960428600000743";
    private static final String SCOPUS_XML_0018132378 = "2-s2.0-0018132378.xml";
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
    private static final String SCOPUS_XML_0000833530 = "2-s2.0-0000833530.xml";
    private static final String XML_ENCODING_DECLARATION = "<?xml version=\"1.0\" encoding=\"UTF-8\" "
                                                           + "standalone=\"yes\"?>";
    private static final String HARDCODED_EXPECTED_TITLE_NAMESPACE = "<titletextTp xml:lang=\"eng\" "
                                                                     + "language=\"English\" original=\"y\"";
    private static final String HARDCODED_EXPECTED_TITLE_IN_0000833530 = "Measurement of A\n"
                                                                         + "    <sup>bb</sup>\n"
                                                                         + "    <inf>FB</inf> in hadronic Z decays "
                                                                         + "using a jet charge technique</titletextTp>";
    private static final String SCOPUS_XML_85114653695 = "2-s2.0-85114653695.xml";
    private static final String CONTRIBUTOR_1_NAME_IN_85114653695 = "Morra A.";
    private static final String CONTRIBUTOR_2_NAME_IN_85114653695 = "Escala-Garcia M.";
    private static final String CONTRIBUTOR_151_NAME_IN_85114653695 = "NBCS Collaborators";
    private static final int CONTRIBUTOR_1_SEQUENCE_NUMBER = 1;
    private static final int CONTRIBUTOR_2_SEQUENCE_NUMBER = 2;
    private static final int CONTRIBUTOR_151_SEQUENCE_NUMBER = 151;
    private static final String FILENAME_EXPECTED_ABSTRACT_IN_0000469852 = "expectedAbstract.txt";
    private static final String EXPECTED_ABSTRACT_NAME_SPACE = "<abstractTp";
    private FakeS3Client s3Client;
    private S3Driver s3Driver;
    private ScopusHandler scopusHandler;
    private WireMockServer httpServer;
    private URI serverUri;
    private MetadataService metadataService;

    @BeforeEach
    public void init() {
        s3Client = new FakeS3Client();
        s3Driver = new S3Driver(s3Client, "ignoredValue");
        startWiremockServer();
        HttpClient httpClient = WiremockHttpClient.create();
        metadataService = new MetadataService(httpClient, serverUri);
        scopusHandler = new ScopusHandler(s3Client, metadataService);
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
        scopusHandler = new ScopusHandler(s3Client, metadataService);
        var appender = LogUtils.getTestingAppenderForRootLogger();
        assertThrows(RuntimeException.class, () -> scopusHandler.handleRequest(s3Event, CONTEXT));
        assertThat(appender.getMessages(), containsString(expectedMessage));
    }

    @Test
    void shouldExtractScopusIdentifierAndPlaceItInsideAdditionalIdentifiersObject() throws IOException {
        var scopusFile = IoUtils.stringFromResources(Path.of(SCOPUS_XML_0000469852));
        var uri = s3Driver.insertFile(UnixPath.of(randomString()), scopusFile);
        var s3Event = createS3Event(uri);
        var createPublicationRequest = scopusHandler.handleRequest(s3Event, CONTEXT);
        var actualAdditionalIdentifiers = createPublicationRequest.getAdditionalIdentifiers();
        var expectedIdentifier =
            new AdditionalIdentifier(ADDITIONAL_IDENTIFIERS_SCOPUS_ID_SOURCE_NAME, SCP_ID_IN_0000469852);
        assertThat(actualAdditionalIdentifiers, contains(expectedIdentifier));
    }

    @Test
    void shouldExtractDoiAndPlaceItInsideReferenceObject() throws IOException {
        var scopusFile = IoUtils.stringFromResources(Path.of(SCOPUS_XML_0000469852));
        var uri = s3Driver.insertFile(UnixPath.of(randomString()), scopusFile);
        var s3Event = createS3Event(uri);
        var createPublicationRequest = scopusHandler.handleRequest(s3Event, CONTEXT);
        var expectedURI = UriWrapper.fromUri(DOI_OPEN_URL_FORMAT).addChild(DOI_IN_0000469852).getUri();
        assertThat(createPublicationRequest.getEntityDescription().getReference().getDoi(), equalToObject(expectedURI));
    }

    @Test
    void shouldReturnCreatePublicationRequestWithMainTitle() throws IOException {
        var scopusFile = IoUtils.stringFromResources(Path.of(SCOPUS_XML_0000833530));
        var uri = s3Driver.insertFile(UnixPath.of(randomString()), scopusFile);
        S3Event s3Event = createS3Event(uri);
        CreatePublicationRequest createPublicationRequest = scopusHandler.handleRequest(s3Event, CONTEXT);
        String actualMainTitle = createPublicationRequest.getEntityDescription().getMainTitle();
        assertThat(actualMainTitle,
                   stringContainsInOrder(XML_ENCODING_DECLARATION,
                                         HARDCODED_EXPECTED_TITLE_NAMESPACE,
                                         HARDCODED_EXPECTED_TITLE_IN_0000833530));
    }

    @Test
    void shouldExtractContributorsNamesAndSequenceNumberCorrectly() throws IOException {
        var scopusFile = IoUtils.stringFromResources(Path.of(SCOPUS_XML_85114653695));
        var uri = s3Driver.insertFile(UnixPath.of(randomString()), scopusFile);
        S3Event s3Event = createS3Event(uri);
        CreatePublicationRequest createPublicationRequest = scopusHandler.handleRequest(s3Event, CONTEXT);
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
        var uri = s3Driver.insertFile(UnixPath.of(randomString()), scopusFile);
        S3Event s3Event = createS3Event(uri);
        CreatePublicationRequest createPublicationRequest = scopusHandler.handleRequest(s3Event, CONTEXT);
        String actualKeywords = createPublicationRequest.getAuthorKeywordsXmlFormat();
        assertThat(actualKeywords, stringContainsInOrder(
            XML_ENCODING_DECLARATION,
            AUTHOR_KEYWORD_NAME_SPACE,
            HARDCODED_KEYWORDS_0000469852));
    }

    @Test
    void shouldReturnCreatePublicationRequestWithUnconfirmedPublicationContextWhenEventWithS3UriThatPointsToScopusXml()
        throws IOException {
        var scopusFile = IoUtils.stringFromResources(Path.of("2-s2.0-0000469852.xml"));
        var uri = s3Driver.insertFile(UnixPath.of(randomString()), scopusFile);
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
        var uri = s3Driver.insertFile(UnixPath.of(randomString()), scopusFile);
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
        var uri = s3Driver.insertFile(UnixPath.of(randomString()), scopusFile);
        var s3Event = createS3Event(uri);
        var exception = assertThrows(RuntimeException.class, () -> {
            scopusHandler.handleRequest(s3Event, CONTEXT);
        });
        var expectedMessage = "no.unit.nva.model.exceptions.InvalidIssnException: The ISSN";
        var actualMessage = exception.getMessage();
        assertThat(actualMessage, startsWith(expectedMessage));
    }

    @Test
    void shouldReturnDefaultPublicationContextWhenEventWithS3UriThatPointsToScopusXmlWithoutPrintIssn()
        throws IOException {
        var scopusFile = IoUtils.stringFromResources(Path.of("2-s2.0-0000469852.xml"));
        scopusFile = scopusFile.replace("<issn type=\"print\">09604286</issn>", "");
        var uri = s3Driver.insertFile(UnixPath.of(randomString()), scopusFile);
        var s3Event = createS3Event(uri);
        var createPublicationRequest = scopusHandler.handleRequest(s3Event, CONTEXT);
        var actualPublicationContext = createPublicationRequest.getEntityDescription().getReference()
            .getPublicationContext();
        assertThat(actualPublicationContext, instanceOf(UnconfirmedJournal.class));
        var actualJournalName = ((UnconfirmedJournal) actualPublicationContext).getTitle();
        assertThat(actualJournalName, is(HARD_CODED_JOURNAL_NAME_IN_RESOURCE_FILE));
    }

    @Test
    void shouldReturnCreatePublicationRequestWithJournalWhenEventWithS3UriThatPointsToScopusXmlWhereSourceTitleIsInNsd()
        throws IOException {

        URI expectedJournalUri = mockedPublicationChannelsReturnsJournalUri();

        var scopusFile = IoUtils.stringFromResources(Path.of("2-s2.0-0000469852.xml"));
        scopusFile = scopusFile.replace("<xocs:pub-year>1993</xocs:pub-year>",
                                        "<xocs:pub-year>2010</xocs:pub-year>");
        var uri = s3Driver.insertFile(UnixPath.of(randomString()), scopusFile);
        var s3Event = createS3Event(uri);
        var createPublicationRequest = scopusHandler.handleRequest(s3Event, CONTEXT);
        var actualPublicationContext = createPublicationRequest.getEntityDescription().getReference()
            .getPublicationContext();
        assertThat(actualPublicationContext, instanceOf(Journal.class));
        var actualJournalName = ((Journal) actualPublicationContext).getId().toString();
        assertThat(actualJournalName, is(expectedJournalUri.toString()));
    }

    @Test
    void shouldReturnDefaultPublicationContextWhenEventWithS3UriThatPointsToScopusXmlWithoutKnownSourceType()
        throws IOException {
        var scopusFile = IoUtils.stringFromResources(Path.of("2-s2.0-0000469852.xml"));
        //Todo: this is actually not perfect as there is a chance my random string starts with a 'j' ...
        var randomChar = randomString().substring(0, 1);
        scopusFile = scopusFile.replace("<xocs:srctype>j</xocs:srctype>", "<xocs:srctype>" + randomChar
                                                                          + "</xocs:srctype>");
        var uri = s3Driver.insertFile(UnixPath.of(randomString()), scopusFile);
        var s3Event = createS3Event(uri);
        var createPublicationRequest = scopusHandler.handleRequest(s3Event, CONTEXT);
        var actualPublicationContext = createPublicationRequest.getEntityDescription().getReference()
            .getPublicationContext();
        assertThat(actualPublicationContext, is(ScopusConstants.EMPTY_PUBLICATION_CONTEXT));
    }

    @Test
    void shouldExtractAuthorKeyWordsAsPlainText() throws IOException {
        var scopusFile = IoUtils.stringFromResources(Path.of(SCOPUS_XML_0018132378));
        var uri = s3Driver.insertFile(UnixPath.of(randomString()), scopusFile);
        S3Event s3Event = createS3Event(uri);
        CreatePublicationRequest createPublicationRequest = scopusHandler.handleRequest(s3Event, CONTEXT);
        var actualPlaintextKeyWords = createPublicationRequest.getEntityDescription().getTags();
        assertThat(actualPlaintextKeyWords, allOf(
            hasItem(HARDCODED_EXPECTED_KEYWORD_1_IN_0000469852),
            hasItem(HARDCODED_EXPECTED_KEYWORD_2_IN_0000469852),
            hasItem(HARDCODED_EXPECTED_KEYWORD_3_IN_0000469852)));
    }

    @Test
    void shouldExtractMainAbstractAsXML() throws IOException {
        var scopusFile = IoUtils.stringFromResources(Path.of(SCOPUS_XML_0000469852));
        var uri = s3Driver.insertFile(UnixPath.of(randomString()), scopusFile);
        S3Event s3Event = createS3Event(uri);
        CreatePublicationRequest createPublicationRequest = scopusHandler.handleRequest(s3Event, CONTEXT);
        var actualMainAbstract = createPublicationRequest.getEntityDescription().getAbstract();
        String expectedAbstract =
            IoUtils.stringFromResources(Path.of(EXPECTED_RESULTS_PATH, FILENAME_EXPECTED_ABSTRACT_IN_0000469852));
        assertThat(actualMainAbstract, stringContainsInOrder(XML_ENCODING_DECLARATION,
                                                             EXPECTED_ABSTRACT_NAME_SPACE,
                                                             expectedAbstract));
    }

    private void startWiremockServer() {
        httpServer = new WireMockServer(options().dynamicHttpsPort());
        httpServer.start();
        serverUri = URI.create(httpServer.baseUrl());
    }

    private URI mockedPublicationChannelsReturnsJournalUri() {
        var journalUri = randomUri();
        ArrayNode publicationChannelsResponseBody = publicationChannelsResponseWithJournalUri(journalUri);
        stubFor(get(anyUrl()).willReturn(aResponse()
                                             .withBody(publicationChannelsResponseBody.toPrettyString())
                                             .withStatus(HttpURLConnection.HTTP_OK)));
        return journalUri;
    }

    private ArrayNode publicationChannelsResponseWithJournalUri(URI journalUri) {
        var publicationChannelsResponseBodyElement = JsonUtils.dtoObjectMapper.createObjectNode();
        publicationChannelsResponseBodyElement.put("id", journalUri.toString());

        var publicationChannelsResponseBody = JsonUtils.dtoObjectMapper.createArrayNode();
        publicationChannelsResponseBody.add(publicationChannelsResponseBodyElement);
        return publicationChannelsResponseBody;
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