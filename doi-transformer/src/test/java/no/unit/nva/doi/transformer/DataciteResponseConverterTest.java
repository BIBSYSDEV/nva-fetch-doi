package no.unit.nva.doi.transformer;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsIterableContainingInOrder.contains;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.hamcrest.core.IsNot.not;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.hamcrest.core.StringContains.containsString;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.core.JsonProcessingException;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import no.sikt.nva.doi.fetch.jsonconfig.Json;
import no.unit.nva.commons.json.JsonUtils;
import no.unit.nva.doi.fetch.commons.publication.model.CreatePublicationRequest;
import no.unit.nva.doi.transformer.model.datacitemodel.DataciteAffiliation;
import no.unit.nva.doi.transformer.model.datacitemodel.DataciteCreator;
import no.unit.nva.doi.transformer.model.datacitemodel.DataciteResponse;
import no.unit.nva.doi.transformer.model.datacitemodel.DataciteRights;
import no.unit.nva.doi.transformer.model.datacitemodel.DataciteTitle;
import no.unit.nva.doi.transformer.utils.InvalidIssnException;
import nva.commons.core.ioutils.IoUtils;
import nva.commons.doi.DoiConverter;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

public class DataciteResponseConverterTest {

  public static final String ENTRY_WITH_ALTERNATIVE_TITLE = "datacite_many_titles.json";
  public static final Path SAMPLE_DATACITE_RESPOSNE = Path.of("datacite_response.json");
  private static final String UNDETERMINED_LANGUAGE = AbstractConverter.UNDETERMINED_LANGUAGE;
  private static final String LEXVO_URI_PREFIX = "http://lexvo.org";
  private static final String AMERICAN_ENGLISH_TAG = "en-US";
  private static final String ENGLISH = "en";
  private static final String ENGLISH_ISO_639_3 = "eng";
  private static final String NORWEGIAN_BOKMAL = "nb";
  private static final String NORWEGIAN_BOKMAL_ISO_639_3 = "nob";
  private static final String GERMAN = "de";
  private static final String GERMAN_TITLE = "Ein alternativer Titel";

  @Test
  void defaultConstructorExists() {
    new DataciteResponseConverter();
  }

  @Test
  void shouldHandlePublishersThatAreObjects() throws JsonProcessingException {
    var expected = "My publisher";
    var data =
        IoUtils.stringFromResources(Path.of("datacite_publisher_is_object.json"))
            .formatted(expected);

    var dataciteResponse = JsonUtils.dtoObjectMapper.readValue(data, DataciteResponse.class);
    assertThat(dataciteResponse.getPublisher(), equalTo(expected));
  }

  @DisplayName("DataciteResponseConverter::toPublication returns valid JSON when input is valid")
  @Test
  public void toPublicationReturnsValidJsonWhenInputIsValid()
      throws IOException, InvalidIssnException {
    DataciteResponse dataciteResponse = sampleDataciteResponse();
    CreatePublicationRequest publication = toPublication(dataciteResponse);
    String json = Json.writeValueAsString(publication);
    assertNotNull(json);
  }

  @Test
  @DisplayName(
      "Publication maps the datacite title text to the alternative title value when the datacite"
          + " document has many titles")
  public void publicationMapsDataciteTitleTextToAlternativeTitleValue()
      throws IOException, InvalidIssnException {
    var response = responseWithMultipleTitles();
    var expectedAlternativeTitle = response.getTitles().get(1).getTitle();

    var alternativeTitles = toPublication(response).getEntityDescription().getAlternativeTitles();

    assertThat(alternativeTitles.values(), contains(expectedAlternativeTitle));
  }

  @Test
  public void publicationMapsThreeLetterLanguageCodesToTwoLetterKeys()
      throws IOException, InvalidIssnException {
    var response = responseWithMultipleTitles();
    var titles = new ArrayList<>(response.getTitles());
    titles.get(1).setLang(ENGLISH_ISO_639_3);
    titles.add(titleWithLanguage(GERMAN_TITLE, NORWEGIAN_BOKMAL_ISO_639_3));
    response.setTitles(titles);

    var alternativeTitles = toPublication(response).getEntityDescription().getAlternativeTitles();

    assertThat(alternativeTitles.size(), is(equalTo(2)));
    assertThat(alternativeTitles.get(ENGLISH), is(notNullValue()));
    assertThat(alternativeTitles.get(NORWEGIAN_BOKMAL), is(equalTo(GERMAN_TITLE)));
  }

  @Test
  @DisplayName(
      "Publication does not contain the main title in the alternative titles when the datacite"
          + " document has many titles")
  public void publicationDoesNotContainMainTitleInAlternativeTItleWhenDataciteDocHasManyTitles()
      throws IOException, URISyntaxException, InvalidIssnException {
    var publication = readPublicationWithMultipleTitles();
    var mainTitle = publication.getEntityDescription().getMainTitle();
    var altTitles = publication.getEntityDescription().getAlternativeTitles().values();
    assertFalse(altTitles.contains(mainTitle));
  }

  @Test
  @DisplayName(
      "Publication keys alternative titles by undetermined language when the datacite document"
          + " does not state a language per title")
  public void publicationKeysAlternativeTitlesByUndeterminedLanguageWhenDataciteHasNoLang()
      throws IOException, URISyntaxException, InvalidIssnException {
    var publication = readPublicationWithMultipleTitles();
    var alternativeTitles = publication.getEntityDescription().getAlternativeTitles();
    assertThat(alternativeTitles.keySet(), contains(UNDETERMINED_LANGUAGE));
  }

