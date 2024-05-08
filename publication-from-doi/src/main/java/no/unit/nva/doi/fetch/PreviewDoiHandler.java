package no.unit.nva.doi.fetch;

import static java.util.Objects.isNull;
import static nva.commons.core.attempt.Try.attempt;
import com.amazonaws.services.lambda.runtime.Context;
import java.net.HttpURLConnection;
import no.unit.nva.doi.DoiProxyService;
import no.unit.nva.doi.fetch.exceptions.MalformedRequestException;
import no.unit.nva.doi.fetch.model.RequestBody;
import no.unit.nva.doi.fetch.service.FetchDoiService;
import no.unit.nva.doi.transformer.DoiTransformService;
import no.unit.nva.doi.transformer.utils.CristinProxyClient;
import no.unit.nva.metadata.CreatePublicationRequest;
import no.unit.nva.metadata.service.MetadataService;
import nva.commons.apigateway.ApiGatewayHandler;
import nva.commons.apigateway.RequestInfo;
import nva.commons.apigateway.exceptions.ApiGatewayException;
import nva.commons.core.Environment;
import nva.commons.core.JacocoGenerated;

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
