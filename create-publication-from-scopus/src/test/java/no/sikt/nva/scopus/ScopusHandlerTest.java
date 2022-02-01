package no.sikt.nva.scopus;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.events.S3Event;
import com.amazonaws.services.lambda.runtime.events.models.s3.S3EventNotification.RequestParametersEntity;
import com.amazonaws.services.lambda.runtime.events.models.s3.S3EventNotification.ResponseElementsEntity;
import com.amazonaws.services.lambda.runtime.events.models.s3.S3EventNotification.S3BucketEntity;
import com.amazonaws.services.lambda.runtime.events.models.s3.S3EventNotification.S3Entity;
import com.amazonaws.services.lambda.runtime.events.models.s3.S3EventNotification.S3EventNotificationRecord;
import com.amazonaws.services.lambda.runtime.events.models.s3.S3EventNotification.S3ObjectEntity;
import com.amazonaws.services.lambda.runtime.events.models.s3.S3EventNotification.UserIdentityEntity;
import no.unit.nva.metadata.CreatePublicationRequest;
import no.unit.nva.model.AdditionalIdentifier;
import no.unit.nva.s3.S3Driver;
import no.unit.nva.stubs.FakeS3Client;
import nva.commons.core.ioutils.IoUtils;
import nva.commons.core.paths.UnixPath;
import nva.commons.core.paths.UriWrapper;
import nva.commons.logutils.LogUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.core.sync.ResponseTransformer;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;

import static no.sikt.nva.scopus.ScopusConstants.ADDITIONAL_IDENTIFIERS_SCOPUS_ID_SOURCE_NAME;
import static no.sikt.nva.scopus.ScopusConstants.DOI_OPEN_URL_FORMAT;
import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalToObject;
import static org.hamcrest.Matchers.stringContainsInOrder;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.core.StringContains.containsString;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;

class ScopusHandlerTest {

    public static final Context CONTEXT = mock(Context.class);
    public static final RequestParametersEntity EMPTY_REQUEST_PARAMETERS = null;
    public static final ResponseElementsEntity EMPTY_RESPONSE_ELEMENTS = null;
    public static final UserIdentityEntity EMPTY_USER_IDENTITY = null;
    public static final long SOME_FILE_SIZE = 100L;
    private FakeS3Client s3Client;
    private S3Driver s3Driver;
    private ScopusHandler scopusHandler;
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

    @BeforeEach
    public void init() {
        s3Client = new FakeS3Client();
        s3Driver = new S3Driver(s3Client, "ignoredValue");
        scopusHandler = new ScopusHandler(s3Client);
    }

    @Test
    void shouldLogExceptionMessageWhenExceptionOccurs() {
        var appender = LogUtils.getTestingAppenderForRootLogger();
        S3Event s3Event = createS3Event(randomString());
        var expectedMessage = randomString();
        s3Client = new FakeS3ClientThrowingException(expectedMessage);
        scopusHandler = new ScopusHandler(s3Client);
        assertThrows(RuntimeException.class, () -> scopusHandler.handleRequest(s3Event, CONTEXT));
        assertThat(appender.getMessages(), containsString(expectedMessage));
    }

    @Test
    void shouldExtractScopusIdentifierAndPlaceItInsideAdditionalIdentifiersObject() throws IOException {
        var scopusFile = IoUtils.stringFromResources(Path.of(SCOPUS_XML_0000469852));
        var uri = s3Driver.insertFile(UnixPath.of(randomString()), scopusFile);
        S3Event s3Event = createS3Event(uri);
        CreatePublicationRequest createPublicationRequest = scopusHandler.handleRequest(s3Event, CONTEXT);
        var actualAdditionalIdentifiers = createPublicationRequest.getAdditionalIdentifiers();
        var expectedIdentifier =
            new AdditionalIdentifier(ADDITIONAL_IDENTIFIERS_SCOPUS_ID_SOURCE_NAME, SCP_ID_IN_0000469852);
        assertThat(actualAdditionalIdentifiers, contains(expectedIdentifier));
    }

    @Test
    void shouldExtractDoiAndPlaceItInsideReferenceObject() throws IOException {
        var scopusFile = IoUtils.stringFromResources(Path.of(SCOPUS_XML_0000469852));
        var uri = s3Driver.insertFile(UnixPath.of(randomString()), scopusFile);
        S3Event s3Event = createS3Event(uri);
        CreatePublicationRequest createPublicationRequest = scopusHandler.handleRequest(s3Event, CONTEXT);
        URI expectedURI = new UriWrapper(DOI_OPEN_URL_FORMAT).addChild(DOI_IN_0000469852).getUri();
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
        S3BucketEntity bucket = new S3BucketEntity(randomString(), EMPTY_USER_IDENTITY, randomString());
        S3ObjectEntity object = new S3ObjectEntity(expectedObjectKey, SOME_FILE_SIZE, randomString(), randomString(),
                                                   randomString());
        String schemaVersion = randomString();
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