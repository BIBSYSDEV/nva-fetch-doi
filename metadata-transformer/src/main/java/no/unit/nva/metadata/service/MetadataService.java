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
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
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
import org.eclipse.rdf4j.model.Value;
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

    private static final String QUERY_SPARQL = "query.sparql";
    private static final String EMPTY_BASE_URI = "";
    private static final String CONTEXT_JSON = "context.json";
    private static final String MISSING_CONTEXT_OBJECT_FILE = "Missing context object file";
    private static final String REPLACEMENT_MARKER = "__URI__";
    private static final String SINDICE_DC_URI_PART = "http://vocab.sindice.net/any23#dc.";
    private static final String SINDICE_DCTERMS_URI_PART = "http://vocab.sindice.net/any23#dcterms.";
    private static final String DOT = ".";
    private static final ObjectMapper objectMapper = new ObjectMapper();
    private static final Logger logger = LoggerFactory.getLogger(MetadataService.class);
    private static final Set<String> HIGHWIRE_DATES = Set.of("citation_publication_date", "citation_cover_date",
            "citation_date");
    private static final String CITATION_LANGUAGE = "citation_language";
    private static final Set<String> DOI_PREDICATES = Set.of("http://vocab.sindice.net/any23#dc.identifier",
            "http://vocab.sindice.net/any23#dcterms.identifier", "http://vocab.sindice.net/any23#citation_doi");
    private static final String DOI_DISPLAY_REGEX = "(doi:|doc:|http(s)?://(dx\\.)?doi\\.org/)?10\\.\\d{4,9}+/.*";
    private static final String SHORT_DOI_REGEX = "^http(s)?://doi.org/[^/]+(/)?$";
    private static final String BIBO_DOI = "http://purl.org/ontology/bibo/doi";
    private static final String DOI_PREFIX = "https://doi.org/";
    private static final String HEAD = "HEAD";
    private static final String LOCATION = "location";
    private static final int MOVED_PERMANENTLY = 301;
    private static final String DOI_FIRST_PART = "10";
    private static final String HTTPS = "https";
    private static final String HTTP = "http";
    public static final String CITATION_AUTHOR = "citation_author";
    private final HttpClient httpClient;
    private final TranslatorService translatorService;
    private final Repository db = new SailRepository(new MemoryStore());

    public MetadataService() throws IOException {
        this(getDefaultHttpClient());
    }

    public MetadataService(HttpClient httpClient) {
        this.translatorService = new TranslatorService();
        this.httpClient = httpClient;
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

    private static HttpClient getDefaultHttpClient() {
        return HttpClient.newBuilder()
                .followRedirects(HttpClient.Redirect.NEVER)
                .build();
    }

    private ByteArrayInputStream loadExtractedData() {
        return new ByteArrayInputStream(translatorService.getOutputStream().toByteArray());
    }

    private Model getMetadata(URI uri) throws ExtractionException, IOException, URISyntaxException,
            InterruptedException {
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
    private Model normalizeStatements(RepositoryResult<Statement> statements) throws IOException, InterruptedException {
        ValueFactory valueFactory = SimpleValueFactory.getInstance();
        Model model = new TreeModel();
        for (Statement statement : statements) {
            if (isDoi(statement)) {
                Optional<Statement> doi = toBiboDoi(valueFactory, statement);
                doi.ifPresent(model::add);
            } else if (isSindiceDcOrDcTerms(statement)) {
                model.add(toDctermsNamespace(valueFactory, statement));
            } else if (isHighwireDate(statement)) {
                model.add(toDctermsDate(valueFactory, statement));
            } else if (isHighWireLanguage(statement)) {
                model.add(toDctermsLanguage(valueFactory, statement));
            } else if (isHighWireAuthor(statement)) {
                model.add(toDctermsCreator(valueFactory, statement));
            } else {
                model.add(statement);
            }
        }
        return model;
    }

    private Statement toDctermsCreator(ValueFactory valueFactory, Statement statement) {
        return valueFactory.createStatement(statement.getSubject(),
                DcTerms.CREATOR.getIri(valueFactory), statement.getObject());
    }

    private boolean isHighWireAuthor(Statement statement) {
        return statement.getPredicate().toString().endsWith(CITATION_AUTHOR);
    }

    private Optional<Statement> toBiboDoi(ValueFactory valueFactory, Statement statement) throws IOException,
            InterruptedException {
        String value = statement.getObject().stringValue();
        String doi = isShortDoi(value) ? fetchDoiUriFromShortDoi(value).orElse(null)
                : DOI_PREFIX + value.substring(value.indexOf(DOI_FIRST_PART));
        IRI property = valueFactory.createIRI(BIBO_DOI);
        return Optional.ofNullable(valueFactory.createStatement(statement.getSubject(),
                property, valueFactory.createLiteral(doi)));
    }

    private Optional<String> fetchDoiUriFromShortDoi(String value) throws IOException, InterruptedException {
        String uri = value.startsWith(HTTPS) ? value : value.replace(HTTP, HTTPS);
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(uri))
                .method(HEAD, HttpRequest.BodyPublishers.noBody())
                .build();
        HttpResponse<Void> response = httpClient.send(request, HttpResponse.BodyHandlers.discarding());
        return response.statusCode() == MOVED_PERMANENTLY ? response.headers().firstValue(LOCATION) : Optional.empty();
    }

    private boolean isDoi(Statement statement) {
        return isPotentialDoiPredicate(statement) && isDoiString(statement.getObject());
    }

    private boolean isDoiString(Value object) {
        String value = object.stringValue();
        if (value.toLowerCase(Locale.ROOT).matches(DOI_DISPLAY_REGEX)) {
            return true;
        } else {
            return isShortDoi(value);
        }
    }

    private boolean isShortDoi(String value) {
        return value.matches(SHORT_DOI_REGEX);
    }

    private boolean isPotentialDoiPredicate(Statement statement) {
        return DOI_PREDICATES.contains(statement.getPredicate().toString().toLowerCase(Locale.ROOT));
    }

    private Statement toDctermsLanguage(ValueFactory valueFactory, Statement statement) {
        return valueFactory.createStatement(statement.getSubject(),
                DcTerms.LANGUAGE.getIri(valueFactory), statement.getObject());
    }

    private Statement toDctermsDate(ValueFactory valueFactory, Statement statement) {
        return valueFactory.createStatement(statement.getSubject(),
                DcTerms.DATE.getIri(valueFactory), statement.getObject());
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
                    dcTerms.get().getIri(valueFactory),
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
