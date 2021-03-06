package no.unit.nva.metadata.service;

import no.unit.nva.api.CreatePublicationRequest;
import no.unit.nva.metadata.MetadataConverter;
import no.unit.nva.metadata.type.Bibo;
import no.unit.nva.metadata.type.Citation;
import no.unit.nva.metadata.type.DcTerms;
import no.unit.nva.metadata.type.OntologyProperty;
import no.unit.nva.metadata.type.RawMetaTag;
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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Locale;
import java.util.Optional;

import static java.util.Objects.nonNull;

public class MetadataService {

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
    public static final ValueFactory valueFactory = SimpleValueFactory.getInstance();
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
    public Optional<CreatePublicationRequest> generateCreatePublicationRequest(URI uri) {
        try {
            Model metadata = getMetadata(uri);
            MetadataConverter converter = new MetadataConverter(metadata);
            return converter.generateCreatePublicationRequest();
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

}
