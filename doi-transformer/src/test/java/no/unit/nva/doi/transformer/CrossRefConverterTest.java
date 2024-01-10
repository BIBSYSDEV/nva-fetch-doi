package no.unit.nva.doi.transformer;

import static java.util.Objects.nonNull;
import static no.unit.nva.doi.transformer.CrossRefConverter.UNRECOGNIZED_TYPE_MESSAGE;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.collection.IsEmptyCollection.empty;
import static org.hamcrest.collection.IsIterableContainingInOrder.contains;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.hamcrest.core.IsNot.not;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.hamcrest.core.IsNull.nullValue;
import static org.hamcrest.core.StringContains.containsString;
import static org.hamcrest.text.IsEmptyString.emptyString;
import static org.hamcrest.text.MatchesPattern.matchesPattern;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Path;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import no.sikt.nva.doi.fetch.jsonconfig.Json;
import no.unit.nva.doi.transformer.language.LanguageMapper;
import no.unit.nva.doi.transformer.language.exceptions.LanguageUriNotFoundException;
import no.unit.nva.doi.transformer.model.crossrefmodel.CrossRefDocument;
import no.unit.nva.doi.transformer.model.crossrefmodel.CrossrefAffiliation;
import no.unit.nva.doi.transformer.model.crossrefmodel.CrossrefApiResponse;
import no.unit.nva.doi.transformer.model.crossrefmodel.CrossrefContributor;
import no.unit.nva.doi.transformer.model.crossrefmodel.CrossrefDate;
import no.unit.nva.doi.transformer.model.crossrefmodel.Isxn;
import no.unit.nva.doi.transformer.model.crossrefmodel.Isxn.IsxnType;
import no.unit.nva.doi.transformer.model.crossrefmodel.Link;
import no.unit.nva.doi.transformer.utils.CrossrefType;
import no.unit.nva.doi.transformer.utils.IssnCleaner;
import no.unit.nva.model.Contributor;
import no.unit.nva.model.Corporation;
import no.unit.nva.model.Identity;
import no.unit.nva.model.Publication;
import no.unit.nva.model.PublicationDate;
import no.unit.nva.model.contexttypes.Anthology;
import no.unit.nva.model.contexttypes.Book;
import no.unit.nva.model.contexttypes.UnconfirmedJournal;
import no.unit.nva.model.contexttypes.UnconfirmedPublisher;
import no.unit.nva.model.contexttypes.UnconfirmedSeries;
import no.unit.nva.model.instancetypes.book.AcademicMonograph;
import no.unit.nva.model.instancetypes.book.BookAnthology;
import no.unit.nva.model.instancetypes.chapter.AcademicChapter;
import no.unit.nva.model.instancetypes.journal.JournalArticle;
import no.unit.nva.model.pages.MonographPages;
import no.unit.nva.model.pages.Pages;
import no.unit.nva.model.pages.Range;
import no.unit.nva.model.role.Role;
import nva.commons.core.ioutils.IoUtils;
import nva.commons.doi.DoiConverter;
import nva.commons.logutils.LogUtils;
import nva.commons.logutils.TestAppender;
import org.apache.commons.validator.routines.ISBNValidator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;

public class CrossRefConverterTest extends ConversionTest {

    public static final String AUTHOR_GIVEN_NAME = "givenName";
    public static final String AUTHOR_FAMILY_NAME = "familyName";
    public static final String FIRST_AUTHOR = "first";
    public static final Integer EXPECTED_YEAR = 2019;
    //    public static final String SURNAME_COMMA_FIRSTNAME = "%s,.*%s";
    public static final String FIRSTNAME_SURNAME = "%s %s";
    public static final String SECOND_AUTHOR = "second";
    public static final String CROSSREF_WITH_ABSTRACT_JSON = "crossrefWithAbstract.json";
    public static final String ENG_ISO_639_3 = "eng";
    public static final String SOME_DOI = "10.1000/182";
    public static final URI SOME_DOI_AS_URL = URI.create("http://dx.doi.org/10.1007/978-3-030-22507-0_9");
    public static final String VALID_ISSN_A = "0306-4379";
    public static final String VALID_ISSN_B = "1066-8888";
    public static final String INVALID_ISSN = "abcd--8888";
    public static final String VALID_ISBN_A = "9788202529819";
    public static final String VALID_ISBN_B = "978-82-450-0364-2";
    public static final String INVALID_ISBN = "9xz-8b-4asdas50-0364-2";
    public static final int EXPECTED_MONTH = 2;
    public static final int EXPECTED_DAY = 20;
    public static final ISBNValidator ISBN_VALIDATOR = new ISBNValidator();
    public static final String SAMPLE_CONTAINER_TITLE = "Container Title";
    public static final String SAMPLE_PUBLISHER = "Sample Publisher Inc";
    public static final String SAMPLE_LINK = "https://localhost/some.link";
    public static final String CONNECTING_MINUS = "-";
    public static final String SAMPLE_ISSUE = "SomeIssue";
    public static final String SOME_STRANGE_CROSSREF_TYPE = "SomeStrangeCrossrefType";
    public static final String FIRST_NAME_OF_JOURNAL = "Journal 1st Name";
    public static final String FIRST_PAGE_IN_RANGE = "45";
    public static final String LAST_PAGE_IN_RANGE = "89";
    public static final String EXPECTED_TAG = "subject1";
    public static final String SECOND_NAME_OF_JOURNAL = "Journal 2nd Name";
    public static final String EXPECTED_VOLUME = "Vol. 1";
    public static final String CROSSREF = "Crossref";
    public static final String SOURCE_URI = "http://www.something.com";
    public static final int EXPECTED_NUMBER_OF_CONTRIBUTORS = 4;
    public static final int FIRST_LIST_ELEMENT = 0;
    private static final int DATE_SIZE = 3;
    private static final String SAMPLE_DOCUMENT_TITLE = "Sample document title";
    private static final Integer NUMBER_OF_SAMPLE_AUTHORS = 2;
    private static final UUID DOC_ID = UUID.randomUUID();
    private static final String OWNER = "TheOwner";
    private static final String INVALID_ORDINAL = "invalid ordinal";
    private static final String PROCESSED_ABSTRACT = "processedAbstract.txt";
    private static final String SAMPLE_ORCID = "http://orcid.org/0000-1111-2222-3333";
    private static final Instant SAMPLE_CROSSREF_DATE_AS_INSTANT = sampleCrossrefDateAsInstant();
    private static final String ALTERNATIVE_TITLE = "Some alternative title";
    private static final int SECOND_LIST_ELEMENT = 1;
    private final CrossRefConverter converter = setUpConverter();

