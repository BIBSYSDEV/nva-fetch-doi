package no.unit.nva.doi.fetch.model;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import no.unit.nva.doi.fetch.ObjectMapperConfig;
import no.unit.nva.doi.fetch.model.utils.MetadataSource;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class DoiProxyResponseTest {

    private ObjectMapper objectMapper = ObjectMapperConfig.createObjectMapper();

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

        Assertions.assertNotNull(mappedResponse);
    }
}
