package no.unit.nva.doi.fetch;

import static java.util.Objects.isNull;
import static nva.commons.utils.attempt.Try.attempt;
import static org.apache.http.HttpStatus.SC_OK;

import com.amazonaws.services.lambda.runtime.Context;
import com.fasterxml.jackson.core.JsonPointer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import no.unit.nva.api.PublicationResponse;
import no.unit.nva.doi.DataciteContentType;
import no.unit.nva.doi.DoiProxyService;
import no.unit.nva.doi.MetadataAndContentLocation;
import no.unit.nva.doi.fetch.exceptions.InsertPublicationException;
import no.unit.nva.doi.fetch.exceptions.MalformedRequestException;
import no.unit.nva.doi.fetch.exceptions.MetadataNotFoundException;
import no.unit.nva.doi.fetch.exceptions.TransformFailedException;
import no.unit.nva.doi.fetch.model.RequestBody;
import no.unit.nva.doi.fetch.model.Summary;
import no.unit.nva.doi.fetch.service.PublicationConverter;
import no.unit.nva.doi.fetch.service.PublicationPersistenceService;
import no.unit.nva.doi.transformer.DoiTransformService;
import no.unit.nva.model.Publication;
import no.unit.nva.model.exceptions.InvalidIssnException;
import nva.commons.exceptions.ApiGatewayException;
import nva.commons.handlers.ApiGatewayHandler;
import nva.commons.handlers.RequestInfo;
import nva.commons.utils.Environment;
import nva.commons.utils.JacocoGenerated;
import nva.commons.utils.JsonUtils;
import nva.commons.utils.RequestUtils;
import org.apache.http.HttpHeaders;
import org.apache.http.client.utils.URIBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MainHandler extends ApiGatewayHandler<RequestBody, Summary> {

    public static final String PUBLICATION_API_HOST_ENV = "PUBLICATION_API_HOST";
    public static final String PUBLICATION_API_SCHEME_ENV = "PUBLICATION_API_SCHEME";

    public static final JsonPointer FEIDE_ID = JsonPointer.compile("/authorizer/claims/custom:feideId");
    public static final JsonPointer ORG_NUMBER = JsonPointer.compile("/authorizer/claims/custom:orgNumber");
    public static final String NULL_DOI_URL_ERROR = "doiUrl can not be null";

    private final ObjectMapper objectMapper;
    private final transient PublicationConverter publicationConverter;
    private final transient DoiTransformService doiTransformService;
    private final transient DoiProxyService doiProxyService;
    private final transient PublicationPersistenceService publicationPersistenceService;
    private final transient String publicationApiHost;
    private final transient String publicationApiScheme;

    private static final Logger logger = LoggerFactory.getLogger(MainHandler.class);

    @JacocoGenerated
    public MainHandler() {
        this(JsonUtils.objectMapper, new PublicationConverter(), new DoiTransformService(),
            new DoiProxyService(), new PublicationPersistenceService(), new Environment());
    }

    /**
     * Constructor for MainHandler.
     *
     * @param objectMapper objectMapper.
     * @param environment  environment.
     */
    public MainHandler(ObjectMapper objectMapper,
                       PublicationConverter publicationConverter,
                       DoiTransformService doiTransformService,
                       DoiProxyService doiProxyService,
                       PublicationPersistenceService publicationPersistenceService,
                       Environment environment) {
        super(RequestBody.class, environment, logger);
        this.objectMapper = objectMapper;
        this.publicationConverter = publicationConverter;
        this.doiTransformService = doiTransformService;
        this.doiProxyService = doiProxyService;
        this.publicationPersistenceService = publicationPersistenceService;
        this.publicationApiHost = environment.readEnv(PUBLICATION_API_HOST_ENV);
        this.publicationApiScheme = environment.readEnv(PUBLICATION_API_SCHEME_ENV);
    }

    @Override
    protected Summary processInput(RequestBody input, RequestInfo requestInfo, Context context)
        throws ApiGatewayException {

        URI apiUrl = urlToPublicationProxy();
        validate(input);

        String owner = RequestUtils.getRequestContextParameter(requestInfo, FEIDE_ID);
        String orgNumber = RequestUtils.getRequestContextParameter(requestInfo, ORG_NUMBER);
        String authorization = RequestUtils.getHeader(requestInfo, HttpHeaders.AUTHORIZATION);
        boolean interrupted = false;
        try {
            Publication publication = getPublicationMetadata(input, owner, orgNumber);
            PublicationResponse publicationResponse = tryInsertPublication(authorization, apiUrl, publication);
            return publicationConverter
                .toSummary(objectMapper.convertValue(publicationResponse, JsonNode.class));
        } catch (URISyntaxException
            | IOException
            | InvalidIssnException e) {
            throw new TransformFailedException(e.getMessage());
        } catch (InterruptedException e) {
            interrupted = true;
            throw new TransformFailedException(e.getMessage());
        } finally {
            if (interrupted) {
                Thread.currentThread().interrupt();
            }
        }
    }

    private URI urlToPublicationProxy() {
        return attempt(() -> new URIBuilder().setHost(publicationApiHost).setScheme(publicationApiScheme).build())
            .orElseThrow(failure -> new IllegalStateException(failure.getException()));
    }

    private void validate(RequestBody input) throws MalformedRequestException {
        if (isNull(input) || isNull(input.getDoiUrl())) {
            throw new MalformedRequestException(NULL_DOI_URL_ERROR);
        }
    }

    @Override
    protected Integer getSuccessStatusCode(RequestBody input, Summary output) {
        return SC_OK;
    }

    private Publication getPublicationMetadata(RequestBody requestBody,
                                               String owner, String orgNumber)
        throws URISyntaxException, IOException, InvalidIssnException, MetadataNotFoundException {
        MetadataAndContentLocation metadataAndContentLocation = doiProxyService.lookupDoiMetadata(
            requestBody.getDoiUrl().toString(), DataciteContentType.DATACITE_JSON);

        return doiTransformService.transformPublication(
            metadataAndContentLocation.getJson(),
            metadataAndContentLocation.getContentHeader(), owner, orgNumber);
    }

    private PublicationResponse tryInsertPublication(String authorization, URI apiUrl, Publication publication)
        throws InterruptedException, IOException, InsertPublicationException, URISyntaxException {
        return insertPublication(authorization, apiUrl, publication);
    }

    private PublicationResponse insertPublication(String authorization, URI apiUrl, Publication publication)
        throws InterruptedException, InsertPublicationException, IOException, URISyntaxException {
        return publicationPersistenceService.insertPublication(publication, apiUrl, authorization);
    }
}
