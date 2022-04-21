package no.unit.nva.doi.model;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import com.fasterxml.jackson.core.JsonProcessingException;
import no.sikt.nva.doi.fetch.jsonconfig.Json;
import no.unit.nva.doi.fetch.model.utils.MetadataSource;
import org.junit.jupiter.api.Test;

class DoiProxyResponseTest {

    @Test
    void canMapDoiProxyResponse() throws JsonProcessingException {
        var response = new DoiProxyResponse(
            Json.createObjectNode(),
            MetadataSource.DataCite.name()
        );
        var mappedResponse = Json.readValue(Json.writeValueAsString(response), DoiProxyResponse.class);

        assertThat(mappedResponse, is(equalTo(response)));
    }
}