    private CrossRefConverter setUpConverter() {
        DoiConverter doiConverter = new DoiConverter(uri -> true);
        return new CrossRefConverter(doiConverter);
    }

    @Test
    void defaultConstructorExists() {
        new CrossRefConverter();
    }

    @Test
    public void toPublicationReturnsObjectWithSourceLinkBeingTheFirstAvailableLinkToTheActualPublication() {
        CrossRefDocument crossRefDocument = sampleBook();
        assertThat(crossRefDocument.getLink(), is(not(empty())));
        Publication publication = toPublication(crossRefDocument);
        String actualSourceUriString = publication.getLink().toString();
        String expectedSourceUriString = crossRefDocument.getLink().get(FIRST_LIST_ELEMENT).getUrl();
        assertThat(actualSourceUriString, is(equalTo(expectedSourceUriString)));
    }

    @Test
    public void toPublicationLogsWarningWhenCrossrefDocumentLinkContainsInvalidUri() {
        TestAppender logAppender = LogUtils.getTestingAppenderForRootLogger();
        String invalidUri = "not a uri";
        CrossRefDocument crossRefDocument = crossRefDocumentWithInvalidUrl(invalidUri);
        toPublication(crossRefDocument);
        assertThat(logAppender.getMessages(), containsString(invalidUri));
    }

    @ParameterizedTest(name = "toPublicationDoesNotThrowExceptionWhenPublicationTypeIsMissingButLogsWarning")
    @NullAndEmptySource
    public void toPublicationDoesNotThrowExceptionWhenPublicationTypeIsMissingButLogsWarning(String crossRefType) {
        TestAppender logAppender = LogUtils.getTestingAppenderForRootLogger();
        CrossRefDocument crossrefDoc = sampleBook();
        crossrefDoc.setType(crossRefType);
        assertDoesNotThrow(() -> toPublication(crossrefDoc));
        String expectedMessage = String.format(UNRECOGNIZED_TYPE_MESSAGE, crossRefType);
        assertThat(logAppender.getMessages(), containsString(expectedMessage));
    }

    @Test
    public void toPublicationReturnsObjectWithAlternativeTitlesWhenMoreThanOneTitlesExist() {
        CrossRefDocument crossrefDoc = sampleBook();
        assertThat(crossrefDoc.getTitle(), hasSize(2));
        String expectedAlternativeTitle = crossrefDoc.getTitle().get(SECOND_LIST_ELEMENT);
        Publication publication = toPublication(crossrefDoc);
        List<String> actualAlternativeTitles =
            new ArrayList<>(publication.getEntityDescription().getAlternativeTitles().keySet());
        assertThat(actualAlternativeTitles, hasSize(1));
        assertThat(actualAlternativeTitles.get(FIRST_LIST_ELEMENT), is(equalTo(expectedAlternativeTitle)));
    }

    @Test
    public void toPublicationReturnsPublicationWithFilteredOutContributorsLackingBothNameAndSurname() {
        CrossRefDocument crossRefDocument = sampleJournalArticle();

        int initialContributorsNumber = crossRefDocument.getAuthor().size();
        List<CrossrefContributor> newContributors = createAuthorListWithAnonymousAuthor(crossRefDocument);
        crossRefDocument.setAuthor(newContributors);
        assertThat(crossRefDocument.getAuthor().size(), is(equalTo(initialContributorsNumber + 1)));

        Publication publication = toPublication(crossRefDocument);
        List<Contributor> actualContributors = publication.getEntityDescription().getContributors();
        assertThat(actualContributors.size(), is(equalTo(initialContributorsNumber)));
    }

    @Test
    @DisplayName("An empty CrossRef document throws IllegalArgument exception")
    void anEmptyCrossRefDocumentThrowsIllegalArgumentException() throws IllegalArgumentException {
        CrossRefDocument crossRefDocument = new CrossRefDocument();
        IllegalArgumentException exception =
            assertThrows(IllegalArgumentException.class, () -> toPublication(crossRefDocument));
        assertThat(exception.getMessage(), is(CrossRefConverter.INVALID_ENTRY_ERROR));
    }

    @Test
    @DisplayName("An CrossRef document with unknown type logs error and continues")
    void anCrossRefDocumentWithUnknownTypeLogsErrorAndContinues()
        throws IllegalArgumentException {
        CrossRefDocument crossRefDocument = sampleCrossRefDocumentWithBasicMetadata();
        crossRefDocument.setType(SOME_STRANGE_CROSSREF_TYPE);
        TestAppender testAppender = LogUtils.getTestingAppender(CrossRefConverter.class);
        toPublication(crossRefDocument);
        String expectedError = String.format(UNRECOGNIZED_TYPE_MESSAGE, SOME_STRANGE_CROSSREF_TYPE);
        assertThat(testAppender.getMessages(), containsString(expectedError));
    }

    @Test
    @DisplayName("An CrossRef document without type throws IllegalArgument exception")
    void anCrossRefDocumentWithoutTypeThrowsIllegalArgumentException()
        throws IllegalArgumentException {
        CrossRefDocument crossRefDocument = sampleCrossRefDocumentWithBasicMetadata();
        crossRefDocument.setType(null);
        TestAppender testAppender = LogUtils.getTestingAppender(CrossRefConverter.class);
        toPublication(crossRefDocument);
        String expectedError = String.format(UNRECOGNIZED_TYPE_MESSAGE, "null");
        assertThat(testAppender.getMessages(), containsString(expectedError));
    }

    @Test
    @DisplayName("The creator's name in the publication contains first given name and then family name name")
    void creatorsNameContainsFirstGivenAndThenFamilyName()
        throws IllegalArgumentException {

        CrossRefDocument sampleJournalArticle = sampleJournalArticle();
        Publication samplePublication = toPublication(sampleJournalArticle);

        List<Contributor> contributors = samplePublication.getEntityDescription().getContributors();

        assertThat(contributors.size(), is(equalTo(sampleJournalArticle.getAuthor().size())));
        assertThat(contributors.size(), is(equalTo(NUMBER_OF_SAMPLE_AUTHORS)));

        String actualName = contributors.get(NUMBER_OF_SAMPLE_AUTHORS - 1).getIdentity().getName();
        String givenName = sampleJournalArticle.getAuthor().get(NUMBER_OF_SAMPLE_AUTHORS - 1).getGivenName();
        assertThat(actualName, containsString(givenName));

        String familyName = sampleJournalArticle.getAuthor().get(NUMBER_OF_SAMPLE_AUTHORS - 1).getFamilyName();
        assertThat(actualName, containsString(familyName));

        String expectedNameRegEx = String.format(FIRSTNAME_SURNAME, givenName, familyName);
        assertThat(actualName, matchesPattern(expectedNameRegEx));
    }

