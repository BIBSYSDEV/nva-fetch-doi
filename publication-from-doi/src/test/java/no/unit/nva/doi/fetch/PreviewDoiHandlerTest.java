package no.unit.nva.doi.fetch;

import static com.google.common.net.HttpHeaders.CONTENT_TYPE;
import static java.net.HttpURLConnection.HTTP_BAD_REQUEST;
import static java.net.HttpURLConnection.HTTP_INTERNAL_ERROR;
import static java.net.HttpURLConnection.HTTP_OK;
import static nva.commons.apigateway.ApiGatewayHandler.MESSAGE_FOR_RUNTIME_EXCEPTIONS_HIDING_IMPLEMENTATION_DETAILS_TO_API_CLIENTS;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.StringContains.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import com.amazonaws.services.lambda.runtime.Context;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.common.net.HttpHeaders;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import no.unit.nva.doi.DoiProxyService;
import no.unit.nva.doi.fetch.exceptions.MetadataNotFoundException;
import no.unit.nva.doi.fetch.exceptions.UnsupportedDocumentTypeException;
import no.unit.nva.doi.fetch.model.PublicationDate;
import no.unit.nva.doi.fetch.model.Summary;
import no.unit.nva.doi.fetch.service.PublicationConverter;
import no.unit.nva.doi.fetch.service.PublicationPersistenceService;
import no.unit.nva.doi.transformer.DoiTransformService;
import no.unit.nva.doi.transformer.utils.CristinProxyClient;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.metadata.CreatePublicationRequest;
import no.unit.nva.metadata.service.MetadataService;
import no.unit.nva.model.exceptions.InvalidIsbnException;
import no.unit.nva.model.exceptions.InvalidIssnException;
import nva.commons.apigateway.ApiGatewayHandler;
import nva.commons.apigateway.GatewayResponse;
import nva.commons.apigateway.exceptions.NotFoundException;
import nva.commons.core.Environment;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.zalando.problem.Problem;

class PreviewDoiHandlerTest extends DoiHandlerTestUtils {


    private Environment environment;
    private Context context;
    private ByteArrayOutputStream output;
    private PublicationPersistenceService publicationPersistenceService;

    @BeforeEach
    public void setUp() {
        environment = mock(Environment.class);
        context = getMockContext();
        output = new ByteArrayOutputStream();
        publicationPersistenceService = mock(PublicationPersistenceService.class);

        when(environment.readEnv(ApiGatewayHandler.ALLOWED_ORIGIN_ENV)).thenReturn(ALL_ORIGINS);
        when(environment.readEnv(ImportDoiHandler.PUBLICATION_API_HOST_ENV)).thenReturn("localhost");
    }

    @Test
    public void shouldReturnCreatePublicationRequestGivenValidInput()
        throws Exception {
        PreviewDoiHandler importDoiHandler = createHandler(environment);
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        importDoiHandler.handleRequest(createSampleRequest(), output, context);
        GatewayResponse<CreatePublicationRequest> gatewayResponse = parseSuccessResponse(output.toString());
        assertEquals(HTTP_OK, gatewayResponse.getStatusCode());
        assertThat(gatewayResponse.getHeaders(), hasKey(CONTENT_TYPE));
        assertThat(gatewayResponse.getHeaders(), hasKey(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN));
        CreatePublicationRequest createPublicationRequest = gatewayResponse.getBodyObject(CreatePublicationRequest.class);

        var isDoi = true;
        CreatePublicationRequest expectedCreateRequest = expectedCreatePublicationRequest(isDoi,
                                                                                          URI.create(VALID_DOI));

        assertEquals(createPublicationRequest, expectedCreateRequest);
    }

    @Test
    public void shouldReturnInternalServerErrorWhenDoiProxyThrows() throws Exception {

        var handler = createHandlerWithFailingDoiProxy(environment);

        ByteArrayOutputStream output = new ByteArrayOutputStream();
        handler.handleRequest(createSampleRequest(), output, context);
        GatewayResponse<Problem> gatewayResponse = parseFailureResponse(output);
        assertEquals(HTTP_INTERNAL_ERROR, gatewayResponse.getStatusCode());
        assertThat(getProblemDetail(gatewayResponse), containsString(
            MESSAGE_FOR_RUNTIME_EXCEPTIONS_HIDING_IMPLEMENTATION_DETAILS_TO_API_CLIENTS));
    }

    @Test
    public void shouldReturnNotFoundWhenDoiProxyThrows() throws Exception {

        var handler = createHandlerWithFailingDoiProxy(environment);

        ByteArrayOutputStream output = new ByteArrayOutputStream();
        handler.handleRequest(createSampleRequest(), output, context);
        GatewayResponse<Problem> gatewayResponse = parseFailureResponse(output);
        assertEquals(HTTP_INTERNAL_ERROR, gatewayResponse.getStatusCode());
        assertThat(getProblemDetail(gatewayResponse), containsString(
            MESSAGE_FOR_RUNTIME_EXCEPTIONS_HIDING_IMPLEMENTATION_DETAILS_TO_API_CLIENTS));
    }

    @Test
    public void shouldReturnMalformedRequestExceptionWhenInputIsNull() throws Exception {

        PreviewDoiHandler importDoiHandler = createHandler(environment);
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        importDoiHandler.handleRequest(malformedInputStream(), output, context);
        GatewayResponse<CreatePublicationRequest> gatewayResponse = parseSuccessResponse(output.toString());
        assertEquals(HTTP_BAD_REQUEST, gatewayResponse.getStatusCode());

    }

    GatewayResponse<CreatePublicationRequest> parseSuccessResponse(String output) throws JsonProcessingException {
        return parseGatewayResponse(output, CreatePublicationRequest.class);
    }

    PreviewDoiHandler createHandler(Environment environment)
        throws URISyntaxException, IOException, InvalidIssnException,
               MetadataNotFoundException, InvalidIsbnException, UnsupportedDocumentTypeException {
        DoiTransformService doiTransformService = mockDoiTransformServiceReturningSuccessfulResult();
        DoiProxyService doiProxyService = mockDoiProxyServiceReceivingSuccessfulResult();
        CristinProxyClient cristinProxyClient = mock(CristinProxyClient.class);
        MetadataService metadataService = mockMetadataServiceReturningSuccessfulResult();

        return new PreviewDoiHandler(doiTransformService, doiProxyService, cristinProxyClient, metadataService, environment);
    }

    PreviewDoiHandler createHandlerWithFailingDoiProxy(Environment environment)
        throws URISyntaxException, IOException, InvalidIssnException,
               MetadataNotFoundException, InvalidIsbnException, UnsupportedDocumentTypeException {
        DoiProxyService doiProxyService = mock(DoiProxyService.class);
        when(doiProxyService.lookupDoiMetadata(anyString(), any())).thenThrow(new IOException(""));

        DoiTransformService doiTransformService = mockDoiTransformServiceReturningSuccessfulResult();
        CristinProxyClient cristinProxyClient = mock(CristinProxyClient.class);
        MetadataService metadataService = mockMetadataServiceReturningSuccessfulResult();

        return new PreviewDoiHandler(doiTransformService, doiProxyService, cristinProxyClient, metadataService, environment);
    }




}
