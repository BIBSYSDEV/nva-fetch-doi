package no.unit.nva.doi.model;

import static no.unit.nva.doi.DoiProxyConfig.doiProxyObjectMapper;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import com.fasterxml.jackson.core.JsonProcessingException;
import no.unit.nva.doi.fetch.model.utils.MetadataSource;
import org.junit.jupiter.api.Test;

;

public class DoiProxyResponseTest {

    @Test
    public void canMapDoiProxyResponse() throws JsonProcessingException {
        DoiProxyResponse response = new DoiProxyResponse(
            doiProxyObjectMapper.createObjectNode(),
            MetadataSource.DataCite.name()
        );

        DoiProxyResponse mappedResponse = doiProxyObjectMapper.readValue(
            doiProxyObjectMapper.writeValueAsString(response),
            DoiProxyResponse.class
        );

        assertThat(mappedResponse, is(equalTo(response)));
    }
}
