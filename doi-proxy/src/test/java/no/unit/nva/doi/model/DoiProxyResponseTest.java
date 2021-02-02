package no.unit.nva.doi.model;

import com.fasterxml.jackson.core.JsonProcessingException;
import no.unit.nva.doi.fetch.model.utils.MetadataSource;
import org.junit.jupiter.api.Test;

import static nva.commons.core.JsonUtils.objectMapperWithEmpty;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;

;

public class DoiProxyResponseTest {


    @Test
    public void canMapDoiProxyResponse() throws JsonProcessingException {
        DoiProxyResponse response = new DoiProxyResponse(
                objectMapperWithEmpty.createObjectNode(),
            MetadataSource.DataCite.name()
        );

        DoiProxyResponse mappedResponse = objectMapperWithEmpty.readValue(
                objectMapperWithEmpty.writeValueAsString(response),
            DoiProxyResponse.class
        );

        assertThat(mappedResponse,is(equalTo(response)));
    }
}
