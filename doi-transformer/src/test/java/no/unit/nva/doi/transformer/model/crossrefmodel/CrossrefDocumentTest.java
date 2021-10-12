package no.unit.nva.doi.transformer.model.crossrefmodel;

import com.fasterxml.jackson.databind.JsonNode;
import nva.commons.core.ioutils.IoUtils;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Path;

import static no.unit.nva.doi.transformer.DoiTransformerConfig.doiTransformerObjectMapper;
import static no.unit.nva.hamcrest.DoesNotHaveNullOrEmptyFields.doesNotHaveNullOrEmptyFields;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;

public class CrossrefDocumentTest {

    private static final Path CROSSREF_RESOURCE = Path.of("crossref.json");
    private static final Path CROSSREF_BOOK_RESOURCE = Path.of("crossref_sample_book.json");
    private static final Path CROSSREF_BOOK_CHAPTER_RESOURCE = Path.of("crossref_sample_book_chapter.json");

    @Test
    public void testSettersAndGetters() throws IOException {

        String resourceString = IoUtils.stringFromResources(CROSSREF_RESOURCE);
        CrossrefApiResponse crossRefResponse = doiTransformerObjectMapper.readValue(resourceString, CrossrefApiResponse.class);
        String serializedObject = doiTransformerObjectMapper.writeValueAsString(crossRefResponse);
        CrossrefApiResponse deserializedObject = doiTransformerObjectMapper.readValue(serializedObject, CrossrefApiResponse.class);

        assertThat(crossRefResponse, doesNotHaveNullOrEmptyFields());
        assertThat(doiTransformerObjectMapper.convertValue(deserializedObject, JsonNode.class),
                   is(equalTo(doiTransformerObjectMapper.convertValue(crossRefResponse, JsonNode.class))));
    }

    @Test
    public void testSettersAndGettersCrossrefBook() throws IOException {

        String resourceString = IoUtils.stringFromResources(CROSSREF_BOOK_RESOURCE);
        CrossrefApiResponse crossRefResponse = doiTransformerObjectMapper.readValue(resourceString, CrossrefApiResponse.class);
        String serializedObject = doiTransformerObjectMapper.writeValueAsString(crossRefResponse);
        CrossrefApiResponse deserializedObject = doiTransformerObjectMapper.readValue(serializedObject, CrossrefApiResponse.class);

        assertThat(crossRefResponse, doesNotHaveNullOrEmptyFields());
        assertThat(doiTransformerObjectMapper.convertValue(deserializedObject, JsonNode.class),
                   is(equalTo(doiTransformerObjectMapper.convertValue(crossRefResponse, JsonNode.class))));

    }

    @Test
    public void testSettersAndGettersCrossrefBookChapter() throws IOException {

        String resourceString = IoUtils.stringFromResources(CROSSREF_BOOK_CHAPTER_RESOURCE);
        CrossrefApiResponse crossRefResponse = doiTransformerObjectMapper.readValue(resourceString, CrossrefApiResponse.class);
        String serializedObject = doiTransformerObjectMapper.writeValueAsString(crossRefResponse);
        CrossrefApiResponse deserializedObject = doiTransformerObjectMapper.readValue(serializedObject, CrossrefApiResponse.class);

        assertThat(crossRefResponse, doesNotHaveNullOrEmptyFields());
        assertThat(doiTransformerObjectMapper.convertValue(deserializedObject, JsonNode.class),
                   is(equalTo(doiTransformerObjectMapper.convertValue(crossRefResponse, JsonNode.class))));

    }


}
