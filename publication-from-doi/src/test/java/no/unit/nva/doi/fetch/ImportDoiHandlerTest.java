package no.unit.nva.doi.fetch;

import static org.apache.hc.core5.http.HttpHeaders.CONTENT_TYPE;
import static java.net.HttpURLConnection.HTTP_BAD_REQUEST;
import static java.net.HttpURLConnection.HTTP_INTERNAL_ERROR;
import static java.net.HttpURLConnection.HTTP_OK;
import static nva.commons.apigateway.ApiGatewayHandler.MESSAGE_FOR_RUNTIME_EXCEPTIONS_HIDING_IMPLEMENTATION_DETAILS_TO_API_CLIENTS;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.hamcrest.core.StringContains.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import com.amazonaws.services.lambda.runtime.Context;
import com.fasterxml.jackson.core.JsonProcessingException;
import org.apache.hc.core5.http.HttpHeaders;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandler;
import java.util.Optional;
import no.unit.nva.clients.cristin.CristinClient;
import no.unit.nva.doi.CrossRefClient;
import no.unit.nva.doi.DataciteClient;
import no.unit.nva.doi.DoiProxyService;
import no.unit.nva.doi.fetch.exceptions.MetadataFetchException;
import no.unit.nva.doi.fetch.exceptions.MetadataNotFoundException;
import no.unit.nva.doi.fetch.model.PublicationDate;
import no.unit.nva.doi.fetch.model.Summary;
import no.unit.nva.doi.fetch.service.PublicationConverter;
import no.unit.nva.doi.fetch.service.PublicationPersistenceService;
import no.unit.nva.doi.transformer.DoiTransformService;
import no.unit.nva.doi.transformer.utils.InvalidIssnException;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.metadata.service.MetadataService;
import nva.commons.apigateway.GatewayResponse;
import nva.commons.core.Environment;
import nva.commons.logutils.LogRecorder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.zalando.problem.Problem;
import org.zalando.problem.Status;

class ImportDoiHandlerTest extends DoiHandlerTestUtils {

    private Environment environment;
    private Context context;
    private ByteArrayOutputStream output;
    private PublicationPersistenceService publicationPersistenceService;
    public static final String NO_METADATA_FOUND = "No metadata found for imported uri";


    @BeforeEach
    void setUp() {
        environment = new Environment();
        context = getMockContext();
        output = new ByteArrayOutputStream();
        publicationPersistenceService = mock(PublicationPersistenceService.class);
    }

    @Test
    void testOkResponse()
        throws Exception {
        var importDoiHandler = this.createImportHandler(environment);
        var output = new ByteArrayOutputStream();
        importDoiHandler.handleRequest(createSampleRequest(), output, context);
        var gatewayResponse = parseSuccessResponse(output.toString());
        assertEquals(HTTP_OK, gatewayResponse.getStatusCode());
        assertThat(gatewayResponse.getHeaders(), hasKey(CONTENT_TYPE));
        assertThat(gatewayResponse.getHeaders(), hasKey(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN));
        var summary = gatewayResponse.getBodyObject(Summary.class);
        assertNotNull(summary.getIdentifier());

        var isDoi = true;
        var expectedCreateRequest = expectedCreatePublicationRequest(isDoi,
                                                                                          URI.create(VALID_DOI));

        verify(publicationPersistenceService, times(1))
            .createPublication(eq(expectedCreateRequest), any(), any());
    }

    @Test
    void handleRequestReturnsSummaryWithIdentifierWhenUrlIsValidNonDoi()
        throws Exception {
        ImportDoiHandler importDoiHandler = this.createImportHandler(environment);
        var output = new ByteArrayOutputStream();
        importDoiHandler.handleRequest(nonDoiUrlInputStream(), output, context);
        var gatewayResponse = parseSuccessResponse(output.toString());
        assertEquals(HTTP_OK, gatewayResponse.getStatusCode());
        assertThat(gatewayResponse.getHeaders(), hasKey(CONTENT_TYPE));
        assertThat(gatewayResponse.getHeaders(), hasKey(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN));
        var summary = gatewayResponse.getBodyObject(Summary.class);
        assertNotNull(summary.getIdentifier());

        var isDoi = false;
        var expectedCreateRequest = expectedCreatePublicationRequest(isDoi,
                                                                                          URI.create(VALID_NON_DOI));

        verify(publicationPersistenceService, times(1))
            .createPublication(eq(expectedCreateRequest), any(), any());
    }

