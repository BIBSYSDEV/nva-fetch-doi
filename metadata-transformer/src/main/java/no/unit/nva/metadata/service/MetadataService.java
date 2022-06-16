package no.unit.nva.metadata.service;

import static java.util.Objects.nonNull;
import static nva.commons.core.attempt.Try.attempt;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.net.HttpHeaders;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Stream;
import no.sikt.nva.doi.fetch.jsonconfig.Json;
import no.unit.nva.metadata.CreatePublicationRequest;
import no.unit.nva.metadata.MetadataConverter;
import no.unit.nva.metadata.type.Bibo;
import no.unit.nva.metadata.type.Citation;
import no.unit.nva.metadata.type.DcTerms;
import no.unit.nva.metadata.type.OntologyProperty;
import no.unit.nva.metadata.type.RawMetaTag;
import nva.commons.apigateway.MediaTypes;
import nva.commons.core.Environment;
import nva.commons.core.SingletonCollector;
import nva.commons.core.paths.UriWrapper;
import org.apache.any23.extractor.ExtractionException;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.impl.TreeModel;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.RepositoryResult;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.sail.memory.MemoryStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MetadataService {

    public static final ValueFactory valueFactory = SimpleValueFactory.getInstance();
    public static final String API_HOST = new Environment().readEnv("API_HOST");
    private static final String EMPTY_BASE_URI = "";
    private static final Logger logger = LoggerFactory.getLogger(MetadataService.class);
    private static final String DOI_DISPLAY_REGEX = "(doi:|doc:|http(s)?://(dx\\.)?doi\\.org/)?10\\.\\d{4,9}+/.*";
    private static final String SHORT_DOI_REGEX = "^http(s)?://doi.org/[^/]+(/)?$";
    private static final String DOI_PREFIX = "https://doi.org/";
    private static final String HEAD = "HEAD";
    private static final String LOCATION = "location";
    private static final int MOVED_PERMANENTLY = 301;
    private static final String DOI_FIRST_PART = "10";
    private static final String HTTPS = "https";
    private static final String HTTP = "http";
    public static final String QUERY_PARAM_QUERY = "query";
    public static final String QUERY_PARAM_YEAR = "year";
    private final HttpClient httpClient;
    private final TranslatorService translatorService;
    private final Repository db = new SailRepository(new MemoryStore());
    private final URI publicationChannelsHostUri;
    private final URI publicationChannelsHostUriPublisher;

    public MetadataService() {
        this(getDefaultHttpClient(), defaultPublicationChannelsHostUri(), defaultPublicationChannelsHostUriPublisher());
    }

    /**
     * Deprecated constructor.
     *
     * @param httpClient the HttpClient.
     * @deprecated For testing, we should also inject the URI so that we can use WireMock.
     */
    @Deprecated
    public MetadataService(HttpClient httpClient) {
        this(httpClient, defaultPublicationChannelsHostUri(), defaultPublicationChannelsHostUriPublisher());
    }

    public MetadataService(HttpClient httpClient, URI publicationChannelsHostUri,
                           URI publicationChannelsHostUriPublisher) {
        this.translatorService = new TranslatorService();
        this.httpClient = httpClient;
        this.publicationChannelsHostUri = publicationChannelsHostUri;
        this.publicationChannelsHostUriPublisher = publicationChannelsHostUriPublisher;
    }

    /**
     * Construct a CreatePublicationRequest for metadata extracted from a supplied URI.
     *
     * @param uri URI to dereference.
     * @return CreatePublicationRequest for selected set of metadata.
     */
    public Optional<CreatePublicationRequest> generateCreatePublicationRequest(URI uri) {
        try {
            Model metadata = getMetadata(uri);
            MetadataConverter converter = new MetadataConverter(metadata);
            return converter.generateCreatePublicationRequest();
        } catch (Exception e) {
            logger.error("Error mapping metadata to CreatePublicationRequest", e);
            throw new RuntimeException(e);
        }
    }

    public Optional<String> fetchJournalIdFromPublicationChannel(String name,
                                                                 String electronicIssn,
                                                                 String printedIssn,
                                                                 int year) {
        return Stream
            .of(electronicIssn, printedIssn, name)
            .map(queryTerm -> fetchPublicationIdentifierFromPublicationChannels(queryTerm, year))
            .flatMap(Optional::stream)
            .findFirst();
    }

    public Optional<java.net.http.HttpHeaders> fetchHeadResponseHeadersFromUrl(String url) {
        return attempt(() -> fetchHeadResponseFromUrl(url))
                .map(this::parseHeadersFromOkResponse)
                .toOptional()
                .flatMap(Function.identity());
    }

    private Optional<java.net.http.HttpHeaders> parseHeadersFromOkResponse(HttpResponse<Void> response) {
        if (HttpURLConnection.HTTP_OK == response.statusCode()) {
            return Optional.of(response.headers());
        }
        return Optional.empty();
    }

    private HttpResponse<Void> fetchHeadResponseFromUrl(String url) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .method(HEAD, HttpRequest.BodyPublishers.noBody())
                .build();
        return httpClient.send(request, HttpResponse.BodyHandlers.discarding());
    }

    public static URI defaultPublicationChannelsHostUri() {
        return UriWrapper.fromHost(API_HOST).addChild("publication-channels").addChild("journal").getUri();
    }

    public static URI defaultPublicationChannelsHostUriPublisher() {
        return UriWrapper.fromHost(API_HOST).addChild("publication-channels").addChild("publisher").getUri();
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
        Model model = new TreeModel();
        for (Statement statement : statements) {
            extractKnownProperties(model, statement);
        }
        return model;
    }

    private void extractKnownProperties(Model model, Statement statement) throws IOException, InterruptedException {
        OntologyProperty ontologyProperty = getMappedOntologyProperty(statement);
        Value value = extractValue(ontologyProperty, statement.getObject());
        OntologyProperty mappedProperty = mapToSpecificProperty(ontologyProperty, value);
        if (nonNull(ontologyProperty)) {
            model.add(statement.getSubject(), mappedProperty.getIri(), value);
        }
    }

    private OntologyProperty mapToSpecificProperty(OntologyProperty ontologyProperty, Value value) {
        return value.toString().startsWith(DOI_PREFIX) ? Bibo.DOI : ontologyProperty;
    }

    private Value extractValue(OntologyProperty ontologyProperty, Value object) throws IOException,
                                                                                       InterruptedException {
        if (isPotentialDoiProperty(ontologyProperty) && isDoiString(object)) {
            return extractDoi(object.stringValue());
        } else {
            return object;
        }
    }

    private boolean isPotentialDoiProperty(OntologyProperty ontologyProperty) {
        return Bibo.DOI.equals(ontologyProperty) || DcTerms.IDENTIFIER.equals(ontologyProperty);
    }

    private OntologyProperty getMappedOntologyProperty(Statement statement) {
        String property = statement.getPredicate().getLocalName();
        Optional<Citation> citationValue = Citation.getTagByString(property);
        Optional<DcTerms> dcTermsValue = DcTerms.getTermByValue(property);
        Optional<RawMetaTag> rawMetaTag = RawMetaTag.getTagByString(property);

        OntologyProperty ontologyProperty = null;
        if (citationValue.isPresent()) {
            ontologyProperty = citationValue.get().getMapping();
        } else if (dcTermsValue.isPresent()) {
            ontologyProperty = dcTermsValue.get();
        } else if (rawMetaTag.isPresent()) {
            ontologyProperty = rawMetaTag.get().getMapping();
        }
        return ontologyProperty;
    }

    private IRI extractDoi(String value) throws IOException, InterruptedException {
        Optional<String> doiString = isShortDoi(value) ? fetchDoiUriFromShortDoi(value)
                                         : Optional.of(DOI_PREFIX + value.substring(value.indexOf(DOI_FIRST_PART)));
        return doiString.map(valueFactory::createIRI).orElse(null);
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

    private boolean isDoiString(Value object) {
        String value = object.stringValue();
        return value.toLowerCase(Locale.ROOT).matches(DOI_DISPLAY_REGEX) || isShortDoi(value);
    }

    private boolean isShortDoi(String value) {
        return value.matches(SHORT_DOI_REGEX);
    }

    private Optional<String> fetchPublicationIdentifierFromPublicationChannels(
        String publicationQueryTerm, int year) {
        return Optional.ofNullable(publicationQueryTerm)
            .flatMap(queryTerm -> queryPublicationChannelForJournal(queryTerm, year));
    }

    private Optional<String> queryPublicationChannelForJournal(String term, int year) {
        return attempt(() -> sendQueryToPublicationChannelsProxy(term, year))
            .map(this::parseResponseFromPublicationChannelsProxy)
            .toOptional()
            .flatMap(Function.identity());
    }

    private Optional<String> queryPublicationChannelForPublisher(String term) {
        return attempt(() -> sendQueryToPublicationChannelsProxy(term))
            .map(this::parseResponseFromPublicationChannelsProxy)
            .toOptional()
            .flatMap(Function.identity());
    }

    private Optional<String> parseResponseFromPublicationChannelsProxy(HttpResponse<String> response) {
        if (HttpURLConnection.HTTP_OK == response.statusCode()) {
            return getIdOfFirstElement(response);
        }
        return Optional.empty();
    }

    private Optional<String> getIdOfFirstElement(HttpResponse<String> response) {
        return attempt(() -> Json.readTree(response.body()))
            .map(ArrayNode.class::cast)
            .map(array -> array.get(0))
            .map(ObjectNode.class::cast)
            .map(node -> node.get("id"))
            .map(JsonNode::textValue)
            .toOptional();
    }

    private HttpResponse<String> sendQueryToPublicationChannelsProxy(String term, int year)
        throws IOException, InterruptedException {
        var searchTerm = URLEncoder.encode(term, StandardCharsets.UTF_8.toString());
        var uri = UriWrapper.fromUri(publicationChannelsHostUri)
            .addQueryParameter(QUERY_PARAM_QUERY, searchTerm)
            .addQueryParameter(QUERY_PARAM_YEAR, Integer.toString(year))
            .getUri();

        var request = HttpRequest.newBuilder()
            .uri(uri)
            .headers(HttpHeaders.ACCESS_CONTROL_ALLOW_HEADERS, MediaTypes.APPLICATION_JSON_LD.toString())
            .GET()
            .build();

        return httpClient.send(request, HttpResponse.BodyHandlers.ofString());
    }

    private HttpResponse<String> sendQueryToPublicationChannelsProxy(String term)
        throws IOException, InterruptedException {
        var searchTerm = URLEncoder.encode(term, StandardCharsets.UTF_8.toString());
        var uri = UriWrapper.fromUri(publicationChannelsHostUriPublisher)
            .addQueryParameter(QUERY_PARAM_QUERY, searchTerm)
            .getUri();

        var request = HttpRequest.newBuilder()
            .uri(uri)
            .headers(HttpHeaders.ACCESS_CONTROL_ALLOW_HEADERS, MediaTypes.APPLICATION_JSON_LD.toString())
            .GET()
            .build();

        return httpClient.send(request, HttpResponse.BodyHandlers.ofString());
    }

    /**
     * Fetches publisherId as URI from publication-channels by publisherName only with exact single match or returns
     * null.
     *
     * @param publisherName the name of the publisher
     * @return jsonString representing the publisher
     */
    public String fetchPublisherIdFromPublicationChannel(String publisherName) {
        return Optional.ofNullable(publisherName)
            .flatMap(queryTerm -> queryPublicationChannelForPublisher(queryTerm))
            .stream()
            .collect(SingletonCollector.collectOrElse(null));
    }
}
