package no.sikt.nva.scopus;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;
import static java.util.Objects.nonNull;
import static no.sikt.nva.scopus.ScopusConstants.ADDITIONAL_IDENTIFIERS_SCOPUS_ID_SOURCE_NAME;
import static no.sikt.nva.scopus.ScopusConstants.AFFILIATION_DELIMITER;
import static no.sikt.nva.scopus.ScopusConstants.ISSN_TYPE_ELECTRONIC;
import static no.sikt.nva.scopus.ScopusConstants.ISSN_TYPE_PRINT;
import static no.sikt.nva.scopus.ScopusConstants.ORCID_DOMAIN_URL;
import static no.sikt.nva.scopus.conversion.ContributorExtractor.NAME_DELIMITER;
import static no.sikt.nva.scopus.conversion.PublicationContextCreator.UNSUPPORTED_SOURCE_TYPE;
import static no.sikt.nva.scopus.test.utils.ScopusGenerator.randomYear;
import static no.unit.nva.commons.json.JsonUtils.dtoObjectMapper;
import static no.unit.nva.language.LanguageConstants.BOKMAAL;
import static no.unit.nva.language.LanguageConstants.ENGLISH;
import static no.unit.nva.language.LanguageConstants.FRENCH;
import static no.unit.nva.language.LanguageConstants.ITALIAN;
import static no.unit.nva.testutils.RandomDataGenerator.randomDoi;
import static no.unit.nva.testutils.RandomDataGenerator.randomInteger;
import static no.unit.nva.testutils.RandomDataGenerator.randomIsbn13;
import static no.unit.nva.testutils.RandomDataGenerator.randomIssn;
import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static nva.commons.core.StringUtils.isNotBlank;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.equalToObject;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.isA;
import static org.hamcrest.Matchers.startsWith;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.hamcrest.core.IsInstanceOf.instanceOf;
import static org.hamcrest.core.IsNot.not;
import static org.hamcrest.core.IsNull.nullValue;
import static org.hamcrest.core.StringContains.containsString;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
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
import jakarta.xml.bind.JAXBElement;
import java.io.IOException;
import java.io.Serializable;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.text.ParseException;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import javax.xml.namespace.QName;
import no.scopus.generated.AffiliationTp;
import no.scopus.generated.AuthorGroupTp;
import no.scopus.generated.AuthorTp;
import no.scopus.generated.BibrecordTp;
import no.scopus.generated.CitationTitleTp;
import no.scopus.generated.CitationtypeAtt;
import no.scopus.generated.CollaborationTp;
import no.scopus.generated.DocTp;
import no.scopus.generated.HeadTp;
import no.scopus.generated.InfTp;
import no.scopus.generated.ItemTp;
import no.scopus.generated.ItemidTp;
import no.scopus.generated.OrganizationTp;
import no.scopus.generated.OrigItemTp;
import no.scopus.generated.SourcetypeAtt;
import no.scopus.generated.SupTp;
import no.scopus.generated.TitletextTp;
import no.scopus.generated.YesnoAtt;
import no.sikt.nva.scopus.conversion.PublicationInstanceCreator;
import no.sikt.nva.scopus.exception.UnsupportedCitationTypeException;
import no.sikt.nva.scopus.exception.UnsupportedSrcTypeException;
import no.sikt.nva.scopus.test.utils.ContentWrapper;
import no.sikt.nva.scopus.test.utils.ScopusGenerator;
import no.sikt.nva.testing.http.WiremockHttpClient;
import no.unit.nva.doi.models.Doi;
import no.unit.nva.events.models.EventReference;
import no.unit.nva.metadata.CreatePublicationRequest;
import no.unit.nva.metadata.service.MetadataService;
import no.unit.nva.model.AdditionalIdentifier;
import no.unit.nva.model.Contributor;
import no.unit.nva.model.Organization;
import no.unit.nva.model.contexttypes.Book;
import no.unit.nva.model.contexttypes.Chapter;
import no.unit.nva.model.contexttypes.Journal;
import no.unit.nva.model.contexttypes.Publisher;
import no.unit.nva.model.contexttypes.Report;
import no.unit.nva.model.contexttypes.Series;
import no.unit.nva.model.contexttypes.UnconfirmedJournal;
import no.unit.nva.model.contexttypes.UnconfirmedPublisher;
import no.unit.nva.model.contexttypes.UnconfirmedSeries;
import no.unit.nva.model.instancetypes.book.BookMonograph;
import no.unit.nva.model.instancetypes.journal.JournalArticle;
import no.unit.nva.model.instancetypes.journal.JournalCorrigendum;
import no.unit.nva.model.instancetypes.journal.JournalLeader;
import no.unit.nva.model.instancetypes.journal.JournalLetter;
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
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.EnumSource.Mode;
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
    public static final String START_OF_QUERY = "/?";
    private static final String EXPECTED_RESULTS_PATH = "expectedResults";
    private static final String HARDCODED_EXPECTED_KEYWORD_1 = "<sup>64</sup>Cu";
    private static final String HARDCODED_EXPECTED_KEYWORD_2 = "excretion";
    private static final String HARDCODED_EXPECTED_KEYWORD_3 = "sheep";
    private static final String SCOPUS_XML_0000469852 = "2-s2.0-0000469852.xml";
    private static final String PUBLICATION_DAY_FIELD_NAME = "day";
    private static final String PUBLICATION_MONTH_FIELD_NAME = "month";
    private static final String PUBLICATION_YEAR_FIELD_NAME = "year";
    private static final String FILENAME_EXPECTED_ABSTRACT_IN_0000469852 = "expectedAbstract.txt";
    public static final String EXPECTED_CONTENT_STRING_TXT = "expectedContentString.txt";
    public static final String INF_CLASS_NAME = "inf";
    public static final String SUP_CLASS_NAME = "sup";
    public static final String INVALID_ISSN = "096042";
    public static final String VALID_ISSN = "0960-4286";
    public static final String LANGUAGE_ENG = "eng";

    private FakeS3Client s3Client;
    private S3Driver s3Driver;
    private ScopusHandler scopusHandler;
    private WireMockServer httpServer;
    private URI serverUriJournal;
    private URI serverUriPublisher;
    private MetadataService metadataService;

    private HttpClient httpClient;
    private FakeEventBridgeClient eventBridgeClient;
    private ScopusGenerator scopusData;

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
        scopusData = ScopusGenerator.createScopusGeneratorWithSpecificDoi(randomDoi());
        var expectedURI = Doi.fromDoiIdentifier(scopusData.getDocument().getMeta().getDoi()).getUri();
        var s3Event = createNewScopusPublicationEvent();
        var createPublicationRequest = scopusHandler.handleRequest(s3Event, CONTEXT);
        assertThat(createPublicationRequest.getEntityDescription().getReference().getDoi(), equalToObject(expectedURI));
    }

    @Test
    void shouldReturnCreatePublicationRequestWithMainTitle() throws IOException {
        var s3Event = createNewScopusPublicationEvent();
        var titleObject = extractTitle(scopusData);
        var expectedTitleString = expectedTitle(titleObject);
        var createPublicationRequest = scopusHandler.handleRequest(s3Event, CONTEXT);
        var actualMainTitle = createPublicationRequest.getEntityDescription().getMainTitle();
        assertThat(actualMainTitle, is(equalTo(expectedTitleString)));
    }

    @Test
    void shouldConvertSpecifiedSupAndInfContentToString() throws IOException {
        scopusData = ScopusGenerator.createWithSpecifiedSupAndInfContent(createContentWithSupAndInfTags());
        var s3Event = createNewScopusPublicationEvent();
        var expectedTitleString =
            IoUtils.stringFromResources(Path.of(EXPECTED_RESULTS_PATH, EXPECTED_CONTENT_STRING_TXT));
        var createPublicationRequest = scopusHandler.handleRequest(s3Event, CONTEXT);
        var actualMainTitle = createPublicationRequest.getEntityDescription().getMainTitle();
        assertThat(actualMainTitle, is(equalTo(expectedTitleString)));
    }

    private ContentWrapper createContentWithSupAndInfTags() {
        return new ContentWrapper(contentWithSupInftagsScopus14244261628());
    }

    private static List<Serializable> contentWithSupInftagsScopus14244261628() {
        // This is an actual title from doi: 10.1016/j.nuclphysbps.2005.01.029
        return List.of("Non-factorizable contributions to B",
                       generateInf("d"),
                       generateSup("0"),
                       " - D",
                       generateInf("s"),
                       "(*) D",
                       generateInf("s"),
                       "(*)");
    }

    private static JAXBElement<InfTp> generateInf(Serializable content) {
        InfTp infTp = new InfTp();
        infTp.getContent().add(content);
        return new JAXBElement<>(new QName(INF_CLASS_NAME), InfTp.class, infTp);
    }

    private static JAXBElement<SupTp> generateSup(Serializable content) {
        SupTp supTp = new SupTp();
        supTp.getContent().add(content);
        return new JAXBElement<>(new QName(SUP_CLASS_NAME), SupTp.class, supTp);
    }

    @Test
    void shouldExtractContributorsNamesAndSequenceNumberCorrectly() throws IOException {
        var authors = keepOnlyTheAuthors();
        var collaborations = keepOnlyTheCollaborations();
        var s3Event = createNewScopusPublicationEvent();
        var createPublicationRequest = scopusHandler.handleRequest(s3Event, CONTEXT);
        var actualContributors = createPublicationRequest.getEntityDescription().getContributors();
        authors.forEach(author -> checkAuthorName(author, actualContributors));
        collaborations.forEach(collaboration -> checkCollaborationName(collaboration, actualContributors));
    }

    @Test
    void shouldExtractContributorAffiliation() throws IOException {
        var authorsGroups = scopusData.getDocument().getItem().getItem().getBibrecord().getHead().getAuthorGroup();
        var authors = keepOnlyTheAuthors();
        var s3Event = createNewScopusPublicationEvent();
        var createPublicationRequest = scopusHandler.handleRequest(s3Event, CONTEXT);
        var actualContributors = createPublicationRequest.getEntityDescription().getContributors();
        authors.forEach(author -> checkAffiliationForAuthor(author, actualContributors, authorsGroups));
    }

    private void checkAffiliationForAuthor(AuthorTp author, List<Contributor> actualContributors,
                                           List<AuthorGroupTp> authorGroupTps) {
        //when we remove duplicates this will have better CPU performance.
        var expectedAffiliationsNames = getAffiliationNameForSequenceNumber(authorGroupTps, author.getSeq());
        var actualAffiliationNames = findContributorsBySequence(author.getSeq(), actualContributors)
            .stream()
            .map(Contributor::getAffiliations)
            .flatMap(Collection::stream)
            .map(Organization::getLabels)
            .map(Map::values)
            .flatMap(Collection::stream)
            .collect(Collectors.toList());
        assertThat(actualAffiliationNames, containsInAnyOrder(expectedAffiliationsNames.toArray()));
    }

    private List<String> getAffiliationNameForSequenceNumber(List<AuthorGroupTp> authorGroupTps,
                                                             String sequenceNumber) {
        return authorGroupTps
            .stream()
            .filter(authorGroupTp -> authorGroupContainAuthorWithSequenceNumber(authorGroupTp, sequenceNumber))
            .collect(Collectors.toList())
            .stream()
            .map(this::expectedAffiliationName)
            .collect(Collectors.toList());
    }

    private String expectedAffiliationName(AuthorGroupTp authorGroupsWithAuthorsWithSequenceNumber) {
        return authorGroupsWithAuthorsWithSequenceNumber
            .getAffiliation()
            .getOrganization()
            .stream()
            .map(organizationTp -> organizationTp.getContent()
                .stream()
                .map(Object::toString)
                .collect(Collectors.joining()))
            .collect(Collectors.joining(AFFILIATION_DELIMITER));
    }

    private boolean authorGroupContainAuthorWithSequenceNumber(AuthorGroupTp authorGroupTp, String sequenceNumber) {
        return keepOnlyTheAuthors(authorGroupTp).stream()
            .anyMatch(authorTp -> sequenceNumber.equals(authorTp.getSeq()));
    }

    @Test
    void shouldReturnCreatePublicationRequestWithUnconfirmedPublicationContextWhenEventWithS3UriThatPointsToScopusXml()
        throws IOException {
        scopusData = ScopusGenerator.createWithSpecifiedSrcType(SourcetypeAtt.J);
        var s3Event = createNewScopusPublicationEvent();
        var createPublicationRequest = scopusHandler.handleRequest(s3Event, CONTEXT);
        var actualPublicationContext = createPublicationRequest.getEntityDescription().getReference()
            .getPublicationContext();
        assertThat(actualPublicationContext, instanceOf(UnconfirmedJournal.class));
    }

    @Test
    void shouldReturnCreatePublicationRequestWithUnconfirmedPublicationContextWhenEventS3UriScopusXmlWithValidIssn()
        throws IOException {
        scopusData = ScopusGenerator.createWithSpecifiedSrcType(SourcetypeAtt.J);
        scopusData.clearIssn();
        scopusData.addIssn(VALID_ISSN, ISSN_TYPE_PRINT);
        var s3Event = createNewScopusPublicationEvent();
        var createPublicationRequest = scopusHandler.handleRequest(s3Event, CONTEXT);
        var actualPublicationContext = createPublicationRequest.getEntityDescription().getReference()
            .getPublicationContext();
        assertThat(actualPublicationContext, instanceOf(UnconfirmedJournal.class));
    }

    @Test
    void shouldReturnCreatePublicationRequestWithUnconfirmedPublicationContextWhenEventS3UriScopusXmlWithInvalidIssn()
        throws IOException {
        scopusData = ScopusGenerator.createWithSpecifiedSrcType(SourcetypeAtt.J);
        scopusData.clearIssn();
        scopusData.addIssn(INVALID_ISSN, ISSN_TYPE_PRINT);
        var s3Event = createNewScopusPublicationEvent();
        Executable action = () -> scopusHandler.handleRequest(s3Event, CONTEXT);
        var exception = assertThrows(RuntimeException.class, action);
        var expectedMessage = "no.unit.nva.model.exceptions.InvalidIssnException: The ISSN";
        var actualMessage = exception.getMessage();
        assertThat(actualMessage, startsWith(expectedMessage));
    }

    @Test
    void shouldReturnDefaultPublicationContextWhenEventWithS3UriThatPointsToScopusXmlWithoutPrintIssn()
        throws IOException {
        scopusData = ScopusGenerator.createWithSpecifiedSrcType(SourcetypeAtt.J);
        var s3Event = createNewScopusPublicationEvent();
        var createPublicationRequest = scopusHandler.handleRequest(s3Event, CONTEXT);
        var actualPublicationContext = createPublicationRequest.getEntityDescription().getReference()
            .getPublicationContext();
        assertThat(actualPublicationContext, instanceOf(UnconfirmedJournal.class));
    }

    @Test
    void shouldReturnPublicationContextBookWithUnconfirmedPublisherWhenEventWithS3UriThatPointsToScopusXmlWithSrctypeB()
        throws IOException {
        scopusData = ScopusGenerator.createWithSpecifiedSrcType(SourcetypeAtt.B);
        var s3Event = createNewScopusPublicationEvent();
        var createPublicationRequest = scopusHandler.handleRequest(s3Event, CONTEXT);
        var actualPublicationContext = createPublicationRequest.getEntityDescription().getReference()
            .getPublicationContext();
        assertThat(actualPublicationContext, instanceOf(Book.class));
        var actualPublisher = ((Book) actualPublicationContext).getPublisher();
        assertThat(actualPublisher, instanceOf(UnconfirmedPublisher.class));
        var expectedPublishername = scopusData.getDocument().getItem().getItem().getBibrecord().getHead().getSource()
            .getPublisher().get(0).getPublishername();
        var actualPublisherName = ((UnconfirmedPublisher) actualPublisher).getName();
        assertThat(actualPublisherName, is(expectedPublishername));
    }

    @Test
    void shouldReturnPublicationContextBookWithConfirmedPublisherWhenScopusXmlHasSrctypeBandIsNotAchapter()
        throws IOException {
        scopusData = ScopusGenerator.createWithSpecifiedSrcType(SourcetypeAtt.B);
        var expectedPublisherName = randomString();
        scopusData.setPublishername(expectedPublisherName);
        var expectedIsbn13 = randomIsbn13();
        scopusData.addIsbn(expectedIsbn13, "13");
        var expectedYear = String.valueOf(randomYear());
        scopusData.setPublicationYear(expectedYear);
        var s3Event = createNewScopusPublicationEvent();
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
        var actualIsbnList = ((Book) actualPublicationContext).getIsbnList();
        assertThat(actualIsbnList.size(), is(1));
        assertThat(actualIsbnList, containsInAnyOrder(expectedIsbn13));
    }

    @Test
    void shouldReturnPublicationContextChapterWhenScopusXmlHasCitationTypeChEvenIfSrctypeIsB()
        throws IOException {
        scopusData = ScopusGenerator.create(CitationtypeAtt.CH);
        scopusData.setSrcType(SourcetypeAtt.B);
        var expectedPublisherName = randomString();
        scopusData.setPublishername(expectedPublisherName);
        var s3Event = createNewScopusPublicationEvent();
        var createPublicationRequest = scopusHandler.handleRequest(s3Event, CONTEXT);
        var actualPublicationContext = createPublicationRequest.getEntityDescription().getReference()
            .getPublicationContext();
        assertThat(actualPublicationContext, instanceOf(Chapter.class));
        var actualPartOfUri = ((Chapter) actualPublicationContext).getPartOf();
        assertThat(actualPartOfUri, is(ScopusConstants.DUMMY_URI));
    }

    @Test
    void shouldReturnPublicationContextReportWithConfirmedPublisherWhenEventWithS3UriThatPointsToScopusXmlWithSrctypeR()
        throws IOException {
        scopusData = ScopusGenerator.createWithSpecifiedSrcType(SourcetypeAtt.R);
        var expectedPublisherName = randomString();
        scopusData.setPublishername(expectedPublisherName);
        var s3Event = createNewScopusPublicationEvent();
        var queryUri = createExpectedQueryUriForPublisherWithName(expectedPublisherName);
        var expectedPublisherUri = mockedPublicationChannelsReturnsUri(queryUri);
        var createPublicationRequest = scopusHandler.handleRequest(s3Event, CONTEXT);
        var actualPublicationContext = createPublicationRequest.getEntityDescription().getReference()
            .getPublicationContext();
        assertThat(actualPublicationContext, instanceOf(Book.class));
        var actualPublisher = ((Report) actualPublicationContext).getPublisher();
        assertThat(actualPublisher, instanceOf(Publisher.class));
        var actualPublisherId = ((Publisher) actualPublisher).getId();
        assertThat(actualPublisherId, is(expectedPublisherUri));
    }

    @Test
    void shouldReturnPublicationContextUnconfirmedBookSeriesWhenEventWithS3UriThatPointsToScopusXmlWithSrctypeK()
        throws IOException, ParseException {
        scopusData = ScopusGenerator.createWithSpecifiedSrcType(SourcetypeAtt.K);
        final var expectedYear = String.valueOf(randomYear());
        scopusData.setPublicationYear(expectedYear);
        scopusData.clearIssn();
        final var expectedIssn = randomIssn();
        scopusData.addIssn(expectedIssn, ISSN_TYPE_ELECTRONIC);
        var s3Event = createNewScopusPublicationEvent();
        var createPublicationRequest = scopusHandler.handleRequest(s3Event, CONTEXT);
        var actualPublicationContext = createPublicationRequest.getEntityDescription().getReference()
            .getPublicationContext();
        assertThat(actualPublicationContext, instanceOf(Book.class));
        var actualSeries = ((Book) actualPublicationContext).getSeries();
        assertThat(actualSeries, instanceOf(UnconfirmedSeries.class));
        var actualIssn = ((UnconfirmedSeries) actualSeries).getOnlineIssn();
        assertThat(actualIssn, is(expectedIssn));
    }

    @Test
    void shouldReturnPublicationContextConfirmedBookSeriesWhenEventWithS3UriThatPointsToScopusXmlWithSrctypeK()
        throws IOException, ParseException {
        scopusData = ScopusGenerator.createWithSpecifiedSrcType(SourcetypeAtt.K);
        final var expectedYear = String.valueOf(randomYear());
        scopusData.setPublicationYear(expectedYear);
        scopusData.clearIssn();
        final var expectedIssn = randomIssn();
        scopusData.addIssn(expectedIssn, ISSN_TYPE_ELECTRONIC);
        var uri = s3Driver.insertFile(UnixPath.of(randomString()), scopusData.toXml());
        var s3Event = createS3Event(uri);
        var queryUri = createExpectedQueryUriForJournalWithEIssn(expectedIssn, expectedYear);
        var expectedSeriesUri = mockedPublicationChannelsReturnsUri(queryUri);
        var createPublicationRequest = scopusHandler.handleRequest(s3Event, CONTEXT);
        var actualPublicationContext = createPublicationRequest.getEntityDescription().getReference()
            .getPublicationContext();
        assertThat(actualPublicationContext, instanceOf(Book.class));
        var actualSeries = ((Book) actualPublicationContext).getSeries();
        assertThat(actualSeries, instanceOf(Series.class));
        var actualUri = ((Series) actualSeries).getId();
        assertThat(actualUri, is(expectedSeriesUri));
    }

    @Test
    void shouldReturnCreatePublicationRequestWithJournalWhenEventWithS3UriThatPointsToScopusXmlWhereSourceTitleIsInNsd()
        throws IOException {
        scopusData = ScopusGenerator.createWithSpecifiedSrcType(SourcetypeAtt.J);
        var expectedYear = "2022";
        scopusData.setPublicationYear(expectedYear);
        scopusData.clearIssn();
        var expectedIssn = randomIssn();
        var queryUri = createExpectedQueryUriForJournalWithEIssn(expectedIssn, expectedYear);
        scopusData.addIssn(expectedIssn, ISSN_TYPE_ELECTRONIC);
        var s3Event = createNewScopusPublicationEvent();
        var expectedJournalUri = mockedPublicationChannelsReturnsUri(queryUri);
        var createPublicationRequest = scopusHandler.handleRequest(s3Event, CONTEXT);
        var actualPublicationContext = createPublicationRequest.getEntityDescription().getReference()
            .getPublicationContext();
        assertThat(actualPublicationContext, instanceOf(Journal.class));
        var actualJournalUri = ((Journal) actualPublicationContext).getId();
        assertThat(actualJournalUri, is(expectedJournalUri));
    }

    @Test
    void shouldThrowExceptionWhenSrcTypeIsNotSupported() throws IOException {
        scopusData = ScopusGenerator.createWithSpecifiedSrcType(SourcetypeAtt.X);
        var expectedMessage = String.format(UNSUPPORTED_SOURCE_TYPE, scopusData.getDocument().getMeta().getEid());
        var s3Event = createNewScopusPublicationEvent();
        var appender = LogUtils.getTestingAppenderForRootLogger();
        assertThrows(UnsupportedSrcTypeException.class, () -> scopusHandler.handleRequest(s3Event, CONTEXT));
        assertThat(appender.getMessages(), containsString(expectedMessage));
    }

    @Test
    void shouldExtractAuthorKeyWordsAsPlainText() throws IOException {
        scopusData = ScopusGenerator.createWithSpecifiedSrcType(SourcetypeAtt.J);
        scopusData.clearAuthorKeywords();
        scopusData.addAuthorKeyword(HARDCODED_EXPECTED_KEYWORD_1, LANGUAGE_ENG);
        scopusData.addAuthorKeyword(HARDCODED_EXPECTED_KEYWORD_2, LANGUAGE_ENG);
        scopusData.addAuthorKeyword(HARDCODED_EXPECTED_KEYWORD_3, LANGUAGE_ENG);
        var s3Event = createNewScopusPublicationEvent();
        var createPublicationRequest = scopusHandler.handleRequest(s3Event, CONTEXT);
        var expectedkeywords = List.of(
            HARDCODED_EXPECTED_KEYWORD_1,
            HARDCODED_EXPECTED_KEYWORD_2,
            HARDCODED_EXPECTED_KEYWORD_3);
        var actualPlaintextKeyWords = createPublicationRequest.getEntityDescription().getTags();
        assertThat(actualPlaintextKeyWords, containsInAnyOrder(expectedkeywords.toArray()));
    }

    @Test
    void shouldExtractPublicationDate() throws IOException {
        scopusData = ScopusGenerator.createWithSpecifiedSrcType(SourcetypeAtt.J);
        var year = "1978";
        var month = "02";
        var day = "01";
        scopusData.setPublicationDate(year, month, day);
        var s3Event = createNewScopusPublicationEvent();
        var createPublicationRequest = scopusHandler.handleRequest(s3Event, CONTEXT);
        var actualPublicationDate = createPublicationRequest.getEntityDescription().getDate();
        assertThat(actualPublicationDate, allOf(
            hasProperty(PUBLICATION_DAY_FIELD_NAME, is(day)),
            hasProperty(PUBLICATION_MONTH_FIELD_NAME, is(month)),
            hasProperty(PUBLICATION_YEAR_FIELD_NAME, is(year))));
    }

    @Test
    void shouldExtractMainAbstract() throws IOException {
        var scopusFile = IoUtils.stringFromResources(Path.of(SCOPUS_XML_0000469852));
        var uri = s3Driver.insertFile(randomS3Path(), scopusFile);
        var s3Event = createS3Event(uri);
        var createPublicationRequest = scopusHandler.handleRequest(s3Event, CONTEXT);
        var actualMainAbstract = createPublicationRequest.getEntityDescription().getAbstract();
        var expectedAbstract =
                IoUtils.stringFromResources(Path.of(EXPECTED_RESULTS_PATH, FILENAME_EXPECTED_ABSTRACT_IN_0000469852));
        assertThat(actualMainAbstract, is(equalTo(expectedAbstract)));
    }

    @Test
    void shouldNotThrowExceptionWhenScopusXmlDoesNotContainAbstract() throws IOException {
        scopusData = ScopusGenerator.createWithSpecifiedAbstract(null);
        var event = createNewScopusPublicationEvent();
        assertDoesNotThrow(() -> scopusHandler.handleRequest(event, CONTEXT));
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

    @Test
    void shouldExtractJournalArticleWhenScopusCitationTypeIsArticle() throws IOException {
        scopusData = ScopusGenerator.create(CitationtypeAtt.AR);
        var s3Event = createNewScopusPublicationEvent();
        CreatePublicationRequest createPublicationRequest = scopusHandler.handleRequest(s3Event, CONTEXT);
        var actualPublicationInstance =
            createPublicationRequest.getEntityDescription().getReference().getPublicationInstance();
        assertThat(actualPublicationInstance, isA(JournalArticle.class));
    }

    @Test
    void shouldExtractJournalArticleWhenScopusCitationTypeIsReview() throws IOException {
        scopusData = ScopusGenerator.create(CitationtypeAtt.RE);
        var s3Event = createNewScopusPublicationEvent();
        CreatePublicationRequest createPublicationRequest = scopusHandler.handleRequest(s3Event, CONTEXT);
        var actualPublicationInstance =
            createPublicationRequest.getEntityDescription().getReference().getPublicationInstance();
        assertThat(actualPublicationInstance, isA(JournalArticle.class));
    }

    @Test
    void shouldExtractJournalArticleWhenScopusCitationTypeIsEditorial() throws IOException {
        scopusData = ScopusGenerator.create(CitationtypeAtt.ED);
        var expectedIssue = String.valueOf(randomInteger());
        var expectedVolume = randomString();
        var expectedPages = randomString();
        scopusData.setJournalInfo(expectedVolume, expectedIssue, expectedPages);
        var s3Event = createNewScopusPublicationEvent();
        CreatePublicationRequest createPublicationRequest = scopusHandler.handleRequest(s3Event, CONTEXT);
        var actualPublicationInstance =
            createPublicationRequest.getEntityDescription().getReference().getPublicationInstance();
        assertThat(actualPublicationInstance, isA(JournalLeader.class));
        assertThat(expectedVolume, is(((JournalLeader) actualPublicationInstance).getVolume()));
        assertThat(expectedIssue, is(((JournalLeader) actualPublicationInstance).getIssue()));
    }

    @Test
    void shouldExtractJournalArticleWhenScopusCitationTypeIsErratum() throws IOException {
        scopusData = ScopusGenerator.create(CitationtypeAtt.ER);
        var expectedIssue = String.valueOf(randomInteger());
        var expectedVolume = randomString();
        var expectedPages = randomString();
        scopusData.setJournalInfo(expectedVolume, expectedIssue, expectedPages);
        var s3Event = createNewScopusPublicationEvent();
        CreatePublicationRequest createPublicationRequest = scopusHandler.handleRequest(s3Event, CONTEXT);
        var actualPublicationInstance =
            createPublicationRequest.getEntityDescription().getReference().getPublicationInstance();
        assertThat(actualPublicationInstance, isA(JournalCorrigendum.class));
        assertThat(ScopusConstants.DUMMY_URI, is(((JournalCorrigendum) actualPublicationInstance).getCorrigendumFor()));
    }

    @Test
    void shouldExtractJournalLetterWhenScopusCitationTypeIsLetter() throws IOException {
        scopusData = ScopusGenerator.create(CitationtypeAtt.LE);
        var expectedIssue = String.valueOf(randomInteger());
        var expectedVolume = randomString();
        var expectedPages = randomString();
        scopusData.setJournalInfo(expectedVolume, expectedIssue, expectedPages);
        var s3Event = createNewScopusPublicationEvent();
        CreatePublicationRequest createPublicationRequest = scopusHandler.handleRequest(s3Event, CONTEXT);
        var actualPublicationInstance =
            createPublicationRequest.getEntityDescription().getReference().getPublicationInstance();
        assertThat(actualPublicationInstance, isA(JournalLetter.class));
        assertThat(expectedIssue, is(((JournalLetter) actualPublicationInstance).getIssue()));
    }

    @ParameterizedTest(name = "should not generate CreatePublicationRequest when CitationType is:{0}")
    @EnumSource(
        value = CitationtypeAtt.class,
        names = {"AR", "BK", "CH", "ED", "ER", "LE", "NO", "RE", "SH"},
        mode = Mode.EXCLUDE)
    void shouldNotGenerateCreatePublicationFromUnsupportedPublicationTypes(CitationtypeAtt citationtypeAtt)
        throws IOException {
        scopusData = ScopusGenerator.create(citationtypeAtt);
        // eid is chosen because it seems to match the file name in the bucket.
        var eid = scopusData.getDocument().getMeta().getEid();
        var s3Event = createNewScopusPublicationEvent();
        var expectedMessage = String.format(PublicationInstanceCreator.UNSUPPORTED_CITATION_TYPE_MESSAGE, eid);
        var appender = LogUtils.getTestingAppenderForRootLogger();
        assertThrows(UnsupportedCitationTypeException.class, () -> scopusHandler.handleRequest(s3Event, CONTEXT));
        assertThat(appender.getMessages(), containsString(expectedMessage));
    }

    @Test
    void shouldExtractAuthorOrcidAndSequenceNumber() throws IOException {
        var authors = keepOnlyTheAuthors();
        var s3Event = createNewScopusPublicationEvent();
        var createPublicationRequest = scopusHandler.handleRequest(s3Event, CONTEXT);
        var actualContributors = createPublicationRequest.getEntityDescription().getContributors();
        authors.forEach(author -> checkAuthorOrcidAndSequenceNumber(author, actualContributors));
    }

    @ParameterizedTest(name = "should have PublicationInstace BookMonograph when CitationType is:{0}")
    @EnumSource(
        value = CitationtypeAtt.class,
        names = {"CH", "BK"},
        mode = Mode.INCLUDE)
    void shouldExtractCitationTypesToBookMonographPublicationInstance(CitationtypeAtt citationtypeAtt)
        throws IOException {
        scopusData = ScopusGenerator.create(citationtypeAtt);
        var s3Event = createNewScopusPublicationEvent();
        CreatePublicationRequest createPublicationRequest = scopusHandler.handleRequest(s3Event, CONTEXT);
        var actualPublicationInstance =
            createPublicationRequest.getEntityDescription().getReference().getPublicationInstance();
        assertThat(actualPublicationInstance, isA(BookMonograph.class));
    }

    @Test
    void shouldNotThrowExceptionWhenDoiInScopusIsNull() throws IOException {
        scopusData = ScopusGenerator.createScopusGeneratorWithSpecificDoi(null);
        var s3Event = createNewScopusPublicationEvent();
        assertDoesNotThrow(() -> scopusHandler.handleRequest(s3Event, CONTEXT));
    }

    @Test
    void shouldExtractVolumeIssueAndPageRange() throws IOException {
        scopusData = ScopusGenerator.create(CitationtypeAtt.AR);
        var expectedIssue = String.valueOf(randomInteger());
        var expectedVolume = randomString();
        var expectedPages = randomString();
        scopusData.setJournalInfo(expectedVolume, expectedIssue, expectedPages);
        var s3Event = createNewScopusPublicationEvent();
        var createPublicationRequest = scopusHandler.handleRequest(s3Event, CONTEXT);
        var actualPublicationInstance = (JournalArticle) createPublicationRequest.getEntityDescription().getReference()
                .getPublicationInstance();
        assertThat(actualPublicationInstance.getVolume(), is(expectedVolume));
        assertThat(actualPublicationInstance.getIssue(), is(expectedIssue));
        assertThat(actualPublicationInstance.getPages().getEnd(), is(expectedPages));
    }

    @Test
    void shouldExtractCorrespondingAuthor() throws IOException {
        var authors = keepOnlyTheAuthors();
        var correspondingAuthorTp = authors.get(0);
        scopusData.setCorrespondence(correspondingAuthorTp);
        var s3Event = createNewScopusPublicationEvent();
        var createPublicationRequest = scopusHandler.handleRequest(s3Event, CONTEXT);
        var actualPublicationContributors = createPublicationRequest.getEntityDescription()
                .getContributors();
        var actualCorrespondingContributor = getCorrespondingContributor(actualPublicationContributors);
        assertThat(actualCorrespondingContributor.getIdentity().getName(),
                startsWith(correspondingAuthorTp.getSurname()));
    }

    private Contributor getCorrespondingContributor(List<Contributor> actualPublicationContributors) {
        return actualPublicationContributors
                .stream()
                .filter(Contributor::isCorrespondingAuthor)
                .findAny().orElse(null);
    }

    @Test
    void shouldAssignCorrectLanguageForAffiliationNames() throws IOException {
        var frenchName = new String(
            "Collège de France, Lab. de Physique Corpusculaire".getBytes(),
            StandardCharsets.UTF_8);
        var italianName = "Dipartimento di Fisica, Università di Bologna";
        var norwegianName = "Institutt for fysikk, Universitetet i Bergen";
        var englishName = "Department of Physics, Iowa State University";
        var nonDeterminableName = "NTNU";
        var institutionNameWithTags = "GA2LENGlobal Allergy and Asthma European Network";
        var thaiNotSupportedByNvaName = "มหาวิทยาลัยมหิดล";
        var expectedLabels = List.of(
            Map.of(ENGLISH.getIso6391Code(), englishName),
            Map.of(FRENCH.getIso6391Code(), frenchName),
            Map.of(BOKMAAL.getIso6391Code(), norwegianName),
            Map.of(ITALIAN.getIso6391Code(), italianName),
            Map.of(ENGLISH.getIso6391Code(), nonDeterminableName),
            Map.of(ENGLISH.getIso6391Code(), thaiNotSupportedByNvaName),
            Map.of(ENGLISH.getIso6391Code(), institutionNameWithTags));
        scopusData = ScopusGenerator.createWithSpecifiedAffiliations(
            languageAffiliations(List.of(List.of("GA", generateSup("2"), "LEN",
                                                 generateInf("Global Allergy and Asthma European Network")),
                                         List.of(thaiNotSupportedByNvaName),
                                         List.of(frenchName),
                                         List.of(italianName),
                                         List.of(norwegianName),
                                         List.of(englishName),
                                         List.of(nonDeterminableName)
                                         )));
        var s3Event = createNewScopusPublicationEvent();
        var createPublicationRequest = scopusHandler.handleRequest(s3Event, CONTEXT);
        var organizations =
            createPublicationRequest.getEntityDescription()
                .getContributors()
                .stream()
                .map(Contributor::getAffiliations)
                .flatMap(Collection::stream)
                .collect(
                    Collectors.toSet());
        var actualOrganizationsLabels =
            organizations.stream().map(Organization::getLabels).collect(Collectors.toList());
        assertThat(actualOrganizationsLabels, containsInAnyOrder(expectedLabels.toArray()));
    }

    private List<AffiliationTp> languageAffiliations(List<List<Serializable>> organizationNames) {
        return organizationNames
            .stream()
            .map(this::createAffiliation)
            .collect(Collectors.toList());
    }

    private AffiliationTp createAffiliation(List<Serializable> organizationName) {
        var affiliation = new AffiliationTp();
        affiliation.setCountryAttribute(randomString());
        affiliation.setAffiliationInstanceId(randomString());
        affiliation.setAfid(randomString());
        affiliation.setCityGroup(randomString());
        affiliation.setDptid(randomString());
        affiliation.setCity(randomString());
        affiliation.setCountryAttribute(randomString());
        affiliation.getOrganization().add(createOrganization(organizationName));
        return affiliation;
    }

    private OrganizationTp createOrganization(List<Serializable> organizationName) {
        var organization = new OrganizationTp();
        organization.getContent().addAll(organizationName);
        return organization;
    }

    private void checkAuthorOrcidAndSequenceNumber(AuthorTp authorTp, List<Contributor> contributors) {
        if (nonNull(authorTp.getOrcid())) {
            var orcidAsUriString = getOrcidAsUriString(authorTp);
            var optionalContributor = findContributorByOrcid(orcidAsUriString, contributors);
            assertTrue(optionalContributor.isPresent());
            var contributor = optionalContributor.get();
            assertEquals(authorTp.getSeq(), contributor.getSequence().toString());
        }
    }

    private void checkAuthorName(AuthorTp authorTp, List<Contributor> contributors) {
        var optionalContributor = findContributorBySequence(authorTp.getSeq(), contributors);
        assertTrue(optionalContributor.isPresent());
        var contributor = optionalContributor.get();
        assertEquals(getExpectedFullAuthorName(authorTp), contributor.getIdentity().getName());
    }

    private void checkCollaborationName(CollaborationTp collaboration, List<Contributor> contributors) {
        var optionalContributor = findContributorBySequence(collaboration.getSeq(), contributors);
        assertTrue(optionalContributor.isPresent());
        var contributor = optionalContributor.get();
        assertEquals(collaboration.getIndexedName(), contributor.getIdentity().getName());
    }

    private Optional<Contributor> findContributorBySequence(String sequence, List<Contributor> contributors) {
        return contributors.stream()
            .filter(contributor -> sequence.equals(Integer.toString(contributor.getSequence())))
            .findFirst();
    }

    private List<Contributor> findContributorsBySequence(String sequence, List<Contributor> contributors) {
        return contributors.stream()
            .filter(contributor -> sequence.equals(Integer.toString(contributor.getSequence())))
            .collect(Collectors.toList());
    }

    private EventReference fetchEmittedEvent() {
        return eventBridgeClient.getRequestEntries().stream()
            .map(PutEventsRequestEntry::detail)
            .map(EventReference::fromJson)
            .collect(SingletonCollector.collect());
    }

    private String getExpectedFullAuthorName(AuthorTp authorTp) {
        return authorTp.getPreferredName().getSurname() + NAME_DELIMITER + authorTp.getPreferredName().getGivenName();
    }

    private S3Event createNewScopusPublicationEvent() throws IOException {
        var uri = s3Driver.insertFile(randomS3Path(), scopusData.toXml());
        return createS3Event(uri);
    }

    private UnixPath randomS3Path() {
        return UnixPath.of(randomString());
    }

    private TitletextTp extractTitle(ScopusGenerator scopusData) {
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

    private URI createExpectedQueryUriForJournalWithEIssn(String electronicIssn, String year) {
        return new UriWrapper(serverUriJournal)
            .addQueryParameter("query", electronicIssn)
            .addQueryParameter("year", year)
            .getUri();
    }

    private String expectedTitle(TitletextTp titleObject) {
        return ScopusConverter.extractContentAndPreserveXmlSupAndInfTags(titleObject.getContent());
    }

    private URI createExpectedQueryUriForPublisherWithName(String name) {
        return new UriWrapper(serverUriPublisher)
            .addQueryParameter("query", name)
            .getUri();
    }

    private URI mockedPublicationChannelsReturnsUri(URI queryUri) {
        var uri = randomPublicationChannelUri();
        ArrayNode publicationChannelsResponseBody = createPublicationChannelsResponseWithUri(uri);
        stubFor(get(START_OF_QUERY + queryUri
            .getQuery())
                    .willReturn(aResponse().withBody(publicationChannelsResponseBody
                                                         .toPrettyString()).withStatus(HttpURLConnection.HTTP_OK)));
        return uri;
    }

    public static URI randomPublicationChannelUri() {
        return URI.create("https://example.nva.aws.unit.no/publication-channels/" + randomString());
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

    private Optional<Contributor> findContributorByOrcid(String orcid, List<Contributor> contributors) {
        return contributors.stream()
            .filter(contributor -> orcid.equals(contributor.getIdentity().getOrcId()))
            .findFirst();
    }

    private String getOrcidAsUriString(AuthorTp authorTp) {
        return isNotBlank(authorTp.getOrcid()) ? craftOrcidUriString(authorTp.getOrcid()) : null;
    }

    private String craftOrcidUriString(String potentiallyMalformedOrcidString) {
        return potentiallyMalformedOrcidString.contains(ORCID_DOMAIN_URL)
                   ? potentiallyMalformedOrcidString
                   : ORCID_DOMAIN_URL + potentiallyMalformedOrcidString;
    }

    private List<AuthorTp> keepOnlyTheAuthors() {
        return keepOnlyTheCollaborationsAndAuthors().stream().filter(
            this::isAuthorTp).map(author -> (AuthorTp) author).collect(Collectors.toList());
    }

    private List<AuthorTp> keepOnlyTheAuthors(AuthorGroupTp authorGroupTp) {
        return authorGroupTp
            .getAuthorOrCollaboration()
            .stream()
            .filter(this::isAuthorTp)
            .map(author -> (AuthorTp) author)
            .collect(Collectors.toList());
    }

    private List<CollaborationTp> keepOnlyTheCollaborations() {
        return keepOnlyTheCollaborationsAndAuthors().stream().filter(
            this::isCollaborationTp).map(collaboration -> (CollaborationTp) collaboration).collect(Collectors.toList());
    }

    private boolean isAuthorTp(Object object) {
        return object instanceof AuthorTp;
    }

    private boolean isCollaborationTp(Object object) {
        return object instanceof CollaborationTp;
    }

    private List<Object> keepOnlyTheCollaborationsAndAuthors() {
        return scopusData
            .getDocument()
            .getItem()
            .getItem()
            .getBibrecord()
            .getHead()
            .getAuthorGroup()
            .stream()
            .map(AuthorGroupTp::getAuthorOrCollaboration)
            .flatMap(Collection::stream)
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