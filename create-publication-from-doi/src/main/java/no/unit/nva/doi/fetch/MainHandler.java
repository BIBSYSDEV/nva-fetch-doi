package no.unit.nva.doi.fetch;

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
import no.unit.nva.doi.fetch.utils.JacocoGenerated;
import no.unit.nva.doi.transformer.DoiTransformService;
import no.unit.nva.doi.transformer.exception.MissingClaimException;
import no.unit.nva.model.Publication;
import no.unit.nva.model.exceptions.InvalidIssnException;
import no.unit.nva.model.exceptions.InvalidPageTypeException;
import nva.commons.exceptions.ApiGatewayException;
import nva.commons.handlers.ApiGatewayHandler;
import nva.commons.handlers.RequestInfo;
import nva.commons.utils.Environment;
import nva.commons.utils.RequestUtils;
import org.apache.http.HttpHeaders;
import org.apache.http.client.utils.URIBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MainHandler extends ApiGatewayHandler<RequestBody,Summary> {

    public static final String API_HOST_ENV = "API_HOST";
    public static final String API_SCHEME_ENV = "API_SCHEME";

    public static final JsonPointer FEIDE_ID = JsonPointer.compile("/authorizer/claims/custom:feideId");
    public static final JsonPointer ORG_NUMBER = JsonPointer.compile("/authorizer/claims/custom:orgNumber");

    private final transient ObjectMapper objectMapper;
    private final transient PublicationConverter publicationConverter;
    private final transient DoiTransformService doiTransformService;
    private final transient DoiProxyService doiProxyService;
    private final transient PublicationPersistenceService publicationPersistenceService;
    private final transient String apiHost;
    private final transient String apiScheme;
    public static final ObjectMapper jsonParser = ObjectMapperConfig.createObjectMapper();

    private static final Logger logger = LoggerFactory.getLogger(MainHandler.class);

    @JacocoGenerated
    public MainHandler() {
        this(jsonParser, new PublicationConverter(), new DoiTransformService(),
            new DoiProxyService(), new PublicationPersistenceService(), new Environment());
    }

    /**
     * Constructor for MainHandler.
     *
     * @param objectMapper objectMapper.
     * @param environment  environment.
     */
    public MainHandler(ObjectMapper objectMapper, PublicationConverter publicationConverter,
                       DoiTransformService doiTransformService, DoiProxyService doiProxyService,
                       PublicationPersistenceService publicationPersistenceService, Environment environment) {
        super(RequestBody.class, environment, logger);
        this.objectMapper = objectMapper;
        this.publicationConverter = publicationConverter;
        this.doiTransformService = doiTransformService;
        this.doiProxyService = doiProxyService;
        this.publicationPersistenceService = publicationPersistenceService;
        this.apiHost = environment.readEnv(API_HOST_ENV);
        this.apiScheme = environment.readEnv(API_SCHEME_ENV);
    }

    @Override
    protected Summary processInput(RequestBody input, RequestInfo requestInfo, Context context)
        throws ApiGatewayException {

        URI apiUrl = urlToPublicationProxy();

        validate(input);

        String owner = RequestUtils.getRequestContextParameter(requestInfo, FEIDE_ID);
        String orgNumber = RequestUtils.getRequestContextParameter(requestInfo, ORG_NUMBER);
        String authorization = RequestUtils.getHeader(requestInfo, HttpHeaders.AUTHORIZATION);

        try {
            Publication publication = getPublicationMetadata(input, owner, orgNumber);
            PublicationResponse publicationResponse = tryInsertPublication(authorization, apiUrl, publication);
            return publicationConverter
                .toSummary(objectMapper.convertValue(publicationResponse, JsonNode.class));
        } catch (URISyntaxException
            | IOException
            | InvalidIssnException
            | InvalidPageTypeException
            | InterruptedException e) {
            throw new TransformFailedException(e.getMessage());
        }
    }

    private URI urlToPublicationProxy() {
        return attempt(()-> new URIBuilder().setHost(apiHost).setScheme(apiScheme).build())
            .orElseThrow(failure-> new IllegalStateException(failure.getException()));
    }

    private void validate(RequestBody input) throws MalformedRequestException {
        if (input == null || input.getDoiUrl() == null) {
            throw new MalformedRequestException("doiUrl can not be null");
        }
    }

    @Override
    protected Integer getSuccessStatusCode(RequestBody input, Summary output) {
        return SC_OK;
    }

    private Publication getPublicationMetadata(RequestBody requestBody,
                                            String owner, String orgNumber)
        throws URISyntaxException, IOException, MissingClaimException, InvalidIssnException,
               InvalidPageTypeException, MetadataNotFoundException {
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
