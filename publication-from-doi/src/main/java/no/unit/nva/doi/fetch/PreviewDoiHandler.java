package no.unit.nva.doi.fetch;

import static java.util.Objects.isNull;
import static no.unit.nva.doi.fetch.RestApiConfig.restServiceObjectMapper;
import static nva.commons.core.attempt.Try.attempt;
import com.amazonaws.services.lambda.runtime.Context;
import com.fasterxml.jackson.databind.JsonNode;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import no.sikt.nva.doi.fetch.jsonconfig.Json;
import no.unit.nva.api.PublicationResponse;
import no.unit.nva.doi.DataciteContentType;
import no.unit.nva.doi.DoiProxyService;
import no.unit.nva.doi.MetadataAndContentLocation;
import no.unit.nva.doi.fetch.exceptions.CreatePublicationException;
import no.unit.nva.doi.fetch.exceptions.MalformedRequestException;
import no.unit.nva.doi.fetch.exceptions.MetadataNotFoundException;
import no.unit.nva.doi.fetch.exceptions.UnsupportedDocumentTypeException;
import no.unit.nva.doi.fetch.model.RequestBody;
import no.unit.nva.doi.fetch.model.Summary;
import no.unit.nva.doi.fetch.service.FetchDoiService;
import no.unit.nva.doi.fetch.service.IdentityUpdater;
import no.unit.nva.doi.fetch.service.PublicationConverter;
import no.unit.nva.doi.fetch.service.PublicationPersistenceService;
import no.unit.nva.doi.transformer.DoiTransformService;
import no.unit.nva.doi.transformer.utils.CristinProxyClient;
import no.unit.nva.metadata.CreatePublicationRequest;
import no.unit.nva.metadata.service.MetadataService;
import no.unit.nva.model.Publication;
import no.unit.nva.model.associatedartifacts.AssociatedArtifactList;
import no.unit.nva.model.associatedartifacts.AssociatedLink;
import no.unit.nva.model.exceptions.InvalidIsbnException;
import no.unit.nva.model.exceptions.InvalidIssnException;
import nva.commons.apigateway.ApiGatewayHandler;
import nva.commons.apigateway.RequestInfo;
import nva.commons.apigateway.exceptions.ApiGatewayException;
import nva.commons.core.Environment;
import nva.commons.core.JacocoGenerated;
import nva.commons.core.paths.UriWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PreviewDoiHandler extends ApiGatewayHandler<RequestBody, CreatePublicationRequest> {

    public static final String NULL_DOI_URL_ERROR = "doiUrl can not be null";
    private FetchDoiService fetchDoiService;

    @SuppressWarnings("unused")
    @JacocoGenerated
    public PreviewDoiHandler() {
        this(new Environment());
    }

    @JacocoGenerated
    public PreviewDoiHandler(Environment environment) {
        this(new DoiTransformService(),
             new DoiProxyService(environment), new CristinProxyClient(),
             getMetadataService(), environment);
    }

    public PreviewDoiHandler(DoiTransformService doiTransformService,
                            DoiProxyService doiProxyService,
                            CristinProxyClient cristinProxyClient,
                            MetadataService metadataService,
                            Environment environment) {
        super(RequestBody.class, environment);
        this.fetchDoiService = new FetchDoiService(doiTransformService, doiProxyService, cristinProxyClient,
                                                   metadataService);
    }

    @Override
    protected CreatePublicationRequest processInput(RequestBody input, RequestInfo requestInfo, Context context)
        throws ApiGatewayException {

        validate(input);

        var owner = requestInfo.getUserName();
        var customerId = requestInfo.getCurrentCustomer();

        var inputUri = input.getDoiUrl();
        return attempt(() -> this.fetchDoiService.newCreatePublicationRequest(owner, customerId, inputUri))
                   .orElseThrow(exception -> new RuntimeException(exception.getException()));
    }

    @Override
    protected Integer getSuccessStatusCode(RequestBody input, CreatePublicationRequest output) {
        return HttpURLConnection.HTTP_OK;
    }

    @JacocoGenerated
    private static MetadataService getMetadataService() {
        return new MetadataService();
    }

    private void validate(RequestBody input) throws MalformedRequestException {
        if (isNull(input) || isNull(input.getDoiUrl())) {
            throw new MalformedRequestException(NULL_DOI_URL_ERROR);
        }
    }

}