    @Test
    @DisplayName("The earliest year found in the \"published-print\" field is stored in the entity description.")
    void entityDescriptionContainsTheEarliestYearFoundInPublishedPrintField() {
        CrossRefDocument sampleJournalArticle = sampleJournalArticle();
        Publication samplePublication = toPublication(sampleJournalArticle);
        String actualYear = samplePublication.getEntityDescription().getPublicationDate().getYear();
        assertThat(actualYear, is(equalTo(EXPECTED_YEAR.toString())));
    }

    @Test
    @DisplayName("toPublication sets entityDescription.date to null when inputdata has no PublicationDate")
    void toPublicationSetsEntityDescriptionDateToNullWhenInputDataHasNoPublicationDate() {

        CrossRefDocument sampleJournalArticle = sampleJournalArticle();
        sampleJournalArticle.setIssued(null);
        Publication publicationWithoutDate = toPublication(sampleJournalArticle);
        PublicationDate actualDate = publicationWithoutDate.getEntityDescription().getPublicationDate();
        assertThat(actualDate, is(nullValue()));
    }

    @Test
    @DisplayName("toPublication sets PublicationContext to Journal when the input has the tag \"journal-article\"")
    void toPublicationSetsPublicationContextToJournalWhenTheInputHasTheTagJournalArticle() {
        Publication samplePublication = toPublication(sampleJournalArticle());
        assertThat(samplePublication.getEntityDescription().getReference().getPublicationContext().getClass(),
                   is(equalTo(UnconfirmedJournal.class)));
    }

    @Test
    @DisplayName("toPublication sets as sequence the position of the author in the list when ordinal is not numerical")
    void toPublicationSetsOrdinalAsSecondAuthorIfInputOrdinalIsNotAValidOrdinal() {
        CrossRefDocument sampleJournalArticle = sampleJournalArticle();
        int numberOfAuthors = sampleJournalArticle.getAuthor().size();
        sampleJournalArticle.getAuthor().forEach(a -> a.setSequence(INVALID_ORDINAL));
        List<Integer> ordinals = toPublication(sampleJournalArticle).getEntityDescription().getContributors().stream()
                                     .map(Contributor::getSequence).collect(Collectors.toList());
        assertThat(ordinals.size(), is(numberOfAuthors));
        assertThat(ordinals, contains(IntStream.range(0, numberOfAuthors)
                                          .map(this::startCountingFromOne)
                                          .boxed().toArray()));
    }

    @Test
    @DisplayName("toPublication sets abstract when input has non empty abstract")
    void toPublicationSetsAbstractWhenInputHasNonEmptyAbstract()
        throws IOException {
        String json = IoUtils.stringFromResources(Path.of(CROSSREF_WITH_ABSTRACT_JSON));
        CrossRefDocument crossRefDocument = Json.readValue(json, CrossrefApiResponse.class).getMessage();
        String abstractText = toPublication(crossRefDocument).getEntityDescription().getAbstract();
        assertThat(abstractText, is(not(emptyString())));
        String expectedAbstract = IoUtils.stringFromResources(Path.of(PROCESSED_ABSTRACT));
        assertThat(abstractText, is(equalTo(expectedAbstract)));
    }

    @Test
    @DisplayName("toPublication sets the language to a URI when the input is an ISO639-3 entry")
    void toPublicationSetsTheLanguageToAUriWhenTheInputFollowsTheIso3Standard()
        throws LanguageUriNotFoundException {
        Locale sampleLanguage = Locale.ENGLISH;
        CrossRefDocument sampleJournalArticle = sampleJournalArticle();
        sampleJournalArticle.setLanguage(sampleLanguage.getISO3Language());
        URI actualLanguage = toPublication(sampleJournalArticle).getEntityDescription().getLanguage();
        URI expectedLanguage = LanguageMapper.getUriFromIso(ENG_ISO_639_3);
        assertThat(actualLanguage, is(equalTo(expectedLanguage)));
        assertThat(actualLanguage, is(notNullValue()));
    }

    @Test
    @DisplayName("toPublication sets the doi of the Reference when the Crossref document has a \"DOI\" value ")
    void toPublicationSetsTheDoiOfTheReferenceWhenTheCrossrefDocHasADoiValue() {
        DoiConverter doiConverter = new DoiConverter(uri -> true);
        CrossRefDocument sampleJournalArticle = sampleJournalArticle();
        sampleJournalArticle.setDoi(SOME_DOI);
        URI actualDoi = toPublication(sampleJournalArticle).getEntityDescription().getReference().getDoi();
        assertThat(actualDoi, is(equalTo(doiConverter.toUri(SOME_DOI))));
    }

    @Test
    @DisplayName("toPublication sets the doi of the Reference when the Crossref document has at least one"
                 + " \"Container\" value ")
    void toPublicationSetsTheNameOfTheReferenceWhenTheCrossrefDocHasAtLeatOneContainterTitle() {
        CrossRefDocument sampleJournalArticle = sampleJournalArticle();
        sampleJournalArticle.setContainerTitle(Arrays.asList(FIRST_NAME_OF_JOURNAL, SECOND_NAME_OF_JOURNAL));
        String actualJournalName = ((UnconfirmedJournal) toPublication(sampleJournalArticle)
                                                             .getEntityDescription()
                                                             .getReference()
                                                             .getPublicationContext()).getTitle();
        assertThat(actualJournalName, is(equalTo(FIRST_NAME_OF_JOURNAL)));
    }

    @Test
    @DisplayName("toPublication sets the volume of the Reference when the Crosref document has a \"Volume\" value")
    void toPublicationSetsTheVolumeOfTheReferenceWhentheCrossrefDocHasAVolume() {
        CrossRefDocument sampleJournalArticle = sampleJournalArticle();
        sampleJournalArticle.setVolume(EXPECTED_VOLUME);
        String actualVolume = ((JournalArticle) (toPublication(sampleJournalArticle)
                                                     .getEntityDescription().getReference()
                                                     .getPublicationInstance()))
                                  .getVolume();
        assertThat(actualVolume, is(equalTo(EXPECTED_VOLUME)));
    }

