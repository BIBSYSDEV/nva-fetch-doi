package no.unit.nva.doi.transformer;

import static java.time.Instant.now;
import static no.unit.nva.doi.transformer.DoiTransformerConfig.objectMapper;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import no.unit.nva.doi.transformer.model.datacitemodel.DataciteAffiliation;
import no.unit.nva.doi.transformer.model.datacitemodel.DataciteCreator;
import no.unit.nva.doi.transformer.model.datacitemodel.DataciteResponse;
import no.unit.nva.doi.transformer.model.datacitemodel.DataciteRights;
import no.unit.nva.model.Publication;
import no.unit.nva.model.exceptions.InvalidIssnException;
import nva.commons.core.JsonUtils;
import nva.commons.core.ioutils.IoUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

public class DataciteResponseConverterTest {

    public static final String ENTRY_WITH_ALTERNATIVE_TITLE = "datacite_many_titles.json";
    public static final Path SAMPLE_DATACITE_RESPOSNE = Path.of("datacite_response.json");
    private static final UUID SOME_ID = UUID.randomUUID();
    private static final String SOME_OWNER = "SomeOwner";
    private static final URI SOME_URI = URI.create("SomeUri");


    @DisplayName("DataciteResponseConverter::toPublication returns valid JSON when input is valid")
    @Test
    public void toPublicationReturnsValidJsonWhenInputIsValid() throws IOException, URISyntaxException,
                                                                       InvalidIssnException {

        DataciteResponse dataciteResponse = sampleDataciteResponse();
        Publication publication = toPublication(dataciteResponse);
        String json = objectMapper.writeValueAsString(publication);
        assertNotNull(json);
    }

    @DisplayName("DataciteResponseConverter::toName creates valid, inverted name when input is valid")
    @Test
    public void toNameReturnsInvertedNameStringWhenInputIsValid() {
        DataciteCreator dataciteCreator = new DataciteCreator();
        dataciteCreator.setFamilyName("Family");
        dataciteCreator.setGivenName("Given");
        DataciteResponseConverter converter = new DataciteResponseConverter();

        String name = converter.toName(dataciteCreator);

        assertEquals("Family, Given", name);
    }

    @Test
    @DisplayName("Publication contains alternativeTitles with non null langauge tags when datacite document has "
                 + "many titles")
    public void publicationContainsAlternativeTitlesWithNonNullLanguageTagsWhenDatataciteDocumentHasManyTitles()
        throws IOException, URISyntaxException, InvalidIssnException {
        Publication publication = readPublicationWithMutlipleTitles();
        Map<String, String> alternativeTitles = publication.getEntityDescription().getAlternativeTitles();
        Collection<String> languageTags = alternativeTitles.values();
        languageTags.forEach(Assertions::assertNotNull);
    }

    @Test
    @DisplayName("Publication does not contain the main title in the alternative titles when the datacite document"
                 + " has many titles")
    public void publicationDoesNotContainMainTitleInAlternativeTItleWhenDataciteDocHasManyTitles()
        throws IOException, URISyntaxException, InvalidIssnException {
        Publication publication = readPublicationWithMutlipleTitles();
        String mainTitle = publication.getEntityDescription().getMainTitle();
        Set<String> altTitles = publication.getEntityDescription().getAlternativeTitles().keySet();
        assertFalse(altTitles.contains(mainTitle));
    }

    @Test
    @DisplayName("Publication contains alternative titles with valid language URIs when the datacite document has "
                 + " many titles")
    public void publicationContainsAlternativeTtitlesWithValidLanguageURisWhenDataciteDocHasManyTitles()
        throws IOException, URISyntaxException, InvalidIssnException {
        Publication publication = readPublicationWithMutlipleTitles();
        Map<String, String> alternativeTitles = publication.getEntityDescription().getAlternativeTitles();
        Collection<String> languageTags = alternativeTitles.values();
        languageTags.forEach(Assertions::assertNotNull);
        List<URI> languageUris = languageTags.stream().map(URI::create).collect(Collectors.toList());
        List<URI> validUris = new ArrayList<>(LanguageMapper.languageUris());
        // for some reason hamcrest containsInAnyOrder does not want to work
        assertTrue(validUris.containsAll(languageUris));
    }

    @Test
    public void toPublicationReturnsNvaPublicationWithoutContributorsThatDoNotHaveFirstOrLastName()
        throws IOException, InvalidIssnException, URISyntaxException {
        DataciteResponse dataciteSample = sampleDataciteResponse();
        List<DataciteCreator> newContributors = new ArrayList<>(dataciteSample.getCreators());
        int expectedNumberOfContributors = sampleDataciteResponse().getContributors().size();

        DataciteAffiliation affiliation = new DataciteAffiliation();
        affiliation.setAffiliation("someAffiliation");
        DataciteCreator anonymousContributor = new DataciteCreator();
        anonymousContributor.setAffiliation(List.of(affiliation));
        anonymousContributor.setNameType("ORGANIZATIONAL");


        newContributors.add(anonymousContributor);
        dataciteSample.setCreators(newContributors);
        Publication publication = toPublication(dataciteSample);
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

    private Publication toPublication(DataciteResponse dataciteResponse)
        throws URISyntaxException, InvalidIssnException {
        DataciteResponseConverter converter = new DataciteResponseConverter();
        return converter.toPublication(dataciteResponse, now(), UUID.randomUUID(), "junit",
                                       URI.create("http://example.org/123"));
    }

    private DataciteResponse sampleDataciteResponse() throws IOException {
        return objectMapper.readValue(IoUtils.stringFromResources(SAMPLE_DATACITE_RESPOSNE), DataciteResponse.class);
    }

    private Publication readPublicationWithMutlipleTitles() throws IOException, URISyntaxException,
                                                                   InvalidIssnException {
        String input = IoUtils.stringFromResources(Path.of(ENTRY_WITH_ALTERNATIVE_TITLE));
        DataciteResponseConverter converter = new DataciteResponseConverter();
        DataciteResponse response = objectMapper.readValue(input, DataciteResponse.class);
        return converter.toPublication(response, now(), SOME_ID,
                                       SOME_OWNER, SOME_URI);
    }
}
