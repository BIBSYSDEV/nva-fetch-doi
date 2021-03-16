package no.unit.nva.metadata.service;

import static com.github.jsonldjava.core.JsonLdProcessor.frame;
import static nva.commons.core.ioutils.IoUtils.inputStreamFromResources;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.jsonldjava.core.JsonLdOptions;
import com.github.jsonldjava.utils.JsonUtils;
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
import java.util.Set;
import java.util.stream.Collectors;
import no.unit.nva.api.CreatePublicationRequest;
import no.unit.nva.metadata.DcTerms;
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

public class MetadataService {

    public static final String QUERY_SPARQL = "query.sparql";
    public static final String EMPTY_BASE_URI = "";
    public static final String CONTEXT_JSON = "context.json";
    public static final String MISSING_CONTEXT_OBJECT_FILE = "Missing context object file";
    public static final String REPLACEMENT_MARKER = "__URI__";
    public static final String SINDICE_DC_URI_PART = "http://vocab.sindice.net/any23#dc.";
    public static final String SINDICE_DCTERMS_URI_PART = "http://vocab.sindice.net/any23#dcterms.";
    public static final String DCTERMS_PREFIX = "http://purl.org/dc/terms/";
    public static final String DOT = ".";
    private static final ObjectMapper objectMapper = new ObjectMapper();
    private static final Logger logger = LoggerFactory.getLogger(MetadataService.class);
    private static final Set<String> HIGHWIRE_DATES = Set.of("citation_publication_date", "citation_cover_date",
            "citation_date");
    public static final String DATE_PROPERTY_NAME = "date";
    public static final String CITATION_LANGUAGE = "citation_language";
    private static final String LANGUAGE_PROPERTY_NAME = "language";
    private final TranslatorService translatorService;
    private final Repository db = new SailRepository(new MemoryStore());

    public MetadataService() throws IOException {
        translatorService = new TranslatorService();
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

    private ByteArrayInputStream loadExtractedData() {
        return new ByteArrayInputStream(translatorService.getOutputStream().toByteArray());
    }

    private Model getMetadata(URI uri) throws ExtractionException, IOException, URISyntaxException {
        translatorService.loadMetadataFromUri(uri);
        try (RepositoryConnection repositoryConnection = db.getConnection()) {
            repositoryConnection.add(loadExtractedData(), EMPTY_BASE_URI, RDFFormat.JSONLD);
            try (RepositoryResult<Statement> statements = repositoryConnection.getStatements(null, null, null)) {
                return normalizeStatements(statements);
            }
        } finally {
            db.shutDown();
        }
    }

    @SuppressWarnings("PMD.CloseResource")
    private Model normalizeStatements(RepositoryResult<Statement> statements) {
        ValueFactory valueFactory = SimpleValueFactory.getInstance();
        Model model = new TreeModel();
        for (Statement statement : statements) {
            if (isSindiceDcOrDcTerms(statement)) {
                model.add(toDctermsNamespace(valueFactory, statement));
            } else if (isHighwireDate(statement)) {
                model.add(toDctermsDate(valueFactory, statement));
            } else if (isHighWireLanguage(statement)) {
                model.add(toDctermsLanguage(valueFactory, statement));
            } else {
                model.add(statement);
            }
        }
        return model;
    }

    private Statement toDctermsLanguage(ValueFactory valueFactory, Statement statement) {
        IRI dateProperty = valueFactory.createIRI(DCTERMS_PREFIX, LANGUAGE_PROPERTY_NAME);
        return valueFactory.createStatement(statement.getSubject(), dateProperty, statement.getObject());    }

    private Statement toDctermsDate(ValueFactory valueFactory, Statement statement) {
        IRI dateProperty = valueFactory.createIRI(DCTERMS_PREFIX, DATE_PROPERTY_NAME);
        return valueFactory.createStatement(statement.getSubject(), dateProperty, statement.getObject());
    }

    private boolean isHighWireLanguage(Statement statement) {
        String property = getLocalName(statement);
        return CITATION_LANGUAGE.equals(property.toLowerCase(Locale.ROOT));
    }

    private String getLocalName(Statement statement) {
        return statement.getPredicate().getLocalName();
    }

    private boolean isHighwireDate(Statement statement) {
        String property = getLocalName(statement);
        return HIGHWIRE_DATES.contains(property.toLowerCase(Locale.ROOT));
    }

    private boolean isSindiceDcOrDcTerms(Statement statement) {
        return statement.getPredicate().toString().toLowerCase(Locale.ROOT).startsWith(SINDICE_DC_URI_PART)
            || statement.getPredicate().toString().toLowerCase(Locale.ROOT).startsWith(SINDICE_DCTERMS_URI_PART);
    }

    private Statement toDctermsNamespace(ValueFactory valueFactory, Statement statement) {
        String rawProperty = getLocalName(statement);
        String dcLocalName = rawProperty.substring(rawProperty.lastIndexOf(DOT) + 1);
        Optional<DcTerms> dcTerms = DcTerms.getTermByValue(dcLocalName);

        if (dcTerms.isPresent()) {
            return valueFactory.createStatement(statement.getSubject(),
                    dcTerms.get().getIri(),
                    statement.getObject());
        } else {
            logger.warn("Received <" + rawProperty + "> claimed as a DC property");
            return statement;
        }
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
            var type = new TypeReference<Map<String, Object>>() {};
            return objectMapper.readValue(inputStreamFromResources(CONTEXT_JSON), type);
        } catch (IOException e) {
            throw new RuntimeException(MISSING_CONTEXT_OBJECT_FILE);
        }
    }

    private String getQueryAsString(URI uri) {
        var inputStream = inputStreamFromResources(MetadataService.QUERY_SPARQL);
        return new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8)).lines()
            .collect(Collectors.joining(System.lineSeparator())).replaceAll(REPLACEMENT_MARKER, uri.toString());
    }
}