    @Test
    @DisplayName("toPublication sets the pages of the Reference when the Crossref document has a \"Pages\" value")
    void toPublicationSetsThePagesOfTheReferenceWhentheCrossrefDocHasPages() {
        String pages = FIRST_PAGE_IN_RANGE + CONNECTING_MINUS + LAST_PAGE_IN_RANGE;
        CrossRefDocument sampleJournalArticle = sampleJournalArticle();
        sampleJournalArticle.setPage(pages);
        Pages actualPages = toPublication(sampleJournalArticle).getEntityDescription().getReference()
                                .getPublicationInstance()
                                .getPages();
        Pages expectedPages = new Range.Builder().withBegin(FIRST_PAGE_IN_RANGE).withEnd(LAST_PAGE_IN_RANGE).build();
        assertThat(actualPages, is(equalTo(expectedPages)));
    }

    @Test
    @DisplayName("toPublication sets the pages of the Reference when the Crossref document has a \"Pages\" value")
    void toPublicationSetsTheMonographPagesOfTheReferenceWhentheCrossrefDocHasPages() {
        String pages = "222";
        CrossRefDocument sampleBook = sampleBook();
        sampleBook.setPage(pages);
        Pages actualPages = toPublication(sampleBook).getEntityDescription().getReference()
                                .getPublicationInstance()
                                .getPages();
        Pages expectedPages = new MonographPages.Builder().withPages(pages).build();
        assertThat(actualPages, is(equalTo(expectedPages)));
    }

    @Test
    @DisplayName("toPublication sets the issue of the Reference when the Crossref document has a \"Issue\" value")
    void toPublicationSetsTheIssueOfTheReferenceWhentheCrossrefDocHasAnIssueValue() {
        String expectedIssue = SAMPLE_ISSUE;
        CrossRefDocument sampleJournalArticle = sampleJournalArticle();
        sampleJournalArticle.setIssue(expectedIssue);
        String actualIssue = ((JournalArticle) (toPublication(sampleJournalArticle)
                                                    .getEntityDescription()
                                                    .getReference()
                                                    .getPublicationInstance()))
                                 .getIssue();
        assertThat(actualIssue, is(equalTo(expectedIssue)));
    }

    @Test
    @DisplayName("toPublication sets the MetadataSource to the CrossRef URL when the Crossref "
                 + "document has a \"source\" containing the word crossref")
    void toPublicationSetsTheMetadataSourceToTheCrossRefUrlWhenTheCrossrefDocHasCrossrefAsSource() {
        URI expectedURI = CrossRefConverter.CROSSEF_URI;
        CrossRefDocument sampleDocumentJournalArticle = sampleJournalArticle();
        sampleDocumentJournalArticle.setSource(CROSSREF);
        URI actualSource = toPublication(sampleDocumentJournalArticle).getEntityDescription().getMetadataSource();
        assertThat(actualSource, is(equalTo(expectedURI)));
    }

    @Test
    @DisplayName("toPublication sets the MetadataSource to the specfied URL when the Crossref "
                 + "document has as \"source\" a valid URL")
    void toPublicationSetsTheMetadataSourceToTheSourceUrlIfTheDocHasAsSourceAValidUrl() {
        URI expectedURI = URI.create(SOURCE_URI);
        CrossRefDocument sampleJournalArticle = sampleJournalArticle();
        sampleJournalArticle.setSource(SOURCE_URI);
        URI actualSource = toPublication(sampleJournalArticle).getEntityDescription().getMetadataSource();
        assertThat(actualSource, is(equalTo(expectedURI)));
    }

    @Test
    @DisplayName("toPublication sets null for online ISSN when only print ISSN is available")
    void toPublicationSetsNullOnlineIssnWhenOnlyPrintISSnIsAvailable() {
        IsxnType type = IsxnType.PRINT;
        Isxn printIssn = sampleIsxn(type, VALID_ISSN_A);
        CrossRefDocument sampleJournalArticle = sampleJournalArticle();
        sampleJournalArticle.setIssnType(Collections.singletonList(printIssn));
        Publication actualPublication = toPublication(sampleJournalArticle);
        UnconfirmedJournal actualPublicationContext = getJournalContext(actualPublication);
        String onlineIssn = actualPublicationContext.getOnlineIssn();
        assertThat(onlineIssn, is(equalTo(null)));
    }

    @Test
    @DisplayName("toPublication sets null for print ISSN when only online ISSN is available")
    void toPublicationSetsNullPrintIssnWhenOnlyOnlineISSnIsAvailable() {
        IsxnType type = IsxnType.ELECTRONIC;
        Isxn onlineIssn = sampleIsxn(type, VALID_ISSN_A);
        CrossRefDocument sampleJournalArticle = sampleJournalArticle();
        sampleJournalArticle.setIssnType(Collections.singletonList(onlineIssn));
        Publication actualPublication = toPublication(sampleJournalArticle);
        UnconfirmedJournal actualPublicationContext = getJournalContext(actualPublication);
        String actualPrintIssn = actualPublicationContext.getPrintIssn();
        assertThat(actualPrintIssn, is(equalTo(null)));
    }

    @Test
    @DisplayName("toPublication sets one of many online ISSNs when multiple online ISSNs are  available")
    void toPublicationSetsOneOfManyOnlineIssnsWhenMultipleOfTheSameTypeAreAvailable() {
        CrossRefDocument sampleJournalArticle = sampleJournalArticle();
        List<Isxn> issns = addIssnsToJournalArticle(sampleJournalArticle, IsxnType.ELECTRONIC,
                                                    VALID_ISSN_A, VALID_ISSN_B).getIssnType();
        Publication actualPublication = toPublication(sampleJournalArticle);
        UnconfirmedJournal actualPublicationContext = getJournalContext(actualPublication);
        String actualOnlineIssn = actualPublicationContext.getOnlineIssn();
        List<String> poolOfExpectedValues = getPoolOfExpectedValues(issns);
        assertThat(poolOfExpectedValues, hasItem(actualOnlineIssn));
    }

