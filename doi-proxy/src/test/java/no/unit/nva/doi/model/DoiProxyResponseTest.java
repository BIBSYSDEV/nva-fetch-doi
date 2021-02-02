package no.unit.nva.doi.model;

import static nva.commons.core.JsonUtils.objectMapper;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;

import com.fasterxml.jackson.core.JsonProcessingException;
import no.unit.nva.doi.fetch.model.utils.MetadataSource;
import org.junit.jupiter.api.Test;

public class DoiProxyResponseTest {


    @Test
    public void canMapDoiProxyResponse() throws JsonProcessingException {
        DoiProxyResponse response = new DoiProxyResponse(
            objectMapper.createObjectNode(),
            MetadataSource.DataCite.name()
        );

        DoiProxyResponse mappedResponse = objectMapper.readValue(
            objectMapper.writeValueAsString(response),
            DoiProxyResponse.class
        );

        assertThat(mappedResponse,is(equalTo(response)));
    }
}