  @Test
  public void publicationDoesNotPutLanguageUriInAlternativeTitleValue()
      throws IOException, URISyntaxException, InvalidIssnException {
    var publication = readPublicationWithMultipleTitles();
    var titles = publication.getEntityDescription().getAlternativeTitles().values();
    titles.forEach(title -> assertThat(title, not(containsString(LEXVO_URI_PREFIX))));
  }

  @Test
  public void publicationKeepsEveryAlternativeTitleThatStatesItsOwnLanguage()
      throws IOException, InvalidIssnException {
    var response = responseWithMultipleTitles();
    var titles = new ArrayList<>(response.getTitles());
    var firstAlternativeTitle = titles.get(1).getTitle();
    titles.get(1).setLang(AMERICAN_ENGLISH_TAG);
    titles.add(titleWithLanguage(GERMAN_TITLE, GERMAN));
    response.setTitles(titles);

    var alternativeTitles = toPublication(response).getEntityDescription().getAlternativeTitles();

    assertThat(alternativeTitles.size(), is(equalTo(2)));
    assertThat(alternativeTitles.get(ENGLISH), is(equalTo(firstAlternativeTitle)));
    assertThat(alternativeTitles.get(GERMAN), is(equalTo(GERMAN_TITLE)));
  }

  @Test
  public void publicationReducesRegionalLanguageTagToPrimarySubtag()
      throws IOException, InvalidIssnException {
    var response = responseWithMultipleTitles();
    response.getTitles().get(1).setLang(AMERICAN_ENGLISH_TAG);

    var alternativeTitles = toPublication(response).getEntityDescription().getAlternativeTitles();

    assertThat(alternativeTitles.keySet(), contains(ENGLISH));
  }

  @Test
  public void publicationKeepsFirstAlternativeTitleWhenSeveralShareTheSameLanguage()
      throws IOException, InvalidIssnException {
    var response = responseWithMultipleTitles();
    var titles = new ArrayList<>(response.getTitles());
    var firstAlternativeTitle = titles.get(1).getTitle();
    titles.add(titleWithLanguage(GERMAN_TITLE, null));
    response.setTitles(titles);

    var alternativeTitles = toPublication(response).getEntityDescription().getAlternativeTitles();

    assertThat(alternativeTitles.size(), is(equalTo(1)));
    assertThat(alternativeTitles.get(UNDETERMINED_LANGUAGE), is(equalTo(firstAlternativeTitle)));
  }

  private DataciteResponse responseWithMultipleTitles() throws IOException {
    var input = IoUtils.stringFromResources(Path.of(ENTRY_WITH_ALTERNATIVE_TITLE));
    return Json.readValue(input, DataciteResponse.class);
  }

  private DataciteTitle titleWithLanguage(String title, String lang) {
    var dataciteTitle = new DataciteTitle();
    dataciteTitle.setTitle(title);
    dataciteTitle.setLang(lang);
    return dataciteTitle;
  }

  @Test
  public void toPublicationReturnsNvaPublicationWithoutContributorsThatDoNotHaveFirstOrLastName()
      throws IOException, InvalidIssnException, URISyntaxException {
    int expectedNumberOfContributors = sampleDataciteResponse().getContributors().size();
    DataciteResponse dataciteSample = sampleDataciteResponse();
    List<DataciteCreator> newContributors = new ArrayList<>(dataciteSample.getCreators());
    newContributors.add(getDataciteCreator());
    dataciteSample.setCreators(newContributors);
    CreatePublicationRequest publication = toPublication(dataciteSample);
    int actualNumberOfContributors = publication.getEntityDescription().getContributors().size();
    assertThat(actualNumberOfContributors, is(equalTo(expectedNumberOfContributors)));
  }

  @Test
  public void hasOpenAccessRightsReturnsTrueOnCreativeCommons() {
    DataciteResponseConverter converter = new DataciteResponseConverter();
    DataciteRights dataciteRights = new DataciteRights();
    dataciteRights.setRightsUri("creativecommons");
    boolean hasOpenAccessRights = converter.hasOpenAccessRights(dataciteRights);

    assertTrue(hasOpenAccessRights);
  }

  private CreatePublicationRequest toPublication(DataciteResponse dataciteResponse)
      throws InvalidIssnException {
    DataciteResponseConverter converter =
        new DataciteResponseConverter(new DoiConverter(uri -> true));
    return converter.toPublication(dataciteResponse);
  }

  private DataciteResponse sampleDataciteResponse() throws IOException {
    return Json.readValue(
        IoUtils.stringFromResources(SAMPLE_DATACITE_RESPOSNE), DataciteResponse.class);
  }

  private CreatePublicationRequest readPublicationWithMultipleTitles()
      throws IOException, URISyntaxException, InvalidIssnException {
    String input = IoUtils.stringFromResources(Path.of(ENTRY_WITH_ALTERNATIVE_TITLE));
    DataciteResponseConverter converter =
        new DataciteResponseConverter(new DoiConverter(uri -> true));
    DataciteResponse response = Json.readValue(input, DataciteResponse.class);
    return converter.toPublication(response);
  }

  private DataciteCreator getDataciteCreator() {
    DataciteCreator anonymousContributor = new DataciteCreator();
    anonymousContributor.setAffiliation(List.of(getDataciteAffiliation()));
    anonymousContributor.setNameType("ORGANIZATIONAL");
    return anonymousContributor;
  }

  private DataciteAffiliation getDataciteAffiliation() {
    DataciteAffiliation affiliation = new DataciteAffiliation();
    affiliation.setAffiliation("someAffiliation");
    return affiliation;
  }
}
