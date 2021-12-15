package no.unit.nva.doi.transformer;

import static java.time.Instant.now;
import static no.unit.nva.doi.transformer.DoiTransformerConfig.doiTransformerObjectMapper;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
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
import no.unit.nva.model.Contributor;
import no.unit.nva.model.Publication;
import no.unit.nva.model.exceptions.InvalidIssnException;
import nva.commons.core.ioutils.IoUtils;
import nva.commons.doi.DoiConverter;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

public class DataciteResponseConverterTest {

    public static final String ENTRY_WITH_ALTERNATIVE_TITLE = "datacite_many_titles.json";
    public static final Path SAMPLE_DATACITE_RESPOSNE = Path.of("datacite_response.json");
    private static final UUID SOME_ID = UUID.randomUUID();
    private static final String SOME_OWNER = "SomeOwner";
    private static final URI SOME_URI = URI.create("SomeUri");


    @Test
    void defaultConstructorExists() {
        new DataciteResponseConverter();
    }

    @DisplayName("DataciteResponseConverter::toPublication returns valid JSON when input is valid")
    @Test
    public void toPublicationReturnsValidJsonWhenInputIsValid() throws IOException, URISyntaxException,
                                                                       InvalidIssnException {
        DataciteResponse dataciteResponse = sampleDataciteResponse();
        Publication publication = toPublication(dataciteResponse);
        String json = doiTransformerObjectMapper.writeValueAsString(publication);
        assertNotNull(json);
    }

    @Test
    @DisplayName("Publication contains alternativeTitles with non null langauge tags when datacite document has "
                 + "many titles")
    public void publicationContainsAlternativeTitlesWithNonNullLanguageTagsWhenDatataciteDocumentHasManyTitles()
        throws IOException, URISyntaxException, InvalidIssnException {
        Publication publication = readPublicationWithMultipleTitles();
        Map<String, String> alternativeTitles = publication.getEntityDescription().getAlternativeTitles();
        Collection<String> languageTags = alternativeTitles.values();
        languageTags.forEach(Assertions::assertNotNull);
    }

    @Test
    @DisplayName("Publication does not contain the main title in the alternative titles when the datacite document"
                 + " has many titles")
    public void publicationDoesNotContainMainTitleInAlternativeTItleWhenDataciteDocHasManyTitles()
        throws IOException, URISyntaxException, InvalidIssnException {
        Publication publication = readPublicationWithMultipleTitles();
        String mainTitle = publication.getEntityDescription().getMainTitle();
        Set<String> altTitles = publication.getEntityDescription().getAlternativeTitles().keySet();
        assertFalse(altTitles.contains(mainTitle));
    }

    @Test
    @DisplayName("Publication contains alternative titles with valid language URIs when the datacite document has "
                 + " many titles")
    public void publicationContainsAlternativeTitlesWithValidLanguageURisWhenDataciteDocHasManyTitles()
        throws IOException, URISyntaxException, InvalidIssnException {
        Publication publication = readPublicationWithMultipleTitles();
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
        int expectedNumberOfContributors = sampleDataciteResponse().getContributors().size();
        DataciteResponse dataciteSample = sampleDataciteResponse();
        List<DataciteCreator> newContributors = new ArrayList<>(dataciteSample.getCreators());
        newContributors.add(getDataciteCreator());
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
        DataciteResponseConverter converter = new DataciteResponseConverter(new DoiConverter(uri -> true));
        return converter.toPublication(dataciteResponse, now(), UUID.randomUUID(), "junit",
                                       URI.create("http://example.org/123"));
    }

    private DataciteResponse sampleDataciteResponse() throws IOException {
        return doiTransformerObjectMapper.readValue(IoUtils.stringFromResources(SAMPLE_DATACITE_RESPOSNE),
            DataciteResponse.class);
    }

    private Publication readPublicationWithMultipleTitles() throws IOException, URISyntaxException,
                                                                   InvalidIssnException {
        String input = IoUtils.stringFromResources(Path.of(ENTRY_WITH_ALTERNATIVE_TITLE));
        DataciteResponseConverter converter = new DataciteResponseConverter(new DoiConverter(uri -> true));
        DataciteResponse response = doiTransformerObjectMapper.readValue(input, DataciteResponse.class);
        return converter.toPublication(response, now(), SOME_ID,
                                       SOME_OWNER, SOME_URI);
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