    @Test
    @DisplayName("toPublication sets one of many print ISSNs when multiple print ISSNs are available")
    void toPublicationSetsOneOfManyPrintIssnsWhenMultipleOfTheSameTypeAreAvailable() {
        CrossRefDocument sampleJournalArticle = sampleJournalArticle();
        List<Isxn> issns = addIssnsToJournalArticle(sampleJournalArticle, IsxnType.PRINT,
                                                    VALID_ISSN_A, VALID_ISSN_B).getIssnType();
        Publication actualPublication = toPublication(sampleJournalArticle);
        UnconfirmedJournal actualPublicationContext = getJournalContext(actualPublication);
        String actualPrintIssn = actualPublicationContext.getPrintIssn();
        List<String> poolOfExpectedValues = getPoolOfExpectedValues(issns);
        assertThat(poolOfExpectedValues, hasItem(actualPrintIssn));
    }

    @Test
    @DisplayName("toPublication sets tags when subject are available")
    void toPublicationSetsTagsWhenSubjectAreAvailable() {
        List<String> subject = List.of(EXPECTED_TAG);
        CrossRefDocument sampleJournalArticle = sampleJournalArticle();
        sampleJournalArticle.setSubject(subject);
        List<String> actualTags = toPublication(sampleJournalArticle).getEntityDescription().getTags();
        assertThat(actualTags, hasItem(EXPECTED_TAG));
    }

    @Test
    @DisplayName("toPublication sets tags to empty list when subject are not available")
    void toPublicationSetsTagsToEmptyListWhenSubjectAreNotAvailable() {
        CrossRefDocument sampleJournalArticle = sampleJournalArticle();
        sampleJournalArticle.setSubject(null);
        List<String> actualTags = toPublication(sampleJournalArticle).getEntityDescription().getTags();
        assertTrue(actualTags.isEmpty());
    }

    @Test
    @DisplayName("toPublication preserves all date parts when they are available")
    void toPublicationPreservesAllDatepartsWhenTheyAreAvailable() {
        CrossrefDate crossrefDate = sampleCrossrefDate();
        CrossRefDocument sampleJournalArticle = sampleJournalArticle();
        sampleJournalArticle.setIssued(crossrefDate);
        PublicationDate publicationDate = toPublication(sampleJournalArticle).getEntityDescription()
                                              .getPublicationDate();
        assertEquals("" + EXPECTED_YEAR, publicationDate.getYear());
        assertEquals("" + EXPECTED_MONTH, publicationDate.getMonth());
        assertEquals("" + EXPECTED_DAY, publicationDate.getDay());
    }

    @Test
    @DisplayName("toPublication sets affiliation labels when author.affiliation.name is available")
    void toPublicationSetsAffiliationLabelWhenAuthorHasAffiliationName() {
        CrossRefDocument sampleJournalArticle = sampleJournalArticle();
        setAuthorWithAffiliation(sampleJournalArticle);
        List<Contributor> contributors = toPublication(sampleJournalArticle).getEntityDescription().getContributors();
        List<Corporation> organisations = getOrganisations(contributors);
        assertFalse(organisations.isEmpty());
    }

    @Test
    @DisplayName("toPublication sets multiple affiliation labels when author has more affiliations name")
    void toPublicationSetsMultipleAffiliationLabelWhenAuthorHasMultipleAffiliationName() {
        CrossRefDocument sampleJournalArticle = sampleJournalArticle();
        setAuthorWithMultipleAffiliations(sampleJournalArticle);
        List<Contributor> contributors = toPublication(sampleJournalArticle).getEntityDescription().getContributors();
        List<Corporation> organisations = getOrganisations(contributors);
        assertTrue(organisations.size() > 1);
    }

    @Test
    @DisplayName("toPublication sets affiliation to empty list when author has no affiliation")
    void toPublicationSetsAffiliationToEmptyListWhenAuthorHasNoAffiliatio() {
        List<Contributor> contributors = toPublication(sampleJournalArticle()).getEntityDescription().getContributors();
        List<Corporation> organisations = getOrganisations(contributors);
        assertTrue(organisations.isEmpty());
    }

    @Test
    @DisplayName("toPublication preserves Orcid when Author has orcid")
    void toPublicationPreservesOrcidWhenAuthorHasOrcid() {
        CrossRefDocument sampleJournalArticle = sampleJournalArticle();
        setAuthorWithUnauthenticatedOrcid(sampleJournalArticle);
        var orcids = toPublication(sampleJournalArticle)
                         .getEntityDescription().getContributors().stream()
                         .map(Contributor::getIdentity)
                         .map(Identity::getOrcId)
                         .collect(Collectors.toList());
        assertThat(orcids, hasItem(SAMPLE_ORCID));
    }

    @Test
    @DisplayName("toPublication sets PublicationContext to Book when crossref-type is book")
    void toPublicationSetsPublicationContextToBookWhjenCrossrefDocumentHasTypeBook() {
        var publicationContext = toPublication(sampleBook())
                                     .getEntityDescription().getReference().getPublicationContext();
        assertTrue(publicationContext instanceof Book);
    }

    @Test
    @DisplayName("toPublication sets ISBN when crossref-type is book")
    void toPublicationSetsIsbnWhenCrossrefDocumentIsTypeBook() {
        CrossRefDocument sampleBook = sampleBook();
        List<Isxn> isbns = addIsbnsToBook(sampleBook, VALID_ISBN_A, VALID_ISBN_B).getIsbnType();
        Book actualPublicationContext = convertAndGetBookContext(sampleBook);
        Set<String> actualValues = new HashSet<>(actualPublicationContext.getIsbnList());
        Set<String> expectedValues = constructExpectedIsbnValues(isbns);
        assertEquals(expectedValues, actualValues);
    }

    @Test
    @DisplayName("toPublication filters ISBN error and continues")
    void toPublicationFiltersIsbnErrorAndContinues() {
        CrossRefDocument sampleBook = sampleBook();
        List<Isxn> isbns = addIsbnsToBook(sampleBook, VALID_ISBN_A, INVALID_ISBN).getIsbnType();
        Book actualPublicationContext = convertAndGetBookContext(sampleBook);
        Set<String> actualValues = new HashSet<>(actualPublicationContext.getIsbnList());
        Set<String> expectedValues = constructExpectedIsbnValues(isbns);
        assertEquals(expectedValues, actualValues);
    }

    @Test
    @DisplayName("toPublication filters ISSN error and continues")
    void toPublicationFiltersIssnErrorAndContinues() {
        CrossRefDocument sampleJournalArticle = sampleJournalArticle();
        List<Isxn> issns = addIssnsToJournalArticle(sampleJournalArticle, IsxnType.ELECTRONIC,
                                                    VALID_ISSN_A, INVALID_ISSN).getIssnType();
        Publication actualPublication = toPublication(sampleJournalArticle);
        String actualOnlineIssn = getJournalContext(actualPublication).getOnlineIssn();
        List<String> poolOfExpectedValues = getPoolOfExpectedValues(issns);
        assertThat(poolOfExpectedValues, hasItem(actualOnlineIssn));
        assertThat(poolOfExpectedValues, not(hasItem(INVALID_ISSN)));
    }

