package no.unit.nva.doi.fetch;

import static org.apache.hc.core5.http.HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN;
import static org.apache.hc.core5.http.HttpHeaders.CONTENT_TYPE;
import static java.net.HttpURLConnection.HTTP_BAD_GATEWAY;
import static java.net.HttpURLConnection.HTTP_BAD_REQUEST;
import static java.net.HttpURLConnection.HTTP_OK;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.core.StringContains.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import com.amazonaws.services.lambda.runtime.Context;
import com.fasterxml.jackson.core.JsonProcessingException;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import no.unit.nva.clients.cristin.CristinClient;
import no.unit.nva.doi.DoiProxyService;
import no.unit.nva.doi.fetch.commons.publication.model.CreatePublicationRequest;
import no.unit.nva.doi.fetch.exceptions.MetadataFetchException;
import no.unit.nva.doi.fetch.exceptions.MetadataNotFoundException;
import no.unit.nva.doi.transformer.utils.InvalidIssnException;
import nva.commons.apigateway.GatewayResponse;
import nva.commons.core.Environment;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class PreviewDoiHandlerTest extends DoiHandlerTestUtils {


    private Environment environment;
    private Context context;

    @BeforeEach
    void setUp() {
        environment = new Environment();
        context = getMockContext();
    }

    @Test
    void shouldReturnCreatePublicationRequestGivenValidInput()
        throws Exception {
        var importDoiHandler = createHandler(environment);
        var output = new ByteArrayOutputStream();

        importDoiHandler.handleRequest(createSampleRequest(), output, context);

        var gatewayResponse = parseSuccessResponse(output.toString());
        assertEquals(HTTP_OK, gatewayResponse.getStatusCode());
        assertThat(gatewayResponse.getHeaders(), hasKey(CONTENT_TYPE));
        assertThat(gatewayResponse.getHeaders(), hasKey(ACCESS_CONTROL_ALLOW_ORIGIN));
        var createPublicationRequest = gatewayResponse.getBodyObject(CreatePublicationRequest.class);

        var isDoi = true;
        var expectedCreateRequest = expectedCreatePublicationRequest(isDoi,
                                                                                          URI.create(VALID_DOI));

        assertEquals(createPublicationRequest, expectedCreateRequest);
    }

    @Test
    void shouldReturnBadGatewayWhenDoiProxyThrows() throws Exception {

        var handler = createHandlerWithFailingDoiProxy(environment);

        var output = new ByteArrayOutputStream();
        handler.handleRequest(createSampleRequest(), output, context);
        var gatewayResponse = parseFailureResponse(output);
        assertEquals(HTTP_BAD_GATEWAY, gatewayResponse.getStatusCode());
        assertThat(getProblemDetail(gatewayResponse), containsString("Failed to fetch metadata from URL"));
    }

    @Test
    void shouldReturnBadRequestWhenNoMetadataFound() throws Exception {
        var handler = createHandlerWithNoMetadataFound(environment);

        var output = new ByteArrayOutputStream();
        handler.handleRequest(createSampleRequest(), output, context);
        var gatewayResponse = parseFailureResponse(output);
        assertEquals(HTTP_BAD_REQUEST, gatewayResponse.getStatusCode());
    }

    @Test
    void shouldReturnMalformedRequestExceptionWhenInputIsNull() throws Exception {

        var importDoiHandler = createHandler(environment);
        var output = new ByteArrayOutputStream();
        importDoiHandler.handleRequest(malformedInputStream(), output, context);
        var gatewayResponse = parseSuccessResponse(output.toString());
        assertEquals(HTTP_BAD_REQUEST, gatewayResponse.getStatusCode());

    }

    GatewayResponse<CreatePublicationRequest> parseSuccessResponse(String output) throws JsonProcessingException {
        return parseGatewayResponse(output, CreatePublicationRequest.class);
    }

    PreviewDoiHandler createHandler(Environment environment)
        throws URISyntaxException, IOException, InvalidIssnException, MetadataNotFoundException,
               MetadataFetchException {
        var doiTransformService = mockDoiTransformServiceReturningSuccessfulResult();
        var doiProxyService = mockDoiProxyServiceReceivingSuccessfulResult();
        var cristinClient = mock(CristinClient.class);
        var metadataService = mockMetadataServiceReturningSuccessfulResult();

        return new PreviewDoiHandler(doiTransformService, doiProxyService, cristinClient, metadataService, environment);
    }

    PreviewDoiHandler createHandlerWithNoMetadataFound(Environment environment)
        throws URISyntaxException, IOException, InvalidIssnException, MetadataNotFoundException,
               MetadataFetchException {
        var doiProxyService = mock(DoiProxyService.class);
        when(doiProxyService.lookupDoiMetadata(anyString(), any()))
            .thenThrow(new MetadataNotFoundException("No metadata found"));

        var doiTransformService = mockDoiTransformServiceReturningSuccessfulResult();
        var cristinClient = mock(CristinClient.class);
        var metadataService = mockMetadataServiceReturningSuccessfulResult();

        return new PreviewDoiHandler(doiTransformService, doiProxyService, cristinClient, metadataService, environment);
    }

    PreviewDoiHandler createHandlerWithFailingDoiProxy(Environment environment)
        throws URISyntaxException, IOException, InvalidIssnException, MetadataNotFoundException,
               MetadataFetchException {
        var doiProxyService = mock(DoiProxyService.class);
        when(doiProxyService.lookupDoiMetadata(anyString(), any())).thenThrow(new IOException(""));

        var doiTransformService = mockDoiTransformServiceReturningSuccessfulResult();
        var cristinClient = mock(CristinClient.class);
        var metadataService = mockMetadataServiceReturningSuccessfulResult();

        return new PreviewDoiHandler(doiTransformService, doiProxyService, cristinClient, metadataService, environment);
    }
}
