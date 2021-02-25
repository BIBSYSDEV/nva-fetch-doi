package no.unit.nva.doi.transformer;

import com.fasterxml.jackson.databind.ObjectMapper;
import no.unit.nva.doi.fetch.exceptions.UnsupportedDocumentTypeException;
import no.unit.nva.doi.transformer.language.LanguageMapper;
import no.unit.nva.doi.transformer.language.exceptions.LanguageUriNotFoundException;
import no.unit.nva.doi.transformer.model.crossrefmodel.CrossRefDocument;
import no.unit.nva.doi.transformer.model.crossrefmodel.CrossrefAffiliation;
import no.unit.nva.doi.transformer.model.crossrefmodel.CrossrefApiResponse;
import no.unit.nva.doi.transformer.model.crossrefmodel.CrossrefContributor;
import no.unit.nva.doi.transformer.model.crossrefmodel.CrossrefDate;
import no.unit.nva.doi.transformer.model.crossrefmodel.CrossrefReview;
import no.unit.nva.doi.transformer.model.crossrefmodel.Isxn;
import no.unit.nva.doi.transformer.model.crossrefmodel.Isxn.IsxnType;
import no.unit.nva.doi.transformer.model.crossrefmodel.Link;
import no.unit.nva.doi.transformer.utils.CrossrefType;
import no.unit.nva.model.Contributor;
import no.unit.nva.model.Identity;
import no.unit.nva.model.Organization;
import no.unit.nva.model.Publication;
import no.unit.nva.model.PublicationDate;
import no.unit.nva.model.Role;
import no.unit.nva.model.contexttypes.Book;
import no.unit.nva.model.contexttypes.Chapter;
import no.unit.nva.model.contexttypes.Journal;
import no.unit.nva.model.exceptions.InvalidIsbnException;
import no.unit.nva.model.exceptions.InvalidIssnException;
import no.unit.nva.model.instancetypes.book.BookAnthology;
import no.unit.nva.model.instancetypes.book.BookMonograph;
import no.unit.nva.model.instancetypes.chapter.ChapterArticle;
import no.unit.nva.model.instancetypes.journal.JournalArticle;
import no.unit.nva.model.pages.Pages;
import no.unit.nva.model.pages.Range;
import nva.commons.core.JsonUtils;
import nva.commons.core.ioutils.IoUtils;
import nva.commons.doi.DoiConverter;
import org.apache.commons.validator.routines.ISBNValidator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Path;
import java.time.Instant;
import java.time.LocalDate;
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