    @Test
    @DisplayName("toPublication sets seriesTitle in PublicationContext when CrossrefDocument has ContainerTitle")
    void toPublicationSetsSeriesTitleInPublicationContextWhenCrossrefDocumentHasContainerTitle() {
        CrossRefDocument sampleBook = sampleBook();
        sampleBook.setContainerTitle(List.of(SAMPLE_CONTAINER_TITLE));
        String actualSeriesTitle = ((UnconfirmedSeries) convertAndGetBookContext(sampleBook).getSeries()).getTitle();
        assertThat(actualSeriesTitle, is(equalTo(SAMPLE_CONTAINER_TITLE)));
    }

    @Test
    @DisplayName("toPublication sets Publisher in PublicationContext when CrossrefDocument has Publisher")
    void toPublicationSetsPublisherInPublicationContextWhenCrossrefDocumentHasPublisher() {
        CrossRefDocument sampleBook = sampleBook();
        sampleBook.setPublisher(SAMPLE_PUBLISHER);
        String actualPublisher = ((UnconfirmedPublisher) convertAndGetBookContext(sampleBook).getPublisher()).getName();
        assertThat(actualPublisher, is(equalTo(SAMPLE_PUBLISHER)));
    }

    @Test
    @DisplayName("toPublication handles all interesting and required fields in CrossrefDocument for journal-article")
    void toPublicationHandlesAllInterestingAndRequiredFieldsInCrossrefDocumentForJournalArticle() {
        assertRequiredValuesAreConverted(toPublication(sampleJournalArticle()));
    }

    @Test
    @DisplayName("toPublication handles all interesting and required fields in CrossrefDocument for book")
    void toPublicationHandlesAllInterestingAndRequiredFieldsInCrossrefDocumentForBook() {
        assertRequiredValuesAreConverted(toPublication(sampleBook()));
    }

    @Test
    @DisplayName("toPublication handles all interesting and required fields in CrossrefDocument for edited-book")
    void toPublicationHandlesAllInterestingAndRequiredFieldsInCrossrefDocumentForEditedBook() {
        assertRequiredValuesAreConverted(toPublication(sampleEditedBook()));
    }

    @Test
    @DisplayName("toPublication handles all interesting and required fields in CrossrefDocument for book-chapter")
    void toPublicationHandlesAllInterestingAndRequiredFieldsInCrossrefDocumentForBookChapter() {
        assertRequiredValuesAreConverted(toPublication(sampleBookChapter()));
    }

    @Test
    @DisplayName("toPublication set publicationContext when input Crossref document is BookChapter")
    void toPublicationSetsPublicationContextWhenInputCrossrefDocumentIsBookChapter() {
        assertThat(toPublication(sampleBookChapter())
                       .getEntityDescription().getReference().getPublicationContext().getClass(),
                   is(equalTo(Anthology.class)));
    }

    @Test
    @DisplayName("toPublication set publicationInstance when input Crossref document is BookChapter")
    void toPublicationSetsPublicationInstanceWhenInputCrossrefDocumentIsBookChapter() {
        assertThat(toPublication(sampleBookChapter())
                       .getEntityDescription().getReference().getPublicationInstance().getClass(),
                   is(equalTo(AcademicChapter.class)));
    }

    @Test
    @DisplayName("toPublication set pages when input Crossref document is BookChapter")
    void toPublicationSetsPagesWhenInputCrossrefDocumentIsBookChapter() {
        Pages actualPages = toPublication(sampleBookChapter())
                                .getEntityDescription()
                                .getReference()
                                .getPublicationInstance()
                                .getPages();
        Pages expectedPages = new Range.Builder().withBegin(FIRST_PAGE_IN_RANGE).withEnd(LAST_PAGE_IN_RANGE).build();
        assertThat(actualPages, is(equalTo(expectedPages)));
    }

    @Test
    @DisplayName("toPublication Assigns Roles to Editors")
    void toPublicationAssignsRolesToEditors() {

        CrossRefDocument sampleBook = sampleBook();
        setEditors(sampleBook);
        Publication samplePublication = toPublication(sampleBook);
        List<Contributor> contributors = samplePublication.getEntityDescription().getContributors();
        final int size = sampleBook.getAuthor().size() + sampleBook.getEditor().size();
        assertThat(contributors.size(), is(equalTo(size)));
        List<Contributor> editors = contributors.stream().filter(this::isEditor).collect(Collectors.toList());
        assertThat(editors.size(), is(equalTo(sampleBook.getEditor().size())));
    }

    @Test
    @DisplayName("toPublication sets publicationInstance to BookAnthology for edited book")
    void toPublicationSetsPublicationInstanceToBookAnthologyWhenEditedBook() {

        CrossRefDocument sampleEditedBook = sampleEditedBook();
        var publicationInstanceClass = toPublication(sampleEditedBook)
            .getEntityDescription()
            .getReference()
            .getPublicationInstance()
            .getClass();

        assertThat(publicationInstanceClass, is(equalTo(BookAnthology.class)));
    }

    @Test
    @DisplayName("toPublication sets publicationInstance to BookAnthology when book has editors")
    void toPublicationSetsPublicationInstanceToBookAnthologyWhenBookHasEditors() {

        CrossRefDocument sampleBook = sampleBook();
        setEditors(sampleBook);
        assertThat(toPublication(sampleBook).getEntityDescription().getReference().getPublicationInstance().getClass(),
                   is(equalTo(BookAnthology.class)));
    }

    @Test
    @DisplayName("toPublication sets publicationInstance to BookMonograph when book has no editors")
    void toPublicationSetsPublicationInstanceToBookMonographWhenBookHasNoEditors() {
        CrossRefDocument sampleBook = sampleBook();
        sampleBook.setEditor(null);
        assertThat(toPublication(sampleBook).getEntityDescription().getReference().getPublicationInstance().getClass(),
                   is(equalTo(AcademicMonograph.class)));
    }

