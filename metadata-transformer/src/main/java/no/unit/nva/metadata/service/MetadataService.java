package no.unit.nva.metadata.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.jsonldjava.core.JsonLdOptions;
import com.github.jsonldjava.utils.JsonUtils;
import no.unit.nva.api.CreatePublicationRequest;
import no.unit.nva.metadata.MetadataConverter;
import org.apache.any23.extractor.ExtractionException;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.impl.TreeModel;
import org.eclipse.rdf4j.query.QueryResults;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.RepositoryResult;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.Rio;
import org.eclipse.rdf4j.sail.memory.MemoryStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import static com.github.jsonldjava.core.JsonLdProcessor.frame;
import static nva.commons.core.ioutils.IoUtils.inputStreamFromResources;

public class MetadataService {

    public static final String QUERY_SPARQL = "query.sparql";
    public static final String EMPTY_BASE_URI = "";
    public static final String CONTEXT_JSON = "context.json";
    public static final String MISSING_CONTEXT_OBJECT_FILE = "Missing context object file";
    private static final ObjectMapper objectMapper = new ObjectMapper();
    public static final String REPLACEMENT_MARKER = "__URI__";
    public static final String NEWLINE_DELIMITER = "\n";
    public static final String SINDICE_DC_URI_PART = "http://vocab.sindice.net/any23#dc.";
    public static final String SINDICE_DCTERMS_URI_PART = "http://vocab.sindice.net/any23#dcterms.";
    public static final String DCTERMS_PREFIX = "http://purl.org/dc/terms/";
    public static final String DOT = ".";

    private final TranslatorService translatorService;
    private final Repository db = new SailRepository(new MemoryStore());
    private static final Logger logger = LoggerFactory.getLogger(MetadataService.class);

    public MetadataService() throws IOException {
        translatorService = new TranslatorService();
    }

    private ByteArrayInputStream loadExtractedData() {
        return new ByteArrayInputStream(translatorService.getOutputStream().toByteArray());
    }

    /**
     * Construct a CreatePublicationRequest for metadata extracted from a supplied URI.
     *
     * @param uri URI to dereference.
     * @return CreatePublicationRequest for selected set of metadata.
     */
    public Optional<CreatePublicationRequest> getCreatePublicationRequest(URI uri) {
        try {
            Model metadata = getMetadata(uri);
            String jsonld = toFramedJsonLd(getModelFromQuery(metadata, uri));
            MetadataConverter converter = new MetadataConverter(metadata, jsonld);
            return Optional.ofNullable(converter.toRequest());
        } catch (Exception e) {
            logger.error("Error mapping metadata to CreatePublicationRequest", e);
            return Optional.empty();
        }
    }

    private Model getMetadata(URI uri) throws ExtractionException, IOException, URISyntaxException {
        translatorService.loadMetadataFromUri(uri);
        try (RepositoryConnection repositoryConnection = db.getConnection()) {
            repositoryConnection.add(loadExtractedData(), EMPTY_BASE_URI, RDFFormat.JSONLD);
            try (RepositoryResult<Statement> statements = repositoryConnection.getStatements(null, null, null)) {
                return fixUpSindiceDcAndDctermsToDcterms(statements);
            }
        } finally {
            db.shutDown();
        }
    }

    private Model fixUpSindiceDcAndDctermsToDcterms(RepositoryResult<Statement> statements) {
        ValueFactory valueFactory = SimpleValueFactory.getInstance();
        Model model = new TreeModel();
        while (statements.hasNext()) {
            Statement statement = statements.next();
            if (isSindiceDcOrDcTerms(statement)) {
                model.add(valueFactory.createStatement(statement.getSubject(),
                        toDcNamespace(statement.getPredicate(), valueFactory),
                        statement.getObject()));
            } else {
                model.add(statement);
            }
        }
        return model;
    }

    private boolean isSindiceDcOrDcTerms(Statement statement) {
        return statement.getPredicate().toString().toLowerCase(Locale.ROOT).startsWith(SINDICE_DC_URI_PART)
                || statement.getPredicate().toString().toLowerCase(Locale.ROOT).startsWith(SINDICE_DCTERMS_URI_PART);
    }

    private IRI toDcNamespace(IRI predicate, ValueFactory valueFactory) {
        String rawProperty = predicate.getLocalName();
        String property = rawProperty.substring(rawProperty.lastIndexOf(DOT) + 1);
        return valueFactory.createIRI(DCTERMS_PREFIX, property);
    }

    private String toFramedJsonLd(Model model) throws IOException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        Rio.write(model, outputStream, RDFFormat.JSONLD);
        var jsonObject = JsonUtils.fromInputStream(new ByteArrayInputStream(outputStream.toByteArray()));
        Object framed = frame(jsonObject, loadContext(), avoidBlankNodeIdentifiersAndOmitDefaultsConfig());
        return JsonUtils.toPrettyString(framed);
    }

    private Model getModelFromQuery(Model model, URI uri) {
        try (RepositoryConnection repositoryConnection = db.getConnection()) {
            repositoryConnection.add(model);
            var query = repositoryConnection.prepareGraphQuery(getQueryAsString(uri));
            return QueryResults.asModel(query.evaluate());
        } finally {
            db.shutDown();
        }
    }

    /**
     * Configures JSON-LD processing. Uses JSON-LD 1.1 processing to avoid the inclusion of blank node identifiers,
     * suppresses generation of a base graph node, omits default so nulls are removed.
     *
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
            var type = new TypeReference<Map<String, Object>>() {
            };
            return objectMapper.readValue(inputStreamFromResources(CONTEXT_JSON), type);
        } catch (IOException e) {
            throw new RuntimeException(MISSING_CONTEXT_OBJECT_FILE);
        }
    }

    private String getQueryAsString(URI uri) {
        var inputStream = inputStreamFromResources(MetadataService.QUERY_SPARQL);
        return new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8)).lines()
                .collect(Collectors.joining(NEWLINE_DELIMITER)).replaceAll(REPLACEMENT_MARKER, uri.toString());
    }
}
