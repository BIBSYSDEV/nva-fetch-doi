package no.unit.nva.doi;

import static no.unit.nva.doi.CrossRefClient.WORKS;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.util.Optional;
import no.bibsys.aws.tools.IoUtils;
import no.unit.nva.doi.utils.AbstractLambdaTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

public class CrossRefClientTest extends AbstractLambdaTest {

    private CrossRefClient crossRefClient;

    public static final String ILLEGAL_DOI_STRING = "doi:" + DOI_STRING;

    @BeforeEach
    void before() throws IOException {
        HttpClient httpClient = mockHttpClientWithNonEmptyResponse();
        crossRefClient = new CrossRefClient(httpClient);
    }

    @DisplayName("createTargetUrl returns a valid Url for DOI strings that are not DOI URLs")
    @Test
    public void createTargetUrlReturnsAValidUrlForDoiStringThatIsNotDoiURL()
        throws URISyntaxException {
        String expected = String.join("/", CrossRefClient.CROSSREF_LINK, WORKS, DOI_STRING);

        String output = crossRefClient.createUrlToCrossRef(DOI_STRING).toString();
        assertThat(output, is(equalTo(expected)));
    }

    @DisplayName("createTargetUrl returns a valid Url for DOI strings that are DOI DX URLs")
    @Test
    public void createTargetUrlReturnsAValidUrlForDoiStringThatIsDoiDxUrl()
        throws URISyntaxException {
        targetURlReturnsAValidUrlForDoiStrings(DOI_DX_URL_PREFIX);
    }

    @DisplayName("createTargetUrl returns a valid Url for DOI strings that are DOI URLs")
    @Test
    public void createTargetUrlReturnsAValidUrlForDoiStringThatIsDoiURL()
        throws URISyntaxException {
        targetURlReturnsAValidUrlForDoiStrings(DOI_URL_PREFIX);
    }

    @Test
    @DisplayName("fetchDataForDoi returns an Optional with a json object for an existing URL")
    public void fetchDataForDoiReturnAnOptionalWithAJsonObjectForAnExistingUrl()
        throws IOException, URISyntaxException {
        Optional<String> result = crossRefClient.fetchDataForDoi(DOI_STRING).map(MetadataAndContentLocation::getJson);
        String expected = IoUtils.resourceAsString(CrossRefSamplePath);
        assertThat(result.isPresent(), is(true));
        assertThat(result.get(), is(equalTo(expected)));
    }

    @Test
    @DisplayName("fetchDataForDoi returns an empty Optional for a non existing URL")
    public void fetchDataForDoiReturnAnEmptyOptionalForANonExistingUrl()
        throws URISyntaxException {

        CrossRefClient crossRefClient = crossRefClientReceives404();

        Optional<String> result = crossRefClient.fetchDataForDoi(DOI_STRING).map(MetadataAndContentLocation::getJson);
        assertThat(result.isEmpty(), is(true));
    }

    @Test
    @DisplayName("fetchDataForDoi returns an empty Optional for an unknown error")
    public void fetchDataForDoiReturnAnEmptyOptionalForAnUnknownError()
        throws URISyntaxException {
        CrossRefClient crossRefClient = crossRefClientReceives500();
        Optional<String> result = crossRefClient.fetchDataForDoi(DOI_STRING).map(MetadataAndContentLocation::getJson);
        assertTrue(result.isEmpty());
    }

    @Test
    public void fetchDataForDoiThrowsExceptionWhenDoiHasInvalidFormat() {
        assertThrows(IllegalArgumentException.class, () -> crossRefClient.fetchDataForDoi(ILLEGAL_DOI_STRING));
    }

    private void targetURlReturnsAValidUrlForDoiStrings(String doiPrefix)
        throws URISyntaxException {
        String doiURL = String.join("/", doiPrefix, DOI_STRING);
        String expected = String.join("/", CrossRefClient.CROSSREF_LINK, WORKS, DOI_STRING);

        String output = crossRefClient.createUrlToCrossRef(doiURL).toString();
        assertThat(output, is(equalTo(expected)));
    }
}