    @Test
    @DisplayName("toPublication assigns roles to all authors and editors")
    void toPublicationAssignRolesToAuthorsAndEditors() {
        CrossRefDocument sampleBook = sampleBook();
        setAuthorWithMultipleAffiliations(sampleBook);
        setEditors(sampleBook);
        final List<Contributor> contributors = toPublication(sampleBook).getEntityDescription().getContributors();
        assertThat(contributors.size(), is(EXPECTED_NUMBER_OF_CONTRIBUTORS));
        assertTrue(contributors.stream().allMatch(this::hasRole));
    }

    @Test
    void toPublicationReturnsNullIsxnWhenInputIsInvalidIsxn() {
        String invalidIsxn = "not an isxn";
        CrossRefDocument crossRefDocument = crossRefDocumentWithInvalidIsxn(invalidIsxn);
        Publication publication = toPublication(crossRefDocument);
        List<String> isbnList = ((Book) publication.getEntityDescription()
                                            .getReference().getPublicationContext()).getIsbnList();
        assertThat(isbnList, is(empty()));
    }

    boolean hasRole(Contributor contributor) {
        return nonNull(contributor.getRole());
    }

    private static Instant sampleCrossrefDateAsInstant() {
        LocalDate date = LocalDate.of(EXPECTED_YEAR, EXPECTED_MONTH, EXPECTED_DAY);
        return date.atStartOfDay(ZoneId.systemDefault()).toInstant();
    }

    private List<CrossrefContributor> createAuthorListWithAnonymousAuthor(CrossRefDocument crossRefDocument) {
        List<CrossrefContributor> newContributors = new ArrayList<>(crossRefDocument.getAuthor());
        CrossrefContributor anonymousContributor = new CrossrefContributor();
        anonymousContributor.setSequence("5");
        anonymousContributor.setAffiliation(sampleAffiliation());
        newContributors.add(anonymousContributor);
        return newContributors;
    }

    private CrossRefDocument crossRefDocumentWithInvalidUrl(String invalidUri) {
        CrossRefDocument crossRefDocument = sampleBook();
        Link link = new Link();
        link.setUrl(invalidUri);
        crossRefDocument.setLink(List.of(link));
        return crossRefDocument;
    }

    private CrossRefDocument crossRefDocumentWithInvalidIsxn(String isxn) {
        CrossRefDocument crossRefDocument = sampleBook();
        crossRefDocument.setIsbn(List.of(isxn));
        return crossRefDocument;
    }

    private boolean isEditor(Contributor contributor) {
        return Role.EDITOR.equals(contributor.getRole().getType());
    }

    private void assertRequiredValuesAreConverted(Publication actualPublication) {
        assertThat(actualPublication.getEntityDescription().getMainTitle(), is(equalTo(SAMPLE_DOCUMENT_TITLE)));
        assertThat(actualPublication.getDoi(), is(equalTo(SOME_DOI_AS_URL)));
        assertThat(actualPublication.getLink(), is(equalTo(URI.create(SAMPLE_LINK))));
        assertThat(actualPublication.getCreatedDate(), is(equalTo(SAMPLE_CROSSREF_DATE_AS_INSTANT)));
        assertThat(actualPublication.getModifiedDate(), is(equalTo(SAMPLE_CROSSREF_DATE_AS_INSTANT)));
        assertThat(actualPublication.getPublishedDate(), is(equalTo(SAMPLE_CROSSREF_DATE_AS_INSTANT)));
    }

    private CrossrefDate sampleCrossrefDate() {
        int[][] dateParts = new int[1][DATE_SIZE];
        dateParts[0] = new int[]{EXPECTED_YEAR, EXPECTED_MONTH, EXPECTED_DAY};
        CrossrefDate crossrefDate = new CrossrefDate();
        crossrefDate.setDateParts(dateParts);
        return crossrefDate;
    }

    private Link sampleLink() {
        Link link = new Link();
        link.setUrl(SAMPLE_LINK);
        return link;
    }

    private Isxn sampleIsxn(IsxnType type, String value) {
        Isxn isxn = new Isxn();
        isxn.setType(type.getName());
        isxn.setValue(value);
        return isxn;
    }

    private Publication toPublication(CrossRefDocument doc) {
        return converter.toPublication(doc, OWNER, DOC_ID);
    }

    private CrossRefDocument sampleJournalArticle() {
        CrossRefDocument document = sampleCrossRefDocumentWithBasicMetadata();
        setPublicationTypeJournalArticle(document);
        return document;
    }

    private CrossRefDocument sampleBook() {
        CrossRefDocument document = sampleCrossRefDocumentWithBasicMetadata();
        setPublicationTypeBook(document);
        return document;
    }

    private CrossRefDocument sampleEditedBook() {
        CrossRefDocument document = sampleCrossRefDocumentWithBasicMetadata();
        setPublicationTypeEditedBook(document);
        return document;
    }

    private CrossRefDocument sampleBookChapter() {
        CrossRefDocument document = sampleCrossRefDocumentWithBasicMetadata();
        setPublicationTypeBookChapter(document);
        String pages = FIRST_PAGE_IN_RANGE + CONNECTING_MINUS + LAST_PAGE_IN_RANGE;
        document.setPage(pages);
        return document;
    }

    private CrossRefDocument sampleCrossRefDocumentWithBasicMetadata() {
        CrossRefDocument crossRefDocument = new CrossRefDocument();
        setAuthor(crossRefDocument);
        crossRefDocument.setPublisher(SAMPLE_PUBLISHER);
        crossRefDocument.setTitle(List.of(SAMPLE_DOCUMENT_TITLE, ALTERNATIVE_TITLE));
        crossRefDocument.setDoi(SOME_DOI);
        crossRefDocument.setUrl(SOME_DOI_AS_URL.toString());
        crossRefDocument.setLink(List.of(sampleLink()));
        CrossrefDate crossrefDate = sampleCrossrefDate();
        crossRefDocument.setCreated(crossrefDate);
        crossRefDocument.setDeposited(crossrefDate);
        crossRefDocument.setPublishedPrint(crossrefDate);
        crossRefDocument.setIndexed(crossrefDate);
        crossRefDocument.setIssued(crossrefDate);
        return crossRefDocument;
    }

    private void setPublicationTypeJournalArticle(CrossRefDocument document) {
        document.setType(CrossrefType.JOURNAL_ARTICLE.getType());
    }

    private void setPublicationTypeBook(CrossRefDocument document) {
        document.setType(CrossrefType.BOOK.getType());
    }

    private void setPublicationTypeEditedBook(CrossRefDocument document) {
        document.setType(CrossrefType.EDITED_BOOK.getType());
    }

