package no.unit.nva.doi.transformer.model.crossrefmodel;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.File;
import java.io.IOException;
import nva.commons.utils.JsonUtils;
import org.junit.jupiter.api.Test;

public class CrossrefDocumentTest {

    private final ObjectMapper objectMapper = JsonUtils.objectMapper;

    @Test
    public void test() throws IOException {

        CrossRefDocument crossRefDocument = objectMapper.readValue(new File("src/test/resources/crossref.json"),
            CrossRefDocument.class);

        String asString = objectMapper.writeValueAsString(crossRefDocument);
    }

}
