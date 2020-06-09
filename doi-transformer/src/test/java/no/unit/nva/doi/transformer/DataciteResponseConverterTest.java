package no.unit.nva.doi.transformer;

import static java.time.Instant.now;
import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import no.unit.nva.doi.transformer.language.LanguageMapper;
import no.unit.nva.doi.transformer.model.internal.external.DataciteCreator;
import no.unit.nva.doi.transformer.model.internal.external.DataciteResponse;
import no.unit.nva.model.Publication;
import no.unit.nva.model.exceptions.InvalidIssnException;
import no.unit.nva.model.exceptions.InvalidPageTypeException;
import nva.commons.utils.IoUtils;
import org.junit.Assert;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

public class DataciteResponseConverterTest {

    public static final String ENTRY_WITH_ALTERNATIVE_TITLE = "datacite_many_titles.json";
    private static final UUID SOME_ID = UUID.randomUUID();
    private static final String SOME_OWNER = "SomeOwner";
    private static final URI SOME_URI = URI.create("SomeUri");
    private final ObjectMapper objectMapper = MainHandler.createObjectMapper();

    @DisplayName("DataciteResponseConverter::toPublication returns valid JSON when input is valid")
    @Test
    public void toPublicationReturnsValidJsonWhenInputIsValid() throws IOException, URISyntaxException,
            InvalidPageTypeException, InvalidIssnException {

        DataciteResponse dataciteResponse = objectMapper.readValue(
            new File("src/test/resources/datacite_response.json"), DataciteResponse.class);

        DataciteResponseConverter converter = new DataciteResponseConverter();
        Publication publication = converter.toPublication(dataciteResponse, now(), UUID.randomUUID(), "junit",
            URI.create("http://example.org/123"));
        String json = objectMapper.writeValueAsString(publication);
        Assert.assertNotNull(json);
    }

    @DisplayName("DataciteResponseConverter::toName creates valid, inverted name when input is valid")
    @Test
    public void toNameReturnsInvertedNameStringWhenInputIsValid() {
        DataciteCreator dataciteCreator = new DataciteCreator();
        dataciteCreator.setFamilyName("Family");
        dataciteCreator.setGivenName("Given");
        DataciteResponseConverter converter = new DataciteResponseConverter();

        String name = converter.toName(dataciteCreator);

        Assert.assertEquals("Family, Given", name);
    }

    @Test
    @DisplayName("Publication contains alternativeTitles with non null langauge tags when datacite document has "
        + "many titles")
    public void publicationContainsAlternativeTitlesWithNonNullLanguageTagsWhenDatataciteDocumentHasManyTitles()
            throws IOException, URISyntaxException, InvalidPageTypeException, InvalidIssnException {
        Publication publication = readPublicationWithMutlipleTitles();
        Map<String, String> alternativeTitles = publication.getEntityDescription().getAlternativeTitles();
        Collection<String> languageTags = alternativeTitles.values();
        languageTags.forEach(Assert::assertNotNull);
    }

    @Test
    @DisplayName("Publication does not contain the main title in the alternative titles when the datacite document"
        + " has many titles")
    public void publicationDoesNotContainMainTitleInAlternativeTItleWhenDataciteDocHasManyTitles()
            throws IOException, URISyntaxException, InvalidPageTypeException, InvalidIssnException {
        Publication publication = readPublicationWithMutlipleTitles();
        String mainTitle = publication.getEntityDescription().getMainTitle();
        Set<String> altTitles = publication.getEntityDescription().getAlternativeTitles().keySet();
        assertFalse(altTitles.contains(mainTitle));
    }

    @Test
    @DisplayName("Publication contains alternative titles with valid language URIs when the datacite document has "
        + " many titles")
    public void publicationContainsAlternativeTtitlesWithValidLanguageURisWhenDataciteDocHasManyTitles()
            throws IOException, URISyntaxException, InvalidPageTypeException, InvalidIssnException {
        Publication publication = readPublicationWithMutlipleTitles();
        Map<String, String> alternativeTitles = publication.getEntityDescription().getAlternativeTitles();
        Collection<String> languageTags = alternativeTitles.values();
        languageTags.forEach(Assert::assertNotNull);
        List<URI> languageUris = languageTags.stream().map(URI::create).collect(Collectors.toList());
        List<URI> validUris = new ArrayList<>(LanguageMapper.languageUris());
        // for some reason hamcrest containsInAnyOrder does not want to work
        assertTrue(validUris.containsAll(languageUris));
    }

    private Publication readPublicationWithMutlipleTitles() throws IOException, URISyntaxException,
            InvalidPageTypeException, InvalidIssnException {
        String input = IoUtils.stringFromResources(Path.of(ENTRY_WITH_ALTERNATIVE_TITLE));
        DataciteResponseConverter converter = new DataciteResponseConverter();
        DataciteResponse response = objectMapper.readValue(input, DataciteResponse.class);
        return converter.toPublication(response, now(), SOME_ID,
            SOME_OWNER, SOME_URI);
    }
}
