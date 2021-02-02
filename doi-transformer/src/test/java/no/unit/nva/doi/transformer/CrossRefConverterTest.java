package no.unit.nva.doi.transformer;

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

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import no.unit.nva.doi.transformer.language.LanguageMapper;
import no.unit.nva.doi.transformer.language.exceptions.LanguageUriNotFoundException;
import no.unit.nva.doi.transformer.model.crossrefmodel.CrossRefDocument;
import no.unit.nva.doi.transformer.model.crossrefmodel.CrossrefAffiliation;
import no.unit.nva.doi.transformer.model.crossrefmodel.CrossrefApiResponse;
import no.unit.nva.doi.transformer.model.crossrefmodel.CrossrefAuthor;
import no.unit.nva.doi.transformer.model.crossrefmodel.CrossrefDate;
import no.unit.nva.doi.transformer.model.crossrefmodel.Issn;
import no.unit.nva.doi.transformer.model.crossrefmodel.Issn.IssnType;
import no.unit.nva.doi.transformer.utils.CrossrefType;
import no.unit.nva.model.Contributor;
import no.unit.nva.model.Identity;
import no.unit.nva.model.Organization;
import no.unit.nva.model.Publication;
import no.unit.nva.model.PublicationDate;
import no.unit.nva.model.contexttypes.Journal;
import no.unit.nva.model.exceptions.InvalidIssnException;
import no.unit.nva.model.instancetypes.journal.JournalArticle;
import no.unit.nva.model.pages.Pages;
import no.unit.nva.model.pages.Range;
import nva.commons.core.ioutils.IoUtils;
import nva.commons.core.JsonUtils;
import nva.commons.doi.DoiConverter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

public class CrossRefConverterTest extends ConversionTest {

    private static final Instant NOW = Instant.now();
    public static final String AUTHOR_GIVEN_NAME = "givenName";
    public static final String AUTHOR_FAMILY_NAME = "familyName";
    public static final String FIRST_AUTHOR = "first";
    private static final int DATE_SIZE = 3;
    private static final int NUMBER_OF_DATES = 2;
    public static final Integer EXPECTED_YEAR = 2019;
    public static final int UNEXPECTED_YEAR = EXPECTED_YEAR + 1;
    private static final String SAMPLE_DOCUMENT_TITLE = "Sample document title";
    private static final Integer NUMBER_OF_SAMPLE_AUTHORS = 2;
    public static final String SURNAME_COMMA_FIRSTNAME = "%s,.*%s";
    public static final String NOT_JOURNAL_ARTICLE = "book";
    private static final UUID DOC_ID = UUID.randomUUID();
    private static final String OWNER = "TheOwner";
    private static final String INVALID_ORDINAL = "invalid ordinal";
    public static final String SECOND_AUTHOR = "second";
    public static final String CROSSREF_WITH_ABSTRACT_JSON = "crossrefWithAbstract.json";
    private static final String PROCESSED_ABSTRACT = "processedAbstract.txt";
    private static final String SAMPLE_ORCID = "http://orcid.org/0000-1111-2222-3333";

    public static final String ENG_ISO_639_3 = "eng";
    public static final String SOME_DOI = "10.1000/182";
    public static final String VALID_ISSN_A = "0306-4379";
    public static final String VALID_ISSN_B = "1066-8888";
    public static final int EXPECTED_MONTH = 2;
    public static final int EXPECTED_DAY = 20;

    private CrossRefDocument sampleInputDocument = createSampleDocument();
    private final CrossRefConverter converter = new CrossRefConverter();
    private Publication samplePublication;
    private static final ObjectMapper objectMapper = JsonUtils.objectMapper;

    @BeforeEach
    public void init() throws InvalidIssnException {
        sampleInputDocument = createSampleDocument();
        samplePublication = toPublication(sampleInputDocument);
    }

