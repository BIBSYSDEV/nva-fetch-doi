package no.unit.nva.doi.fetch;

import static java.util.Objects.isNull;
import static nva.commons.core.attempt.Try.attempt;
import com.amazonaws.services.lambda.runtime.Context;
import com.fasterxml.jackson.databind.JsonNode;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URI;
import no.sikt.nva.doi.fetch.jsonconfig.Json;
import no.unit.nva.clients.cristin.CristinClient;
import no.unit.nva.doi.DoiProxyService;
import no.unit.nva.doi.fetch.commons.publication.model.CreatePublicationRequest;
import no.unit.nva.doi.fetch.commons.publication.model.PublicationResponse;
import no.unit.nva.doi.fetch.exceptions.CreatePublicationException;
import no.unit.nva.doi.fetch.exceptions.MalformedRequestException;
import no.unit.nva.doi.fetch.model.RequestBody;
import no.unit.nva.doi.fetch.model.Summary;
import no.unit.nva.doi.fetch.service.FetchDoiService;
import no.unit.nva.doi.fetch.service.PublicationConverter;
import no.unit.nva.doi.fetch.service.PublicationPersistenceService;
import no.unit.nva.doi.transformer.DoiTransformService;
import no.unit.nva.metadata.service.MetadataService;
import nva.commons.apigateway.ApiGatewayHandler;
import nva.commons.apigateway.RequestInfo;
import nva.commons.apigateway.exceptions.ApiGatewayException;
import nva.commons.core.Environment;
import nva.commons.core.JacocoGenerated;
import nva.commons.core.paths.UriWrapper;

public class ImportDoiHandler extends ApiGatewayHandler<RequestBody, Summary> {

    public static final String PUBLICATION_API_HOST_ENV = "PUBLICATION_API_HOST";
    public static final String NULL_DOI_URL_ERROR = "doiUrl can not be null";
    private final transient PublicationConverter publicationConverter;
    private final transient PublicationPersistenceService publicationPersistenceService;
    private final transient String publicationApiHost;
    private final FetchDoiService fetchDoiService;

    @SuppressWarnings("unused")
    @JacocoGenerated
    public ImportDoiHandler() {
        this(new Environment());
    }

    @JacocoGenerated
    public ImportDoiHandler(Environment environment) {
        this(new PublicationConverter(), new DoiTransformService(),
             new DoiProxyService(environment), new PublicationPersistenceService(), CristinClient.defaultClient(),
             getMetadataService(), environment);
    }

    public ImportDoiHandler(PublicationConverter publicationConverter,
                            DoiTransformService doiTransformService,
                            DoiProxyService doiProxyService,
                            PublicationPersistenceService publicationPersistenceService,
                            CristinClient cristinClient,
                            MetadataService metadataService,
                            Environment environment) {
        super(RequestBody.class, environment);
        this.publicationConverter = publicationConverter;
        this.publicationPersistenceService = publicationPersistenceService;

        this.publicationApiHost = environment.readEnv(PUBLICATION_API_HOST_ENV);
        this.fetchDoiService = new FetchDoiService(doiTransformService, doiProxyService, cristinClient,
                                                   metadataService);
    }

    @Override
    protected void validateRequest(RequestBody requestBody, RequestInfo requestInfo, Context context)
        throws ApiGatewayException {
        validate(requestBody);
    }

    @Override
    protected Summary processInput(RequestBody input, RequestInfo requestInfo, Context context)
        throws ApiGatewayException {

        var apiUrl = urlToPublicationProxy();


        var authHeader = requestInfo.getAuthHeader();

        var inputUri = input.getDoiUrl();
        return attempt(() -> this.fetchDoiService.newCreatePublicationRequest(inputUri))
                   .map(createPublicationRequest -> tryCreatePublication(authHeader, apiUrl, createPublicationRequest))
                   .map(response -> Json.convertValue(response, JsonNode.class))
                   .map(publicationConverter::toSummary)
                   .orElseThrow(fail -> handleError(fail.getException()));
    }

    @Override
    protected Integer getSuccessStatusCode(RequestBody input, Summary output) {
        return HttpURLConnection.HTTP_OK;
    }

    @JacocoGenerated
    private static MetadataService getMetadataService() {
        return new MetadataService();
    }

    @JacocoGenerated
    private ApiGatewayException handleError(Exception exception) {
        if (exception instanceof ApiGatewayException) {
            return (ApiGatewayException) exception;
        }
        if (exception instanceof RuntimeException) {
            throw (RuntimeException) exception;
        }
        throw new RuntimeException(exception);
    }

    private URI urlToPublicationProxy() {
        return attempt(() -> UriWrapper.fromHost(publicationApiHost).getUri())
                   .orElseThrow(failure -> new IllegalStateException(failure.getException()));
    }

    private void validate(RequestBody input) throws MalformedRequestException {
        if (isNull(input) || isNull(input.getDoiUrl())) {
            throw new MalformedRequestException(NULL_DOI_URL_ERROR);
        }
    }

    private PublicationResponse tryCreatePublication(String authorization, URI apiUrl, CreatePublicationRequest request)
        throws InterruptedException, IOException, CreatePublicationException {
        return createPublication(authorization, apiUrl, request);
    }

    private PublicationResponse createPublication(String authorization, URI apiUrl, CreatePublicationRequest request)
        throws InterruptedException, CreatePublicationException, IOException {
        return publicationPersistenceService.createPublication(request, apiUrl, authorization);
    }
}
