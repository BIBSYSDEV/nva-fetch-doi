package no.unit.nva.metadata.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.jsonldjava.core.JsonLdOptions;
import com.github.jsonldjava.core.JsonLdProcessor;
import com.github.jsonldjava.utils.JsonUtils;
import org.apache.any23.extractor.ExtractionException;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.query.QueryResults;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.Rio;
import org.eclipse.rdf4j.sail.memory.MemoryStore;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.stream.Collectors;

import static com.github.jsonldjava.core.JsonLdProcessor.frame;

public class MetadataService {

    public static final String QUERY_SPARQL = "/query.sparql";
    public static final String EMPTY_BASE_URI = "";
    public static final String CONTEXT_JSON = "/context.json";
    public static final String MISSING_CONTEXT_OBJECT_FILE = "Missing context object file";
    private static final ObjectMapper objectMapper = new ObjectMapper();
    public static final String REPLACEMENT_MARKER = "__URI__";
    public static final String NEWLINE_DELIMITER = "\n";

    private final TranslatorService translatorService;
    private final Repository db = new SailRepository(new MemoryStore());


    public MetadataService() throws IOException {
        translatorService = new TranslatorService();
    }

    private ByteArrayInputStream loadExtractedData() {
        return new ByteArrayInputStream(translatorService.getOutputStream().toByteArray());
    }

    /**
     * Extracts metadata from the HTML dereferenced from a supplied URI.
     * @param uri URI to dereference.
     * @return JSON representation of an NVA Publication.
     * @throws ExtractionException If the metadata extraction fails.
     * @throws IOException If resources files cannot be found.
     * @throws URISyntaxException If the URI is invalid.
     */
    public String getMetadataJson(URI uri) throws ExtractionException, IOException, URISyntaxException {
        Model metadata = getMetadata(uri);
        return toFramedJsonLd(metadata);
    }

    private Model getMetadata(URI uri) throws ExtractionException, IOException, URISyntaxException {
        translatorService.loadMetadataFromUri(uri);
        try (RepositoryConnection repositoryConnection = db.getConnection()) {
            repositoryConnection.add(loadExtractedData(), EMPTY_BASE_URI, RDFFormat.JSONLD);
            var query = repositoryConnection.prepareGraphQuery(getQueryAsString(uri));
            return QueryResults.asModel(query.evaluate());
        } finally {
            db.shutDown();
        }
    }

    private String toFramedJsonLd(Model resultModel) throws IOException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        Rio.write(resultModel, outputStream, RDFFormat.JSONLD);
        var jsonObject = JsonUtils.fromInputStream(new ByteArrayInputStream(outputStream.toByteArray()));
        Object framed = frame(jsonObject, loadContext(), avoidBlankNodeIdentifiersAndOmitDefaultsConfig());
        return JsonUtils.toPrettyString(framed);
    }

    /**
     * Configures JSON-LD processing. Uses JSON-LD 1.1 processing to avoid the inclusion of blank node identifiers,
     * suppresses generation of a base graph node, omits default so nulls are removed.
     * @return JsonLdOptions
     */
    private JsonLdOptions avoidBlankNodeIdentifiersAndOmitDefaultsConfig() {
        JsonLdOptions jsonLdOptions = new JsonLdOptions();
        jsonLdOptions.setProcessingMode(JsonLdOptions.JSON_LD_1_1);
        jsonLdOptions.setOmitGraph(true);
        jsonLdOptions.setOmitDefault(true);
        return jsonLdOptions;
    }

    private Map<String, Object> loadContext() {
        try {
            var type = new TypeReference<Map<String,Object>>() {};
            return objectMapper.readValue(getClass().getResource(CONTEXT_JSON), type);
        } catch (IOException e) {
            throw new RuntimeException(MISSING_CONTEXT_OBJECT_FILE);
        }
    }

    private String getQueryAsString(URI uri) {
        var inputStream = getClass().getResourceAsStream(MetadataService.QUERY_SPARQL);
        return new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8)).lines()
                .collect(Collectors.joining(NEWLINE_DELIMITER)).replaceAll(REPLACEMENT_MARKER, uri.toString());
    }

}