    @Test
    @DisplayName("An empty CrossRef document throws IllegalArgument exception")
    public void anEmptyCrossRefDocumentThrowsIllegalArgumentException() throws IllegalArgumentException {
        CrossRefDocument doc = new CrossRefDocument();
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> toPublication(doc));
        assertThat(exception.getMessage(), is(CrossRefConverter.INVALID_ENTRY_ERROR));
    }

    @Test
    @DisplayName("The creator's name in the publication contains first family and then given name")
    public void creatorsNameContainsFirstFamilyAndThenGivenName() throws IllegalArgumentException {

        List<Contributor> contributors = samplePublication.getEntityDescription().getContributors();

        assertThat(contributors.size(), is(equalTo(sampleInputDocument.getAuthor().size())));
        assertThat(contributors.size(), is(equalTo(NUMBER_OF_SAMPLE_AUTHORS)));

        String actualName = contributors.get(NUMBER_OF_SAMPLE_AUTHORS - 1).getIdentity().getName();
        String givenName = sampleInputDocument.getAuthor().get(NUMBER_OF_SAMPLE_AUTHORS - 1).getGivenName();
        assertThat(actualName, containsString(givenName));

        String familyName = sampleInputDocument.getAuthor().get(NUMBER_OF_SAMPLE_AUTHORS - 1).getFamilyName();
        assertThat(actualName, containsString(familyName));

        String expectedNameRegEx = String.format(SURNAME_COMMA_FIRSTNAME, familyName, givenName);
        assertThat(actualName, matchesPattern(expectedNameRegEx));
    }

    @Test
    @DisplayName("The earliest year found in the \"published-print\" field is stored in the entity description.")
    public void entityDescriptionContainsTheEarliestYearFoundInPublishedPrintField() {
        String actualYear = samplePublication.getEntityDescription().getDate().getYear();
        assertThat(actualYear, is(equalTo(EXPECTED_YEAR.toString())));
    }

    @Test
    @DisplayName("toPublication sets null EntityDescription date when input has no \"issued\" date")
    public void entityDescriptionDateIsNullWhenInputDataHasNoPublicationDate() throws InvalidIssnException {
        sampleInputDocument.setIssued(null);
        Publication publicationWithoutDate = toPublication(sampleInputDocument);
        PublicationDate actualDate = publicationWithoutDate.getEntityDescription().getDate();
        assertThat(actualDate, is(nullValue()));
    }

    @Test
    @DisplayName("toPublication sets PublicationContext to Journal when the input has the tag \"journal-article\"")
    public void toPublicationSetsPublicationContextToJournalWhenTheInputHasTheTagJournalArticle() {
        assertThat(samplePublication.getEntityDescription().getReference().getPublicationContext().getClass(),
            is(equalTo(Journal.class)));
    }

    @Test
    @DisplayName("toPublication throws Exception when the input does not have the tag \"journal-article\"")
    public void toPublicationSetsThrowsExceptionWhenTheInputDoesNotHaveTheTagJournalArticle() {
        sampleInputDocument.setType(NOT_JOURNAL_ARTICLE);
        String expectedError = String.format(CrossRefConverter.UNRECOGNIZED_TYPE_MESSAGE, NOT_JOURNAL_ARTICLE);
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
            () -> toPublication(sampleInputDocument));
        assertThat(exception.getMessage(), is(equalTo(expectedError)));
    }

    @Test
    @DisplayName("toPublication sets as sequence the position of the author in the list when ordinal is not numerical")
    public void toPublicationSetsOrdinalAsSecondAuthorIfInputOrdinalIsNotAValidOrdinal() throws InvalidIssnException {
        int numberOfAuthors = sampleInputDocument.getAuthor().size();
        sampleInputDocument.getAuthor().forEach(a -> {
            a.setSequence(INVALID_ORDINAL);
        });
        Publication publication = toPublication(sampleInputDocument);
        List<Integer> ordinals = publication.getEntityDescription().getContributors().stream()
            .map(Contributor::getSequence).collect(Collectors.toList());
        assertThat(ordinals.size(), is(numberOfAuthors));
        List<Integer> expectedValues = IntStream.range(0, numberOfAuthors).map(this::startCountingFromOne).boxed()
            .collect(Collectors.toList());
        assertThat(ordinals, contains(expectedValues.toArray()));
    }

    @Test
    @DisplayName("toPublication sets the correct number when the sequence ordinal is valid")
    public void toPublicationSetsCorrectNumberForValidOrdinal() throws InvalidIssnException {
        CrossrefAuthor author = sampleInputDocument.getAuthor().stream().findFirst().get();
        String validOrdinal = "second";
        int expected = 2;
        author.setSequence(validOrdinal);

        int actual = toPublication(sampleInputDocument).getEntityDescription().getContributors().stream().findFirst()
            .get().getSequence();
        assertThat(actual, is(equalTo(expected)));
    }

    @Test
    @DisplayName("toPublication sets abstract when input has non empty abstract")
    public void toPublicationSetsAbstractWhenInputHasNonEmptyAbstract() throws IOException, InvalidIssnException {
        String json = IoUtils.stringFromResources(Path.of(CROSSREF_WITH_ABSTRACT_JSON));
        CrossRefDocument docWithAbstract = objectMapper.readValue(json, CrossrefApiResponse.class).getMessage();
        String abstractText = toPublication(docWithAbstract).getEntityDescription().getAbstract();
        assertThat(abstractText, is(not(emptyString())));
        String expectedAbstract = IoUtils.stringFromResources(Path.of(PROCESSED_ABSTRACT));
        assertThat(abstractText, is(equalTo(expectedAbstract)));
    }

    @Test
    @DisplayName("toPublication sets the language to a URI when the input is an ISO639-3 entry")
    public void toPublicationSetsTheLanguageToAUriWhenTheInputFollowsTheIso3Standard()
        throws LanguageUriNotFoundException, InvalidIssnException {
        Locale sampleLanguage = Locale.ENGLISH;
        sampleInputDocument.setLanguage(sampleLanguage.getISO3Language());
        URI actualLanguage = toPublication(sampleInputDocument).getEntityDescription().getLanguage();
        URI expectedLanguage = LanguageMapper.getUriFromIso(ENG_ISO_639_3);
        assertThat(actualLanguage, is(equalTo(expectedLanguage)));
        assertThat(actualLanguage, is(notNullValue()));
    }

    @Test
    @DisplayName("toPublication sets the doi of the Reference when the Crossref document has a \"DOI\" value ")
    public void toPublicationSetsTheDoiOfTheReferenceWhenTheCrossrefDocHasADoiValue() throws InvalidIssnException {
        DoiConverter doiConverter = new DoiConverter();
        sampleInputDocument.setDoi(SOME_DOI);
        URI actualDoi = toPublication(sampleInputDocument).getEntityDescription().getReference().getDoi();
        assertThat(actualDoi, is(equalTo(doiConverter.toUri(SOME_DOI))));
    }

    @Test
    @DisplayName("toPublication sets the doi of the Reference when the Crossref document has at least one"
        + " \"Container\" value ")
    public void toPublicationSetsTheNameOfTheReferenceWhenTheCrossrefDocHasAtLeatOneContainterTitle()
            throws InvalidIssnException {
        String firstNameOfJournal = "Journal 1st Name";
        String secondNameOfJournal = "Journal 2nd Name";
        sampleInputDocument.setContainerTitle(Arrays.asList(firstNameOfJournal, secondNameOfJournal));

        String actualJournalName = ((Journal)toPublication(sampleInputDocument).getEntityDescription().getReference()
            .getPublicationContext()).getTitle();
        assertThat(actualJournalName, is(equalTo(firstNameOfJournal)));
    }

    @Test
    @DisplayName("toPublication sets the volume of the Reference when the Crosref document has a \"Volume\" value")
    public void toPublicationSetsTheVolumeOfTheReferenceWhentheCrossrefDocHasAVolume()
        throws InvalidIssnException {
        String expectedVolume = "Vol. 1";
        sampleInputDocument.setVolume(expectedVolume);
        String actualVolume = ((JournalArticle) (toPublication(sampleInputDocument)
            .getEntityDescription().getReference()
            .getPublicationInstance()))
            .getVolume();
        assertThat(actualVolume, is(equalTo(expectedVolume)));
    }

    @Test
    @DisplayName("toPublication sets the pages of the Reference when the Crosref document has a \"Pages\" value")
    public void toPublicationSetsThePagesOfTheReferenceWhentheCrossrefDocHasPages()
        throws InvalidIssnException {
        String pages = "45-89";

        sampleInputDocument.setPage(pages);
        Pages actualPages = toPublication(sampleInputDocument).getEntityDescription().getReference()
            .getPublicationInstance()
            .getPages();
        Pages expectedPages = new Range.Builder().withBegin("45").withEnd("89").build();
        assertThat(actualPages, is(equalTo(expectedPages)));
    }

    @Test
    @DisplayName("toPublication sets the issue of the Reference when the Crosref document has a \"Issue\" value")
    public void toPublicationSetsTheIssueOfTheReferenceWhentheCrossrefDocHasAnIssueValue()
        throws InvalidIssnException {
        String expectedIssue = "SomeIssue";

        sampleInputDocument.setIssue(expectedIssue);
        String actualIssue = ((JournalArticle) (toPublication(sampleInputDocument)
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
        throws InvalidIssnException {
        String source = "Crossref";
        URI expectedURI = CrossRefConverter.CROSSEF_URI;

        sampleInputDocument.setSource(source);
        URI actualSource = toPublication(sampleInputDocument).getEntityDescription().getMetadataSource();

        assertThat(actualSource, is(equalTo(expectedURI)));
    }

    @Test
    @DisplayName("toPublication sets the MetadataSource to the specfied URL when the Crossref "
        + "document has as \"source\" a valid URL")
    public void toPublicationSetsTheMetadataSourceToTheSourceUrlIfTheDocHasAsSourceAValidUrl()
        throws InvalidIssnException {
        String source = "http://www.something.com";
        URI expectedURI = URI.create(source);

        sampleInputDocument.setSource(source);
        URI actualSource = toPublication(sampleInputDocument).getEntityDescription().getMetadataSource();

        assertThat(actualSource, is(equalTo(expectedURI)));
    }

    @Test
    @DisplayName("toPublication sets null for online ISSN when only print ISSN is available")
    public void toPublicationSetsNullOnlineIssnWhenOnlyPrintISSnIsAvailable() throws InvalidIssnException {
        IssnType type = IssnType.PRINT;
        Issn printIssn = sampleIssn(type,VALID_ISSN_A);
        sampleInputDocument.setIssnType(Collections.singletonList(printIssn));

        Publication actualDocument = toPublication(sampleInputDocument);
        Journal actualPublicationContext = (Journal) actualDocument.getEntityDescription()
            .getReference()
            .getPublicationContext();
        String onlineIssn = actualPublicationContext.getOnlineIssn();
        assertThat(onlineIssn, is(equalTo(null)));
    }

    @Test
    @DisplayName("toPublication sets null for print ISSN when only online ISSN is available")
    public void toPublicationSetsNullPrintIssnWhenOnlyOnlineISSnIsAvailable() throws InvalidIssnException {
        IssnType type = IssnType.ELECTRONIC;
        Issn onlineIssn = sampleIssn(type,VALID_ISSN_A);
        sampleInputDocument.setIssnType(Collections.singletonList(onlineIssn));

        Publication actualDocument = toPublication(sampleInputDocument);
        Journal actualPublicationContext = (Journal) actualDocument.getEntityDescription()
            .getReference()
            .getPublicationContext();
        String actualPrintIssn = actualPublicationContext.getPrintIssn();
        assertThat(actualPrintIssn, is(equalTo(null)));
    }

    @Test
    @DisplayName("toPublication sets one of many online ISSNs when multiple online ISSNs are  available")
    public void toPublicationSetsOneOfManyOnlineIssnsWhenMultipleOfTheSameTypeAreAvailable()
        throws InvalidIssnException {
        IssnType type = IssnType.ELECTRONIC;
        Issn onlineIssnA = sampleIssn(type, VALID_ISSN_A);
        Issn onlineIssnB = sampleIssn(type, VALID_ISSN_B);
        List<Issn> issns = Arrays.asList(onlineIssnA, onlineIssnB);
        sampleInputDocument.setIssnType(issns);

        Publication actualDocument = toPublication(sampleInputDocument);
        Journal actualPublicationContext = (Journal) actualDocument.getEntityDescription()
            .getReference()
            .getPublicationContext();
        String actualOnlineIssn = actualPublicationContext.getOnlineIssn();

        List<String> poolOfExpectedValues = issns.stream().map(Issn::getValue).collect(Collectors.toList());
        assertThat(poolOfExpectedValues, hasItem(actualOnlineIssn));
    }

    @Test
    @DisplayName("toPublication sets one of many print ISSNs when multiple print ISSNs are available")
    public void toPublicationSetsOneOfManyPrintIssnsWhenMultipleOfTheSameTypeAreAvailable()
        throws InvalidIssnException {
        IssnType type = IssnType.PRINT;
        Issn printIssnA = sampleIssn(type, VALID_ISSN_A);
        Issn printIssnB = sampleIssn(type, VALID_ISSN_B);
        List<Issn> issns = Arrays.asList(printIssnA, printIssnB);
        sampleInputDocument.setIssnType(issns);

        Publication actualDocument = toPublication(sampleInputDocument);
        Journal actualPublicationContext = (Journal) actualDocument.getEntityDescription()
            .getReference()
            .getPublicationContext();
        String actualPrintIssn = actualPublicationContext.getPrintIssn();

        List<String> poolOfExpectedValues = issns.stream().map(Issn::getValue).collect(Collectors.toList());
        assertThat(poolOfExpectedValues, hasItem(actualPrintIssn));
    }

    @Test
    @DisplayName("toPublication sets tags when subject are available")
    public void toPublicationSetsTagsWhenSubjectAreAvailable() throws InvalidIssnException {
        final String expectedTag = "subject1";
        List<String> subject = List.of(expectedTag);
        sampleInputDocument.setSubject(subject);
        Publication actualDocument = toPublication(sampleInputDocument);
        List<String> actualTags = actualDocument.getEntityDescription().getTags();

        assertThat(actualTags, hasItem(expectedTag));
    }

    @Test
    @DisplayName("toPublication sets tags to empty list when subject are not available")
    public void toPublicationSetsTagsToEmptyListWhenSubjectAreNotAvailable() throws InvalidIssnException {
        sampleInputDocument.setSubject(null);
        Publication actualDocument = toPublication(sampleInputDocument);
        List<String> actualTags = actualDocument.getEntityDescription().getTags();

        assertTrue(actualTags.isEmpty());
    }

    @Test
    @DisplayName("toPublication preserves all date parts when they are available")
    public void toPublicationPreservesAllDatepartsWhenTheyAreAvailable() throws InvalidIssnException {
        int[][] dateParts = new int[1][DATE_SIZE];
        dateParts[0] = new int[]{EXPECTED_YEAR, EXPECTED_MONTH, EXPECTED_DAY};
        CrossrefDate crossrefDate = new CrossrefDate();
        crossrefDate.setDateParts(dateParts);

        sampleInputDocument.setIssued(crossrefDate);
        Publication actualDocument = toPublication(sampleInputDocument);
        PublicationDate publicationDate = actualDocument.getEntityDescription().getDate();

        assertEquals("" + EXPECTED_YEAR, publicationDate.getYear());
        assertEquals("" + EXPECTED_MONTH, publicationDate.getMonth());
        assertEquals("" + EXPECTED_DAY, publicationDate.getDay());

    }

    @Test
    @DisplayName("toPublication sets affiliation labels when author.affiliation.name is available")
    public void toPublicationSetsAffiliationLabelWhenAuthorHasAffiliationName() throws InvalidIssnException {
        setAuthorWithAffiliation(sampleInputDocument);
        Publication actualDocument = toPublication(sampleInputDocument);

        List<Contributor> contributors = actualDocument.getEntityDescription().getContributors();
        List<List<Organization>> organisations =  contributors.stream()
                .map(Contributor::getAffiliations)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        assertFalse(organisations.isEmpty());
    }

    @Test
    @DisplayName("toPublication sets multiple affiliation labels when author has more affiliations name")
    public void toPublicationSetsMultipleAffiliationLabelWhenAuthorHasMultipleAffiliationName()
            throws InvalidIssnException {
        setAuthorWithMultipleAffiliations(sampleInputDocument);
        Publication actualDocument = toPublication(sampleInputDocument);

        List<Contributor> contributors = actualDocument.getEntityDescription().getContributors();
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
    public void toPublicationSetsAffiliationToEmptyListWhenAuthorHasNoAffiliatio() throws InvalidIssnException {
        Publication actualDocument = toPublication(sampleInputDocument);

        List<Contributor> contributors = actualDocument.getEntityDescription().getContributors();
        List<List<Organization>> organisations =  contributors.stream()
                .map(Contributor::getAffiliations)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        assertTrue(organisations.isEmpty());
    }

    @Test
    @DisplayName("toPublication preserves Orcid when Author has orcid")
    public void toPublicationPreservesOrcidWhenAuthorHasOrcid() throws InvalidIssnException {
        setAuthorWithUnauthenticatedOrcid(sampleInputDocument);
        Publication actualDocument = toPublication(sampleInputDocument);
        var orcids = actualDocument.getEntityDescription().getContributors().stream()
                .map(Contributor::getIdentity)
                .map(Identity::getOrcId)
                .collect(Collectors.toList());
        assertThat(orcids, hasItem(SAMPLE_ORCID));
    }


    private Issn sampleIssn(IssnType type, String value) {
        Issn issn = new Issn();
        issn.setType(type.getName());
        issn.setValue(value);
        return issn;
    }

    private Publication toPublication(CrossRefDocument doc) throws InvalidIssnException {
        return converter.toPublication(doc, NOW, OWNER, DOC_ID, SOME_PUBLISHER_URI);
    }

    private CrossRefDocument createSampleDocument() {
        CrossRefDocument document = new CrossRefDocument();
        setAuthor(document);
        setPublicationDate(document);
        setIssuedDate(document);    // Issued is required from CrossRef, is either printed-date or
        setTitle(document);
        setPublicationType(document);
        return document;
    }

    private void setPublicationType(CrossRefDocument document) {
        document.setType(CrossrefType.JOURNAL_ARTICLE.getType());
    }

    private void setTitle(CrossRefDocument document) {
        List<String> titleArray = Collections.singletonList(SAMPLE_DOCUMENT_TITLE);
        document.setTitle(titleArray);
    }

    private void setPublicationDate(CrossRefDocument document) {
        int[][] dateParts = new int[NUMBER_OF_DATES][DATE_SIZE];
        dateParts[0] = new int[]{EXPECTED_YEAR, 2, 20};
        dateParts[1] = new int[]{UNEXPECTED_YEAR};
        CrossrefDate date = new CrossrefDate();
        date.setDateParts(dateParts);
        document.setPublishedPrint(date);
    }

    private void setIssuedDate(CrossRefDocument document) {
        int[][] dateParts = new int[1][DATE_SIZE];
        dateParts[0] = new int[]{EXPECTED_YEAR, 2, 20};
        CrossrefDate date = new CrossrefDate();
        date.setDateParts(dateParts);
        document.setIssued(date);
    }


    private CrossRefDocument setAuthor(CrossRefDocument document) {
        CrossrefAuthor author = new CrossrefAuthor.Builder().withGivenName(AUTHOR_GIVEN_NAME)
            .withFamilyName(AUTHOR_FAMILY_NAME)
            .withSequence(FIRST_AUTHOR).build();
        CrossrefAuthor secondAuthor = new CrossrefAuthor.Builder().withGivenName(AUTHOR_GIVEN_NAME)
            .withFamilyName(AUTHOR_FAMILY_NAME)
            .withSequence(SECOND_AUTHOR).build();
        List<CrossrefAuthor> authors = Arrays.asList(author, secondAuthor);
        document.setAuthor(authors);
        return document;
    }

    private CrossRefDocument setAuthorWithAffiliation(CrossRefDocument document) {
        CrossrefAuthor author = new CrossrefAuthor.Builder().withGivenName(AUTHOR_GIVEN_NAME)
                .withFamilyName(AUTHOR_FAMILY_NAME)
                .withSequence(FIRST_AUTHOR).build();
        CrossrefAuthor secondAuthor = new CrossrefAuthor.Builder().withGivenName(AUTHOR_GIVEN_NAME)
                .withFamilyName(AUTHOR_FAMILY_NAME)
                .withAffiliation(createCrossRefAffiliation())
                .withSequence(SECOND_AUTHOR).build();
        List<CrossrefAuthor> authors = Arrays.asList(author, secondAuthor);
        document.setAuthor(authors);
        return document;
    }

    private CrossRefDocument setAuthorWithMultipleAffiliations(CrossRefDocument document) {
        CrossrefAuthor author = new CrossrefAuthor.Builder().withGivenName(AUTHOR_GIVEN_NAME)
                .withFamilyName(AUTHOR_FAMILY_NAME)
                .withSequence(FIRST_AUTHOR).build();
        CrossrefAuthor secondAuthor = new CrossrefAuthor.Builder().withGivenName(AUTHOR_GIVEN_NAME)
                .withFamilyName(AUTHOR_FAMILY_NAME)
                .withAffiliation(createCrossRefMultipleAffiliations())
                .withSequence(SECOND_AUTHOR).build();
        List<CrossrefAuthor> authors = Arrays.asList(author, secondAuthor);
        document.setAuthor(authors);
        return document;
    }

    private CrossRefDocument setAuthorWithUnauthenticatedOrcid(CrossRefDocument document) {
        CrossrefAuthor author = new CrossrefAuthor.Builder()
                .withGivenName(AUTHOR_GIVEN_NAME)
                .withFamilyName(AUTHOR_FAMILY_NAME)
                .withSequence(FIRST_AUTHOR)
                .withOrcid(SAMPLE_ORCID)
                .withAuthenticatedOrcid(false)
                .build();
        CrossrefAuthor secondAuthor = new CrossrefAuthor.Builder().withGivenName(AUTHOR_GIVEN_NAME)
                .withFamilyName(AUTHOR_FAMILY_NAME)
                .withSequence(SECOND_AUTHOR).build();
        List<CrossrefAuthor> authors = Arrays.asList(author, secondAuthor);
        document.setAuthor(authors);
        return document;
    }


    private List<CrossrefAffiliation> createCrossRefAffiliation() {
        CrossrefAffiliation affiliation = new CrossrefAffiliation();
        affiliation.setName("affiliationName");
        List<CrossrefAffiliation> affiliations = List.of(affiliation);
        return affiliations;
    }

    private List<CrossrefAffiliation> createCrossRefMultipleAffiliations() {
        CrossrefAffiliation firstAffiliation = new CrossrefAffiliation();
        firstAffiliation.setName("firstAffiliationName");
        CrossrefAffiliation secondAffiliation = new CrossrefAffiliation();
        secondAffiliation.setName("secondAffiliationName");
        List<CrossrefAffiliation> affiliations = List.of(firstAffiliation, secondAffiliation);
        return affiliations;
    }


    private int startCountingFromOne(int i) {
        return i + 1;
    }
}