    @Test
    void testBadRequestResponseWhenUrlIsInvalidNonDoi() throws Exception {
        var publicationConverter = mock(PublicationConverter.class);

        var importDoiHandler = handlerReceivingEmptyResponse(publicationConverter);
        importDoiHandler.handleRequest(nonDoiUrlInputStream(), output, context);
        var gatewayResponse = parseFailureResponse(output);
        assertEquals(HTTP_BAD_REQUEST, gatewayResponse.getStatusCode());
        assertThat(getProblemDetail(gatewayResponse), containsString(NO_METADATA_FOUND));
    }

    @Test
    void shouldReturnInternalErrorWhenUrlToPublicationProxyIsNotValidAndContainInformativeMessage()
        throws IOException, InvalidIssnException, URISyntaxException,
               MetadataNotFoundException, MetadataFetchException {

        var logRecorder = LogRecorder.forRoot(ImportDoiHandler.class);
        var environmentWithInvalidHost = createEnvironmentWithInvalidHost();
        var importDoiHandler = this.createImportHandler(environmentWithInvalidHost);

        importDoiHandler.handleRequest(createSampleRequest(), output, context);
        var response = GatewayResponse.fromOutputStream(output, Problem.class);
        assertThat(response.getStatusCode(), is(equalTo(HTTP_INTERNAL_ERROR)));
        assertThat(logRecorder.messages(), hasItem(containsString("Missing host for creating URI")));
    }

    @Test
    void testBadRequestResponse() throws Exception {
        var publicationConverter = mock(PublicationConverter.class);
        var doiTransformService = mockDoiTransformServiceReturningSuccessfulResult();
        var doiProxyService = mock(DoiProxyService.class);
        var publicationPersistenceService = mock(PublicationPersistenceService.class);
        var cristinClient = mock(CristinClient.class);
        var metadataService = mock(MetadataService.class);
        var importDoiHandler = new ImportDoiHandler(publicationConverter, doiTransformService,
                                                                 doiProxyService, publicationPersistenceService, cristinClient,
                                                                 metadataService, environment);
        var output = new ByteArrayOutputStream();
        importDoiHandler.handleRequest(malformedInputStream(), output, context);
        var gatewayResponse = parseFailureResponse(output);
        assertEquals(HTTP_BAD_REQUEST, gatewayResponse.getStatusCode());
        assertThat(getProblemDetail(gatewayResponse), containsString(ImportDoiHandler.NULL_DOI_URL_ERROR));
    }

    @Test
    void testInternalServerErrorResponse() throws Exception {
        var publicationConverter = mock(PublicationConverter.class);
        when(publicationConverter.toSummary(any())).thenThrow(new RuntimeException(SOME_ERROR_MESSAGE));
        var doiTransformService = mockDoiTransformServiceReturningSuccessfulResult();
        var doiProxyService = mockDoiProxyServiceReceivingSuccessfulResult();
        var publicationPersistenceService = mock(PublicationPersistenceService.class);
        var cristinProxyClient = mock(CristinClient.class);
        var metadataService = mock(MetadataService.class);

        var importDoiHandler = new ImportDoiHandler(publicationConverter, doiTransformService,
                                                                 doiProxyService, publicationPersistenceService, cristinProxyClient,
                                                                 metadataService, environment);
        var output = new ByteArrayOutputStream();
        importDoiHandler.handleRequest(createSampleRequest(), output, context);
        var gatewayResponse = parseFailureResponse(output);
        assertEquals(HTTP_INTERNAL_ERROR, gatewayResponse.getStatusCode());
        assertThat(getProblemDetail(gatewayResponse), containsString(
            MESSAGE_FOR_RUNTIME_EXCEPTIONS_HIDING_IMPLEMENTATION_DETAILS_TO_API_CLIENTS));
    }

    @Test
    @DisplayName("handler returns BadRequest when DoiProxyService returns no metadata")
    void handlerReturnsBadRequestWhenDoiProxyServiceReturnsNoMetadata()
        throws Exception {

        var publicationConverter = mockPublicationConverter();
        var doiTransformService = mockDoiTransformServiceReturningSuccessfulResult();
        var doiProxyService = mockDoiProxyReceivingFailedResult();
        var publicationPersistenceService = mock(PublicationPersistenceService.class);
        var cristinProxyClient = mock(CristinClient.class);
        var metadataService = mock(MetadataService.class);

        var handler = new ImportDoiHandler(publicationConverter, doiTransformService, doiProxyService,
                                                        publicationPersistenceService, cristinProxyClient, metadataService,
                                                        environment);
        var outputStream = outputStream();
        handler.handleRequest(createSampleRequest(), outputStream, context);
        var gatewayResponse = parseFailureResponse(outputStream);
        assertThat(gatewayResponse.getStatusCode(), is(equalTo(Status.BAD_REQUEST.getStatusCode())));
        assertThat(getProblemDetail(gatewayResponse), containsString(DoiProxyService.ERROR_READING_METADATA));
    }