    private void setPublicationTypeBookChapter(CrossRefDocument document) {
        document.setType(CrossrefType.BOOK_CHAPTER.getType());
    }

    private void setAuthor(CrossRefDocument document) {
        CrossrefContributor author = new CrossrefContributor.Builder().withGivenName(AUTHOR_GIVEN_NAME)
                                         .withFamilyName(AUTHOR_FAMILY_NAME)
                                         .withSequence(FIRST_AUTHOR).build();
        CrossrefContributor secondAuthor = new CrossrefContributor.Builder().withGivenName(AUTHOR_GIVEN_NAME)
                                               .withFamilyName(AUTHOR_FAMILY_NAME)
                                               .withSequence(SECOND_AUTHOR).build();
        document.setAuthor(Arrays.asList(author, secondAuthor));
    }

    private void setAuthorWithAffiliation(CrossRefDocument document) {
        CrossrefContributor author = new CrossrefContributor.Builder().withGivenName(AUTHOR_GIVEN_NAME)
                                         .withFamilyName(AUTHOR_FAMILY_NAME)
                                         .withSequence(FIRST_AUTHOR).build();
        CrossrefContributor secondAuthor = new CrossrefContributor.Builder().withGivenName(AUTHOR_GIVEN_NAME)
                                               .withFamilyName(AUTHOR_FAMILY_NAME)
                                               .withAffiliation(sampleAffiliation())
                                               .withSequence(SECOND_AUTHOR).build();
        document.setAuthor(Arrays.asList(author, secondAuthor));
    }

    private void setAuthorWithMultipleAffiliations(CrossRefDocument document) {
        CrossrefContributor author = new CrossrefContributor.Builder().withGivenName(AUTHOR_GIVEN_NAME)
                                         .withFamilyName(AUTHOR_FAMILY_NAME)
                                         .withSequence(FIRST_AUTHOR).build();
        CrossrefContributor secondAuthor = new CrossrefContributor.Builder().withGivenName(AUTHOR_GIVEN_NAME)
                                               .withFamilyName(AUTHOR_FAMILY_NAME)
                                               .withAffiliation(sampleMultipleAffiliations())
                                               .withSequence(SECOND_AUTHOR).build();
        document.setAuthor(Arrays.asList(author, secondAuthor));
    }

    private void setAuthorWithUnauthenticatedOrcid(CrossRefDocument document) {
        CrossrefContributor author = new CrossrefContributor.Builder()
                                         .withGivenName(AUTHOR_GIVEN_NAME)
                                         .withFamilyName(AUTHOR_FAMILY_NAME)
                                         .withSequence(FIRST_AUTHOR)
                                         .withOrcid(SAMPLE_ORCID)
                                         .withAuthenticatedOrcid(false)
                                         .build();
        CrossrefContributor secondAuthor = new CrossrefContributor.Builder().withGivenName(AUTHOR_GIVEN_NAME)
                                               .withFamilyName(AUTHOR_FAMILY_NAME)
                                               .withSequence(SECOND_AUTHOR).build();
        document.setAuthor(Arrays.asList(author, secondAuthor));
    }

    private List<CrossrefAffiliation> sampleAffiliation() {
        CrossrefAffiliation affiliation = new CrossrefAffiliation();
        affiliation.setName("affiliationName");
        return List.of(affiliation);
    }

    private List<CrossrefAffiliation> sampleMultipleAffiliations() {
        CrossrefAffiliation firstAffiliation = new CrossrefAffiliation();
        firstAffiliation.setName("firstAffiliationName");
        CrossrefAffiliation secondAffiliation = new CrossrefAffiliation();
        secondAffiliation.setName("secondAffiliationName");
        return List.of(firstAffiliation, secondAffiliation);
    }

    private void setEditors(CrossRefDocument document) {
        CrossrefContributor editor = new CrossrefContributor.Builder().withGivenName(AUTHOR_GIVEN_NAME)
                                         .withFamilyName(AUTHOR_FAMILY_NAME)
                                         .withSequence(FIRST_AUTHOR).build();
        CrossrefContributor secondEditor = new CrossrefContributor.Builder().withGivenName(AUTHOR_GIVEN_NAME)
                                               .withFamilyName(AUTHOR_FAMILY_NAME)
                                               .withSequence(SECOND_AUTHOR).build();
        document.setEditor(Arrays.asList(editor, secondEditor));
    }

    private int startCountingFromOne(int i) {
        return i + 1;
    }

    private Book convertAndGetBookContext(CrossRefDocument sampleBook) {
        return (Book) toPublication(sampleBook).getEntityDescription()
                          .getReference()
                          .getPublicationContext();
    }

    private Set<String> constructExpectedIsbnValues(List<Isxn> isbns) {
        return isbns.stream()
                   .map(Isxn::getValue)
                   .map(ISBN_VALIDATOR::validate)
                   .filter(Objects::nonNull)
                   .collect(Collectors.toSet());
    }

    private CrossRefDocument addIsbnsToBook(CrossRefDocument sampleBook, String... isxnStrings) {
        IsxnType type = IsxnType.PRINT;
        List<Isxn> isbns = Arrays.stream(isxnStrings)
                               .map(isxnString -> sampleIsxn(type, isxnString))
                               .collect(Collectors.toList());
        sampleBook.setIsbnType(isbns);
        return sampleBook;
    }

    private CrossRefDocument addIssnsToJournalArticle(CrossRefDocument sampleJournalArticle,
                                                      IsxnType type,
                                                      String... isxnStrings) {
        List<Isxn> issns = Arrays.stream(isxnStrings)
                               .map(isxnString -> sampleIsxn(type, isxnString))
                               .collect(Collectors.toList());

        sampleJournalArticle.setIssnType(issns);
        return sampleJournalArticle;
    }

    private UnconfirmedJournal getJournalContext(Publication actualPublication) {
        return (UnconfirmedJournal) actualPublication.getEntityDescription()
                                        .getReference()
                                        .getPublicationContext();
    }

    private List<Corporation> getOrganisations(List<Contributor> contributors) {
        return contributors.stream()
                   .map(Contributor::getAffiliations)
                   .filter(Objects::nonNull)
                   .flatMap(Collection::stream)
                   .collect(Collectors.toList());
    }

    private List<String> getPoolOfExpectedValues(List<Isxn> issns) {
        return issns.stream()
                   .map(Isxn::getValue)
                   .map(IssnCleaner::clean)
                   .filter(Objects::nonNull)
                   .collect(Collectors.toList());
    }
}