import static java.util.Objects.nonNull;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.collection.IsIterableContainingInOrder.contains;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.hamcrest.core.IsNot.not;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.hamcrest.core.IsNull.nullValue;
import static org.hamcrest.core.StringContains.containsString;
import static org.hamcrest.text.IsEmptyString.emptyString;
import static org.hamcrest.text.MatchesPattern.matchesPattern;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class CrossRefConverterTest extends ConversionTest {

    public static final String AUTHOR_GIVEN_NAME = "givenName";
    public static final String AUTHOR_FAMILY_NAME = "familyName";
    public static final String FIRST_AUTHOR = "first";
    public static final Integer EXPECTED_YEAR = 2019;
    public static final String SURNAME_COMMA_FIRSTNAME = "%s,.*%s";
    public static final String NOT_JOURNAL_ARTICLE = "dissertation";
    public static final String SECOND_AUTHOR = "second";
    public static final String CROSSREF_WITH_ABSTRACT_JSON = "crossrefWithAbstract.json";
    public static final String ENG_ISO_639_3 = "eng";
    public static final String SOME_DOI = "10.1000/182";
    public static final URI SOME_DOI_AS_URL = URI.create("http://dx.doi.org/10.1007/978-3-030-22507-0_9");
    public static final String VALID_ISSN_A = "0306-4379";
    public static final String VALID_ISSN_B = "1066-8888";
    public static final String VALID_ISBN_A = "9788202529819";
    public static final String VALID_ISBN_B = "978-82-450-0364-2";
    public static final int EXPECTED_MONTH = 2;
    public static final int EXPECTED_DAY = 20;
    public static final ISBNValidator ISBN_VALIDATOR = new ISBNValidator();
    public static final String SAMPLE_CONTAINER_TITLE = "Container Title";
    public static final String SAMPLE_PUBLISHER = "Sample Publisher Inc";
    public static final String SAMPLE_LINK = "https://localhost/some.link";
    public static final String REVIEW_RECOMMENDATION = "GO";
    public static final String CONNECTING_MINUS = "-";
    public static final String SAMPLE_ISSUE = "SomeIssue";
    public static final String SOME_STRANGE_CROSSREF_TYPE = "SomeStrangeCrossrefType";
    public static final String ORDINAL_SECOND = "second";
    public static final String FIRST_NAME_OF_JOURNAL = "Journal 1st Name";
    public static final String FIRST_PAGE_IN_RANGE = "45";
    public static final String LAST_PAGE_IN_RANGE = "89";
    public static final String EXPECTED_TAG = "subject1";
    public static final String SECOND_NAME_OF_JOURNAL = "Journal 2nd Name";
    public static final String EXPECTED_VOLUME = "Vol. 1";
    public static final String CROSSREF = "Crossref";
    public static final String SOURCE_URI = "http://www.something.com";
    private static final int DATE_SIZE = 3;
    private static final String SAMPLE_DOCUMENT_TITLE = "Sample document title";
    private static final Integer NUMBER_OF_SAMPLE_AUTHORS = 2;
    private static final UUID DOC_ID = UUID.randomUUID();
    private static final String OWNER = "TheOwner";
    private static final String INVALID_ORDINAL = "invalid ordinal";
    private static final String PROCESSED_ABSTRACT = "processedAbstract.txt";
    private static final String SAMPLE_ORCID = "http://orcid.org/0000-1111-2222-3333";
    private static final Instant SAMPLE_CROSSREF_DATE_AS_INSTANT = sampleCrossrefDateAsInstant();
    private static final ObjectMapper objectMapper = JsonUtils.objectMapper;
    private final CrossRefConverter converter = new CrossRefConverter();

    private static Instant sampleCrossrefDateAsInstant() {
        LocalDate date = LocalDate.of(EXPECTED_YEAR, EXPECTED_MONTH, EXPECTED_DAY);
        return Instant.ofEpochSecond(date.toEpochDay() * 86400L);
    }

    @Test
    @DisplayName("An empty CrossRef document throws IllegalArgument exception")
    public void anEmptyCrossRefDocumentThrowsIllegalArgumentException() throws IllegalArgumentException {
        CrossRefDocument crossRefDocument = new CrossRefDocument();
        IllegalArgumentException exception =
                assertThrows(IllegalArgumentException.class, () -> toPublication(crossRefDocument));
        assertThat(exception.getMessage(), is(CrossRefConverter.INVALID_ENTRY_ERROR));
    }

    @Test
    @DisplayName("An CrossRef document with unknown type throws IllegalArgument exception")
    public void anCrossRefDocumentWithUnknownTypeThrowsIllegalArgumentException() throws IllegalArgumentException {
        CrossRefDocument crossRefDocument = sampleCrossRefDocumentWithBasicMetadata();
        crossRefDocument.setType(SOME_STRANGE_CROSSREF_TYPE);
        UnsupportedDocumentTypeException exception =
                assertThrows(UnsupportedDocumentTypeException.class, () -> toPublication(crossRefDocument));
        String expectedError = String.format(CrossRefConverter.UNRECOGNIZED_TYPE_MESSAGE, SOME_STRANGE_CROSSREF_TYPE);
        assertThat(exception.getMessage(), is(equalTo(expectedError)));
    }

    @Test
    @DisplayName("An CrossRef document without type throws IllegalArgument exception")
    public void anCrossRefDocumentWithoutTypeThrowsIllegalArgumentException() throws IllegalArgumentException {
        CrossRefDocument crossRefDocument = sampleCrossRefDocumentWithBasicMetadata();
        crossRefDocument.setType(null);
        UnsupportedDocumentTypeException exception =
                assertThrows(UnsupportedDocumentTypeException.class, () -> toPublication(crossRefDocument));
        String expectedError = String.format(CrossRefConverter.UNRECOGNIZED_TYPE_MESSAGE, "null");
        assertThat(exception.getMessage(), is(equalTo(expectedError)));
    }

    @Test
    @DisplayName("The creator's name in the publication contains first family and then given name")
    public void creatorsNameContainsFirstFamilyAndThenGivenName()
            throws IllegalArgumentException, InvalidIssnException,
            InvalidIsbnException, UnsupportedDocumentTypeException {

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

        String expectedNameRegEx = String.format(SURNAME_COMMA_FIRSTNAME, familyName, givenName);
        assertThat(actualName, matchesPattern(expectedNameRegEx));
    }

    @Test
    @DisplayName("The earliest year found in the \"published-print\" field is stored in the entity description.")
    public void entityDescriptionContainsTheEarliestYearFoundInPublishedPrintField()
            throws InvalidIssnException, InvalidIsbnException, UnsupportedDocumentTypeException {
        CrossRefDocument sampleJournalArticle = sampleJournalArticle();
        Publication samplePublication = toPublication(sampleJournalArticle);
        String actualYear = samplePublication.getEntityDescription().getDate().getYear();
        assertThat(actualYear, is(equalTo(EXPECTED_YEAR.toString())));
    }

    @Test
    @DisplayName("toPublication sets entityDescription.date to null when inputdata has no PublicationDate")
    public void toPublicationSetsEntityDescriptionDateToNullWhenInputDataHasNoPublicationDate()
            throws InvalidIssnException, InvalidIsbnException, UnsupportedDocumentTypeException {

        CrossRefDocument sampleJournalArticle = sampleJournalArticle();
        sampleJournalArticle.setIssued(null);
        Publication publicationWithoutDate = toPublication(sampleJournalArticle);
        PublicationDate actualDate = publicationWithoutDate.getEntityDescription().getDate();
        assertThat(actualDate, is(nullValue()));
    }

    @Test
    @DisplayName("toPublication sets PublicationContext to Journal when the input has the tag \"journal-article\"")
    public void toPublicationSetsPublicationContextToJournalWhenTheInputHasTheTagJournalArticle()
            throws InvalidIssnException, InvalidIsbnException, UnsupportedDocumentTypeException {
        Publication samplePublication = toPublication(sampleJournalArticle());
        assertThat(samplePublication.getEntityDescription().getReference().getPublicationContext().getClass(),
                is(equalTo(Journal.class)));
    }

    @Test
    @DisplayName("toPublication throws Exception when the input does not have the tag \"journal-article\"")
    public void toPublicationThrowsExceptionWhenTheInputDoesNotHaveTheTagJournalArticle() {
        CrossRefDocument sampleDocumentJournalArticle = sampleJournalArticle();
        sampleDocumentJournalArticle.setType(NOT_JOURNAL_ARTICLE);
        String expectedError = String.format(CrossRefConverter.UNRECOGNIZED_TYPE_MESSAGE, NOT_JOURNAL_ARTICLE);
        UnsupportedDocumentTypeException exception = assertThrows(UnsupportedDocumentTypeException.class,
            () -> toPublication(sampleDocumentJournalArticle));
        assertThat(exception.getMessage(), is(equalTo(expectedError)));
    }

    @Test
    @DisplayName("toPublication sets as sequence the position of the author in the list when ordinal is not numerical")
    public void toPublicationSetsOrdinalAsSecondAuthorIfInputOrdinalIsNotAValidOrdinal()
            throws InvalidIssnException, InvalidIsbnException, UnsupportedDocumentTypeException {
        CrossRefDocument sampleJournalArticle = sampleJournalArticle();
        int numberOfAuthors = sampleJournalArticle.getAuthor().size();
        sampleJournalArticle.getAuthor().forEach(a -> a.setSequence(INVALID_ORDINAL));
        List<Integer> ordinals = toPublication(sampleJournalArticle).getEntityDescription().getContributors().stream()
                .map(Contributor::getSequence).collect(Collectors.toList());
        assertThat(ordinals.size(), is(numberOfAuthors));
        List<Integer> expectedValues = IntStream.range(0, numberOfAuthors)
                .map(this::startCountingFromOne)
                .boxed()
                .collect(Collectors.toList());
        assertThat(ordinals, contains(expectedValues.toArray()));
    }

    @Test
    @DisplayName("toPublication sets the correct number when the sequence ordinal is valid")
    public void toPublicationSetsCorrectNumberForValidOrdinal()
            throws InvalidIssnException, InvalidIsbnException, UnsupportedDocumentTypeException {
        CrossRefDocument sampleJournalArticle = sampleJournalArticle();
        CrossrefContributor author = sampleJournalArticle.getAuthor().stream().findFirst().get();
        String validOrdinal = ORDINAL_SECOND;
        int expected = 2;
        author.setSequence(validOrdinal);
        int actual = toPublication(sampleJournalArticle)
                .getEntityDescription().getContributors().stream().findFirst().get().getSequence();
        assertThat(actual, is(equalTo(expected)));
    }

    @Test
    @DisplayName("toPublication sets abstract when input has non empty abstract")
    public void toPublicationSetsAbstractWhenInputHasNonEmptyAbstract()
            throws IOException, InvalidIssnException, InvalidIsbnException, UnsupportedDocumentTypeException {
        String json = IoUtils.stringFromResources(Path.of(CROSSREF_WITH_ABSTRACT_JSON));
        CrossRefDocument crossRefDocument = objectMapper.readValue(json, CrossrefApiResponse.class).getMessage();
        String abstractText = toPublication(crossRefDocument).getEntityDescription().getAbstract();
        assertThat(abstractText, is(not(emptyString())));
        String expectedAbstract = IoUtils.stringFromResources(Path.of(PROCESSED_ABSTRACT));
        assertThat(abstractText, is(equalTo(expectedAbstract)));
    }

    @Test
    @DisplayName("toPublication sets the language to a URI when the input is an ISO639-3 entry")
    public void toPublicationSetsTheLanguageToAUriWhenTheInputFollowsTheIso3Standard()
            throws LanguageUriNotFoundException, InvalidIssnException,
            InvalidIsbnException, UnsupportedDocumentTypeException {
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
    public void toPublicationSetsTheDoiOfTheReferenceWhenTheCrossrefDocHasADoiValue()
            throws InvalidIssnException, InvalidIsbnException, UnsupportedDocumentTypeException {
        DoiConverter doiConverter = new DoiConverter();
        CrossRefDocument sampleJournalArticle = sampleJournalArticle();
        sampleJournalArticle.setDoi(SOME_DOI);
        URI actualDoi = toPublication(sampleJournalArticle).getEntityDescription().getReference().getDoi();
        assertThat(actualDoi, is(equalTo(doiConverter.toUri(SOME_DOI))));
    }

    @Test
    @DisplayName("toPublication sets the doi of the Reference when the Crossref document has at least one"
            + " \"Container\" value ")
    public void toPublicationSetsTheNameOfTheReferenceWhenTheCrossrefDocHasAtLeatOneContainterTitle()
            throws InvalidIssnException, InvalidIsbnException, UnsupportedDocumentTypeException {
        CrossRefDocument sampleJournalArticle = sampleJournalArticle();
        sampleJournalArticle.setContainerTitle(Arrays.asList(FIRST_NAME_OF_JOURNAL, SECOND_NAME_OF_JOURNAL));
        String actualJournalName = ((Journal) toPublication(sampleJournalArticle)
                .getEntityDescription().getReference().getPublicationContext()).getTitle();
        assertThat(actualJournalName, is(equalTo(FIRST_NAME_OF_JOURNAL)));
    }

    @Test
    @DisplayName("toPublication sets the volume of the Reference when the Crosref document has a \"Volume\" value")
    public void toPublicationSetsTheVolumeOfTheReferenceWhentheCrossrefDocHasAVolume()
            throws InvalidIssnException, InvalidIsbnException, UnsupportedDocumentTypeException {
        CrossRefDocument sampleJournalArticle = sampleJournalArticle();
        sampleJournalArticle.setVolume(EXPECTED_VOLUME);
        String actualVolume = ((JournalArticle) (toPublication(sampleJournalArticle)
                .getEntityDescription().getReference()
                .getPublicationInstance()))
                .getVolume();
        assertThat(actualVolume, is(equalTo(EXPECTED_VOLUME)));
    }

    @Test
    @DisplayName("toPublication sets the pages of the Reference when the Crosref document has a \"Pages\" value")
    public void toPublicationSetsThePagesOfTheReferenceWhentheCrossrefDocHasPages()
            throws InvalidIssnException, InvalidIsbnException, UnsupportedDocumentTypeException {
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
    @DisplayName("toPublication sets the issue of the Reference when the Crosref document has a \"Issue\" value")
    public void toPublicationSetsTheIssueOfTheReferenceWhentheCrossrefDocHasAnIssueValue()
            throws InvalidIssnException, InvalidIsbnException, UnsupportedDocumentTypeException {
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
    public void toPublicationSetsTheMetadataSourceToTheCrossRefUrlWhenTheCrossrefDocHasCrossrefAsSource()
            throws InvalidIssnException, InvalidIsbnException, UnsupportedDocumentTypeException {
        URI expectedURI = CrossRefConverter.CROSSEF_URI;
        CrossRefDocument sampleDocumentJournalArticle = sampleJournalArticle();
        sampleDocumentJournalArticle.setSource(CROSSREF);
        URI actualSource = toPublication(sampleDocumentJournalArticle).getEntityDescription().getMetadataSource();
        assertThat(actualSource, is(equalTo(expectedURI)));
    }

    @Test
    @DisplayName("toPublication sets the MetadataSource to the specfied URL when the Crossref "
            + "document has as \"source\" a valid URL")
    public void toPublicationSetsTheMetadataSourceToTheSourceUrlIfTheDocHasAsSourceAValidUrl()
            throws InvalidIssnException, InvalidIsbnException, UnsupportedDocumentTypeException {
        URI expectedURI = URI.create(SOURCE_URI);
        CrossRefDocument sampleJournalArticle = sampleJournalArticle();
        sampleJournalArticle.setSource(SOURCE_URI);
        URI actualSource = toPublication(sampleJournalArticle).getEntityDescription().getMetadataSource();
        assertThat(actualSource, is(equalTo(expectedURI)));
    }

    @Test
    @DisplayName("toPublication sets null for online ISSN when only print ISSN is available")
    public void toPublicationSetsNullOnlineIssnWhenOnlyPrintISSnIsAvailable()
            throws InvalidIssnException, InvalidIsbnException, UnsupportedDocumentTypeException {
        IsxnType type = IsxnType.PRINT;
        Isxn printIssn = sampleIsxn(type, VALID_ISSN_A);
        CrossRefDocument sampleJournalArticle = sampleJournalArticle();
        sampleJournalArticle.setIssnType(Collections.singletonList(printIssn));
        Publication actualPublication = toPublication(sampleJournalArticle);
        Journal actualPublicationContext = (Journal) actualPublication.getEntityDescription()
                .getReference()
                .getPublicationContext();
        String onlineIssn = actualPublicationContext.getOnlineIssn();
        assertThat(onlineIssn, is(equalTo(null)));
    }

    @Test
    @DisplayName("toPublication sets null for print ISSN when only online ISSN is available")
    public void toPublicationSetsNullPrintIssnWhenOnlyOnlineISSnIsAvailable()
            throws InvalidIssnException, InvalidIsbnException, UnsupportedDocumentTypeException {
        IsxnType type = IsxnType.ELECTRONIC;
        Isxn onlineIssn = sampleIsxn(type, VALID_ISSN_A);
        CrossRefDocument sampleJournalArticle = sampleJournalArticle();
        sampleJournalArticle.setIssnType(Collections.singletonList(onlineIssn));
        Publication actualPublication = toPublication(sampleJournalArticle);
        Journal actualPublicationContext = (Journal) actualPublication.getEntityDescription()
                .getReference()
                .getPublicationContext();
        String actualPrintIssn = actualPublicationContext.getPrintIssn();
        assertThat(actualPrintIssn, is(equalTo(null)));
    }

    @Test
    @DisplayName("toPublication sets one of many online ISSNs when multiple online ISSNs are  available")
    public void toPublicationSetsOneOfManyOnlineIssnsWhenMultipleOfTheSameTypeAreAvailable()
            throws InvalidIssnException, InvalidIsbnException, UnsupportedDocumentTypeException {
        IsxnType type = IsxnType.ELECTRONIC;
        Isxn onlineIssnA = sampleIsxn(type, VALID_ISSN_A);
        Isxn onlineIssnB = sampleIsxn(type, VALID_ISSN_B);
        List<Isxn> issns = Arrays.asList(onlineIssnA, onlineIssnB);
        CrossRefDocument sampleJournalArticle = sampleJournalArticle();
        sampleJournalArticle.setIssnType(issns);
        Publication actualPublication = toPublication(sampleJournalArticle);
        Journal actualPublicationContext = (Journal) actualPublication.getEntityDescription()
                .getReference()
                .getPublicationContext();
        String actualOnlineIssn = actualPublicationContext.getOnlineIssn();
        List<String> poolOfExpectedValues = issns.stream().map(Isxn::getValue).collect(Collectors.toList());
        assertThat(poolOfExpectedValues, hasItem(actualOnlineIssn));
    }

    @Test
    @DisplayName("toPublication sets one of many print ISSNs when multiple print ISSNs are available")
    public void toPublicationSetsOneOfManyPrintIssnsWhenMultipleOfTheSameTypeAreAvailable()
            throws InvalidIssnException, InvalidIsbnException, UnsupportedDocumentTypeException {
        IsxnType type = IsxnType.PRINT;
        Isxn printIssnA = sampleIsxn(type, VALID_ISSN_A);
        Isxn printIssnB = sampleIsxn(type, VALID_ISSN_B);
        List<Isxn> issns = Arrays.asList(printIssnA, printIssnB);
        CrossRefDocument sampleJournalArticle = sampleJournalArticle();
        sampleJournalArticle.setIssnType(issns);
        Publication actualPublication = toPublication(sampleJournalArticle);
        Journal actualPublicationContext = (Journal) actualPublication.getEntityDescription()
                .getReference()
                .getPublicationContext();
        String actualPrintIssn = actualPublicationContext.getPrintIssn();
        List<String> poolOfExpectedValues = issns.stream().map(Isxn::getValue).collect(Collectors.toList());
        assertThat(poolOfExpectedValues, hasItem(actualPrintIssn));
    }

    @Test
    @DisplayName("toPublication sets tags when subject are available")
    public void toPublicationSetsTagsWhenSubjectAreAvailable()
            throws InvalidIssnException, InvalidIsbnException, UnsupportedDocumentTypeException {
        List<String> subject = List.of(EXPECTED_TAG);
        CrossRefDocument sampleJournalArticle = sampleJournalArticle();
        sampleJournalArticle.setSubject(subject);
        List<String> actualTags = toPublication(sampleJournalArticle).getEntityDescription().getTags();
        assertThat(actualTags, hasItem(EXPECTED_TAG));
    }

    @Test
    @DisplayName("toPublication sets tags to empty list when subject are not available")
    public void toPublicationSetsTagsToEmptyListWhenSubjectAreNotAvailable()
            throws InvalidIssnException, InvalidIsbnException, UnsupportedDocumentTypeException {
        CrossRefDocument sampleJournalArticle = sampleJournalArticle();
        sampleJournalArticle.setSubject(null);
        List<String> actualTags = toPublication(sampleJournalArticle).getEntityDescription().getTags();
        assertTrue(actualTags.isEmpty());
    }

    @Test
    @DisplayName("toPublication preserves all date parts when they are available")
    public void toPublicationPreservesAllDatepartsWhenTheyAreAvailable()
            throws InvalidIssnException, InvalidIsbnException, UnsupportedDocumentTypeException {
        CrossrefDate crossrefDate = sampleCrossrefDate();
        CrossRefDocument sampleJournalArticle = sampleJournalArticle();
        sampleJournalArticle.setIssued(crossrefDate);
        PublicationDate publicationDate = toPublication(sampleJournalArticle).getEntityDescription().getDate();
        assertEquals("" + EXPECTED_YEAR, publicationDate.getYear());
        assertEquals("" + EXPECTED_MONTH, publicationDate.getMonth());
        assertEquals("" + EXPECTED_DAY, publicationDate.getDay());
    }

    @Test
    @DisplayName("toPublication sets affiliation labels when author.affiliation.name is available")
    public void toPublicationSetsAffiliationLabelWhenAuthorHasAffiliationName()
            throws InvalidIssnException, InvalidIsbnException, UnsupportedDocumentTypeException {
        CrossRefDocument sampleJournalArticle = sampleJournalArticle();
        setAuthorWithAffiliation(sampleJournalArticle);
        List<Contributor> contributors = toPublication(sampleJournalArticle).getEntityDescription().getContributors();
        List<List<Organization>> organisations = contributors.stream()
                .map(Contributor::getAffiliations)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
        assertFalse(organisations.isEmpty());
    }

    @Test
    @DisplayName("toPublication sets multiple affiliation labels when author has more affiliations name")
    public void toPublicationSetsMultipleAffiliationLabelWhenAuthorHasMultipleAffiliationName()
            throws InvalidIssnException, InvalidIsbnException, UnsupportedDocumentTypeException {
        CrossRefDocument sampleJournalArticle = sampleJournalArticle();
        setAuthorWithMultipleAffiliations(sampleJournalArticle);
        List<Contributor> contributors = toPublication(sampleJournalArticle).getEntityDescription().getContributors();
        List<Organization> organisations =
                contributors.stream()
                        .map(Contributor::getAffiliations)
                        .filter(Objects::nonNull)
                        .flatMap(Collection::stream)
                        .collect(Collectors.toList());
        assertTrue(organisations.size() > 1);
    }

    @Test
    @DisplayName("toPublication sets affiliation to empty list when author has no affiliation")
    public void toPublicationSetsAffiliationToEmptyListWhenAuthorHasNoAffiliatio()
            throws InvalidIssnException, InvalidIsbnException, UnsupportedDocumentTypeException {
        List<Contributor> contributors = toPublication(sampleJournalArticle()).getEntityDescription().getContributors();
        List<List<Organization>> organisations = contributors.stream()
                .map(Contributor::getAffiliations)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
        assertTrue(organisations.isEmpty());
    }

    @Test
    @DisplayName("toPublication preserves Orcid when Author has orcid")
    public void toPublicationPreservesOrcidWhenAuthorHasOrcid()
            throws InvalidIssnException, InvalidIsbnException, UnsupportedDocumentTypeException {

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
    public void toPublicationSetsPublicationContextToBookWhjenCrossrefDocumentHasTypeBook()
            throws InvalidIssnException, InvalidIsbnException, UnsupportedDocumentTypeException {
        var publicationContext = toPublication(sampleBook())
                .getEntityDescription().getReference().getPublicationContext();
        assertTrue(publicationContext instanceof Book);
    }

    @Test
    @DisplayName("toPublication sets ISBN when crossref-type is book")
    public void toPublicationSetsIsbnWhenCrossrefDocumentIsTypeBook()
            throws InvalidIssnException, InvalidIsbnException, UnsupportedDocumentTypeException {
        CrossRefDocument sampleBook = sampleBook();

        IsxnType type = IsxnType.PRINT;
        Isxn printIsbnA = sampleIsxn(type, VALID_ISBN_A);
        Isxn printIsbnB = sampleIsxn(type, VALID_ISBN_B);
        List<Isxn> isbns = Arrays.asList(printIsbnA, printIsbnB);
        sampleBook.setIsbnType(isbns);
        Book actualPublicationContext = (Book) toPublication(sampleBook).getEntityDescription()
                .getReference()
                .getPublicationContext();
        Set<String> actualValues = new HashSet<>(actualPublicationContext.getIsbnList());
        Set<String> expectedValues = isbns.stream()
                .map(Isxn::getValue)
                .map(ISBN_VALIDATOR::validate)
                .collect(Collectors.toSet());
        assertEquals(actualValues, expectedValues);
    }

    @Test
    @DisplayName("toPublication sets seriesTitle in PublicationContext when CrossrefDocument has ContainerTitle")
    public void toPublicationSetsSeriesTitleInPublicationContextWhenCrossrefDocumentHasContainerTitle()
            throws InvalidIssnException, InvalidIsbnException, UnsupportedDocumentTypeException {
        CrossRefDocument sampleBook = sampleBook();
        sampleBook.setContainerTitle(List.of(SAMPLE_CONTAINER_TITLE));
        var publicationContext = toPublication(sampleBook)
                .getEntityDescription().getReference().getPublicationContext();
        String actualSeriesTitle = ((Book) publicationContext).getSeriesTitle();
        assertThat(actualSeriesTitle, is(equalTo(SAMPLE_CONTAINER_TITLE)));
    }

    @Test
    @DisplayName("toPublication sets Publisher in PublicationContext when CrossrefDocument has Publisher")
    public void toPublicationSetsPublisherInPublicationContextWhenCrossrefDocumentHasPublisher()
            throws InvalidIssnException, InvalidIsbnException, UnsupportedDocumentTypeException {
        CrossRefDocument sampleBook = sampleBook();
        sampleBook.setPublisher(SAMPLE_PUBLISHER);
        var publicationContext = toPublication(sampleBook)
                .getEntityDescription().getReference().getPublicationContext();
        String actualPublisher = ((Book) publicationContext).getPublisher();
        assertThat(actualPublisher, is(equalTo(SAMPLE_PUBLISHER)));
    }

    @Test
    @DisplayName("toPublication sets PeerReviewed to True PublicationContext when CrossrefDocument has Reviewer")
    public void toPublicationSetsPeerReviewedInPublicationContextWhenCrossrefDocumentHasReviewer()
            throws InvalidIssnException, InvalidIsbnException, UnsupportedDocumentTypeException {
        CrossRefDocument sampleBook = sampleBook();
        CrossrefReview crossrefReview = new CrossrefReview();
        crossrefReview.setRecommendation(REVIEW_RECOMMENDATION);
        sampleBook.setReview(crossrefReview);
        var publicationContext = toPublication(sampleBook)
                .getEntityDescription().getReference().getPublicationContext();
        assertTrue(((Book) publicationContext).isPeerReviewed());
    }

    @Test
    @DisplayName("toPublication sets Url in PublicationContext when CrossrefDocument has link")
    public void toPublicationSetsUrlInPublicationContextWhenCrossrefDocumentHasLink()
            throws InvalidIssnException, InvalidIsbnException, UnsupportedDocumentTypeException {
        CrossRefDocument sampleBook = sampleBook();
        sampleBook.setLink(List.of(sampleLink()));
        var publicationContext = toPublication(sampleBook)
                .getEntityDescription().getReference().getPublicationContext();
        var actualLink = ((Book) publicationContext).getUrl();
        assertThat(actualLink.toString(), is(equalTo(SAMPLE_LINK)));
    }

    @Test
    @DisplayName("toPublication handles all interesting and required fields in CrossrefDocument for journal-article")
    public void toPublicationHandlesAllInterestingAndRequiredFieldsInCrossrefDocumentForJournalArticle()
            throws InvalidIssnException, InvalidIsbnException, UnsupportedDocumentTypeException {
        assertRequiredValuesAreConverted(toPublication(sampleJournalArticle()));
    }

    @Test
    @DisplayName("toPublication handles all interesting and required fields in CrossrefDocument for book")
    public void toPublicationHandlesAllInterestingAndRequiredFieldsInCrossrefDocumentForBook()
            throws InvalidIssnException, InvalidIsbnException, UnsupportedDocumentTypeException {
        assertRequiredValuesAreConverted(toPublication(sampleBook()));
    }

    @Test
    @DisplayName("toPublication handles all interesting and required fields in CrossrefDocument for book-chapter")
    public void toPublicationHandlesAllInterestingAndRequiredFieldsInCrossrefDocumentForBookChapter()
            throws InvalidIssnException, InvalidIsbnException, UnsupportedDocumentTypeException {
        assertRequiredValuesAreConverted(toPublication(sampleBookChapter()));
    }

    @Test
    @DisplayName("toPublication set publicationContext when input Crossref document is BookChapter")
    public void toPublicationSetsPublicationContextWhenInputCrossrefDocumentIsBookChapter()
            throws InvalidIssnException, InvalidIsbnException, UnsupportedDocumentTypeException {
        assertThat(toPublication(sampleBookChapter())
                        .getEntityDescription().getReference().getPublicationContext().getClass(),
                is(equalTo(Chapter.class)));
    }

    @Test
    @DisplayName("toPublication set publicationInstance when input Crossref document is BookChapter")
    public void toPublicationSetsPublicationInstanceWhenInputCrossrefDocumentIsBookChapter()
            throws InvalidIssnException, InvalidIsbnException, UnsupportedDocumentTypeException {
        assertThat(toPublication(sampleBookChapter())
                        .getEntityDescription().getReference().getPublicationInstance().getClass(),
                is(equalTo(ChapterArticle.class)));
    }

    @Test
    @DisplayName("toPublication set pages when input Crossref document is BookChapter")
    public void toPublicationSetsPagesWhenInputCrossrefDocumentIsBookChapter()
            throws InvalidIssnException, InvalidIsbnException, UnsupportedDocumentTypeException {
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
    public void toPublicationAssignsRolesToEditors()
            throws InvalidIssnException, InvalidIsbnException, UnsupportedDocumentTypeException {

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
    @DisplayName("toPublication sets publicationInstance to BookAnthology when book has editors")
    public void toPublicationSetsPublicationInstanceToBookAnthologyWhenBookHasEditors()
            throws InvalidIssnException, InvalidIsbnException, UnsupportedDocumentTypeException {

        CrossRefDocument sampleBook = sampleBook();
        setEditors(sampleBook);

        var publicationInstance = toPublication(sampleBook)
                .getEntityDescription().getReference().getPublicationInstance();

        assertTrue(publicationInstance instanceof BookAnthology);

    }

    @Test
    @DisplayName("toPublication sets publicationInstance to BookMonograph when book has no editors")
    public void toPublicationSetsPublicationInstanceToBookMonographWhenBookHasNoEditors()
            throws InvalidIssnException, InvalidIsbnException, UnsupportedDocumentTypeException {

        CrossRefDocument sampleBook = sampleBook();
        sampleBook.setEditor(null);
        var publicationInstance = toPublication(sampleBook)
                .getEntityDescription().getReference().getPublicationInstance();

        assertTrue(publicationInstance instanceof BookMonograph);
    }

    @Test
    @DisplayName("toPublication assigns roles to all authors and editors")
    public void toPublicationAssignRolesToAuthorsAndEditors()
            throws InvalidIssnException, InvalidIsbnException, UnsupportedDocumentTypeException {

        CrossRefDocument sampleBook = sampleBook();
        setAuthorWithMultipleAffiliations(sampleBook);
        setEditors(sampleBook);
        assertTrue(toPublication(sampleBook).getEntityDescription().getContributors().size() > 2);
        assertTrue(toPublication(sampleBook).getEntityDescription().getContributors().stream().allMatch(this::hasRole));
    }

    boolean hasRole(Contributor contributor) {
        return nonNull(contributor.getRole());
    }

    private boolean isEditor(Contributor contributor) {
        return Role.EDITOR.equals(contributor.getRole());
    }

    private void assertRequiredValuesAreConverted(Publication actualPublication) {
        assertTrue(actualPublication.getPublisher().getLabels().containsValue(SAMPLE_PUBLISHER));
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

    private Publication toPublication(CrossRefDocument doc)
            throws InvalidIssnException, InvalidIsbnException, UnsupportedDocumentTypeException {
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
        crossRefDocument.setTitle(List.of(SAMPLE_DOCUMENT_TITLE));
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

    private void setPublicationTypeBookChapter(CrossRefDocument document) {
        document.setType(CrossrefType.BOOK_CHAPTER.getType());
    }

    private CrossRefDocument setAuthor(CrossRefDocument document) {
        CrossrefContributor author = new CrossrefContributor.Builder().withGivenName(AUTHOR_GIVEN_NAME)
                .withFamilyName(AUTHOR_FAMILY_NAME)
                .withSequence(FIRST_AUTHOR).build();
        CrossrefContributor secondAuthor = new CrossrefContributor.Builder().withGivenName(AUTHOR_GIVEN_NAME)
                .withFamilyName(AUTHOR_FAMILY_NAME)
                .withSequence(SECOND_AUTHOR).build();
        List<CrossrefContributor> authors = Arrays.asList(author, secondAuthor);
        document.setAuthor(authors);
        return document;
    }

    private CrossRefDocument setAuthorWithAffiliation(CrossRefDocument document) {
        CrossrefContributor author = new CrossrefContributor.Builder().withGivenName(AUTHOR_GIVEN_NAME)
                .withFamilyName(AUTHOR_FAMILY_NAME)
                .withSequence(FIRST_AUTHOR).build();
        CrossrefContributor secondAuthor = new CrossrefContributor.Builder().withGivenName(AUTHOR_GIVEN_NAME)
                .withFamilyName(AUTHOR_FAMILY_NAME)
                .withAffiliation(sampleAffiliation())
                .withSequence(SECOND_AUTHOR).build();
        List<CrossrefContributor> authors = Arrays.asList(author, secondAuthor);
        document.setAuthor(authors);
        return document;
    }

    private CrossRefDocument setAuthorWithMultipleAffiliations(CrossRefDocument document) {
        CrossrefContributor author = new CrossrefContributor.Builder().withGivenName(AUTHOR_GIVEN_NAME)
                .withFamilyName(AUTHOR_FAMILY_NAME)
                .withSequence(FIRST_AUTHOR).build();
        CrossrefContributor secondAuthor = new CrossrefContributor.Builder().withGivenName(AUTHOR_GIVEN_NAME)
                .withFamilyName(AUTHOR_FAMILY_NAME)
                .withAffiliation(sampleMultipleAffiliations())
                .withSequence(SECOND_AUTHOR).build();
        List<CrossrefContributor> authors = Arrays.asList(author, secondAuthor);
        document.setAuthor(authors);
        return document;
    }

    private CrossRefDocument setAuthorWithUnauthenticatedOrcid(CrossRefDocument document) {
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
        List<CrossrefContributor> authors = Arrays.asList(author, secondAuthor);
        document.setAuthor(authors);
        return document;
    }

    private List<CrossrefAffiliation> sampleAffiliation() {
        CrossrefAffiliation affiliation = new CrossrefAffiliation();
        affiliation.setName("affiliationName");
        List<CrossrefAffiliation> affiliations = List.of(affiliation);
        return affiliations;
    }

    private List<CrossrefAffiliation> sampleMultipleAffiliations() {
        CrossrefAffiliation firstAffiliation = new CrossrefAffiliation();
        firstAffiliation.setName("firstAffiliationName");
        CrossrefAffiliation secondAffiliation = new CrossrefAffiliation();
        secondAffiliation.setName("secondAffiliationName");
        List<CrossrefAffiliation> affiliations = List.of(firstAffiliation, secondAffiliation);
        return affiliations;
    }

    private CrossRefDocument setEditors(CrossRefDocument document) {
        CrossrefContributor editor = new CrossrefContributor.Builder().withGivenName(AUTHOR_GIVEN_NAME)
                .withFamilyName(AUTHOR_FAMILY_NAME)
                .withSequence(FIRST_AUTHOR).build();
        CrossrefContributor secondEditor = new CrossrefContributor.Builder().withGivenName(AUTHOR_GIVEN_NAME)
                .withFamilyName(AUTHOR_FAMILY_NAME)
                .withSequence(SECOND_AUTHOR).build();
        List<CrossrefContributor> editors = Arrays.asList(editor, secondEditor);
        document.setEditor(editors);
        return document;
    }



    private int startCountingFromOne(int i) {
        return i + 1;
    }
}