    @Test
    @DisplayName("handler returns BadGateway when ResourcePersistenceService returns failed response")
    void handlerReturnsBadGatewayErrorWhenResourcePersistenceServiceReturnsFailedResponse()
        throws Exception {

        var publicationConverter = mockPublicationConverter();
        var doiProxyService = mockDoiProxyServiceReceivingSuccessfulResult();
        var doiTransformService = mockDoiTransformServiceReturningSuccessfulResult();
        var cristinProxyClient = mock(CristinClient.class);
        var metadataService = mock(MetadataService.class);

        var publicationPersistenceService =
            mockResourcePersistenceServiceReceivingFailedResult();

        var handler = new ImportDoiHandler(publicationConverter, doiTransformService, doiProxyService,
                                                        publicationPersistenceService, cristinProxyClient, metadataService,
                                                        environment);
        var outputStream = outputStream();
        handler.handleRequest(createSampleRequest(), outputStream, context);
        var gatewayResponse = parseFailureResponse(outputStream);
        assertThat(gatewayResponse.getStatusCode(), is(equalTo(Status.BAD_GATEWAY.getStatusCode())));
        assertThat(getProblemDetail(gatewayResponse), containsString(PublicationPersistenceService.WARNING_MESSAGE));
    }


    private ImportDoiHandler handlerReceivingEmptyResponse(PublicationConverter publicationConverter)
        throws MetadataFetchException {
        var doiTransformService = mock(DoiTransformService.class);
        var doiProxyService = mock(DoiProxyService.class);
        var cristinProxyClient = mock(CristinClient.class);
        var metadataService = mock(MetadataService.class);
        when(metadataService.generateCreatePublicationRequest(any())).thenReturn(Optional.empty());

        return new ImportDoiHandler(publicationConverter, doiTransformService,
                                    doiProxyService, publicationPersistenceService, cristinProxyClient, metadataService,
                                    environment);
    }

    private DoiProxyService mockDoiProxyReceivingFailedResult() {
        var dataciteClient = mock(DataciteClient.class);
        var crossRefClient = mock(CrossRefClient.class);
        return new DoiProxyService(crossRefClient, dataciteClient);
    }

    private ImportDoiHandler createImportHandler(Environment environment)
        throws URISyntaxException, IOException, InvalidIssnException,
               MetadataNotFoundException, MetadataFetchException {
        var publicationConverter = mockPublicationConverter();
        var doiTransformService = mockDoiTransformServiceReturningSuccessfulResult();
        var doiProxyService = mockDoiProxyServiceReceivingSuccessfulResult();
        var cristinProxyClient = mock(CristinClient.class);
        var metadataService = mockMetadataServiceReturningSuccessfulResult();

        return new ImportDoiHandler(publicationConverter, doiTransformService,
                                    doiProxyService, publicationPersistenceService, cristinProxyClient, metadataService,
                                    environment);
    }

    private PublicationConverter mockPublicationConverter() {
        var publicationConverter = mock(PublicationConverter.class);
        when(publicationConverter.toSummary(any())).thenReturn(createSummary());
        return publicationConverter;
    }


    private Summary createSummary() {
        return new Summary.Builder().withIdentifier(SortableIdentifier.next())
                   .withTitle("Title on publication")
                   .withCreatorName("Name, Creator")
                   .withDate(new PublicationDate.Builder().withYear("2020").build()).build();
    }

    private PublicationPersistenceService mockResourcePersistenceServiceReceivingFailedResult()
        throws IOException, InterruptedException {
        return new PublicationPersistenceService(mockHttpClientReceivingFailure());
    }

    @SuppressWarnings("unchecked")
    private HttpClient mockHttpClientReceivingFailure() throws IOException, InterruptedException {
        var client = mock(HttpClient.class);
        var failedResponse = mockFailedHttpResponse();
        when(client.send(any(HttpRequest.class), any(BodyHandler.class))).thenReturn(failedResponse);
        return client;
    }

    @SuppressWarnings("unchecked")
    private HttpResponse<Object> mockFailedHttpResponse() {
        var response = mock(HttpResponse.class);
        when(response.statusCode()).thenReturn(Status.BAD_REQUEST.getStatusCode());
        return response;
    }

    private ByteArrayOutputStream outputStream() {
        return new ByteArrayOutputStream();
    }

    private GatewayResponse<Summary> parseSuccessResponse(String output) throws JsonProcessingException {
        return parseGatewayResponse(output, Summary.class);
    }

}
