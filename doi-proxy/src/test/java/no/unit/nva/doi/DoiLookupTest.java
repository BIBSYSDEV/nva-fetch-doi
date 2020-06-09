package no.unit.nva.doi;

import static org.junit.Assert.assertEquals;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.MalformedURLException;
import no.unit.nva.doi.fetch.ObjectMapperConfig;
import org.junit.Test;

public class DoiLookupTest {

    public static final ObjectMapper objectMapper = ObjectMapperConfig.createObjectMapper();

    @Test
    public void test() throws MalformedURLException, JsonProcessingException {
        DoiLookup doiLookup = new DoiLookup();
        doiLookup.setDoi("https://doi.org/10.1109/5.771073");

        String json = objectMapper.writeValueAsString(doiLookup);
        System.out.println(json);

        DoiLookup processedDoiLookup = objectMapper.readValue(json, DoiLookup.class);

        assertEquals(processedDoiLookup.getDoi(), doiLookup.getDoi());
    }
}
