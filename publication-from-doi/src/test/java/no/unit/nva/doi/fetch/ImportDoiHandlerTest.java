package no.unit.nva.doi.fetch;

import static com.google.common.net.HttpHeaders.AUTHORIZATION;
import static com.google.common.net.HttpHeaders.CONTENT_TYPE;
import static java.net.HttpURLConnection.HTTP_BAD_GATEWAY;
import static java.net.HttpURLConnection.HTTP_BAD_REQUEST;
import static java.net.HttpURLConnection.HTTP_INTERNAL_ERROR;
import static java.net.HttpURLConnection.HTTP_OK;
import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;
import static no.unit.nva.doi.fetch.RestApiConfig.restServiceObjectMapper;
import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static no.unit.nva.testutils.RandomDataGenerator.randomUri;
import static nva.commons.apigateway.ApiGatewayHandler.MESSAGE_FOR_RUNTIME_EXCEPTIONS_HIDING_IMPLEMENTATION_DETAILS_TO_API_CLIENTS;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
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
import com.amazonaws.services.lambda.runtime.CognitoIdentity;
import com.amazonaws.services.lambda.runtime.Context;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JavaType;
import com.google.common.net.HttpHeaders;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandler;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import no.unit.nva.doi.CrossRefClient;
import no.unit.nva.doi.DataciteClient;
import no.unit.nva.doi.DoiProxyService;
import no.unit.nva.doi.MetadataAndContentLocation;
import no.unit.nva.doi.fetch.exceptions.MetadataNotFoundException;
import no.unit.nva.doi.fetch.exceptions.UnsupportedDocumentTypeException;
import no.unit.nva.doi.fetch.model.PublicationDate;
import no.unit.nva.doi.fetch.model.RequestBody;
import no.unit.nva.doi.fetch.model.Summary;
import no.unit.nva.doi.fetch.service.PublicationConverter;
import no.unit.nva.doi.fetch.service.PublicationPersistenceService;
import no.unit.nva.doi.transformer.DoiTransformService;
import no.unit.nva.doi.transformer.utils.CristinProxyClient;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.metadata.CreatePublicationRequest;
import no.unit.nva.metadata.service.MetadataService;
import no.unit.nva.model.EntityDescription;
import no.unit.nva.model.Organization;
import no.unit.nva.model.Publication;
import no.unit.nva.model.PublicationStatus;
import no.unit.nva.model.ResourceOwner;
import no.unit.nva.model.Username;
import no.unit.nva.model.exceptions.InvalidIsbnException;
import no.unit.nva.model.exceptions.InvalidIssnException;
import no.unit.nva.testutils.HandlerRequestBuilder;
import no.unit.nva.testutils.TestHeaders;
import nva.commons.apigateway.ApiGatewayHandler;
import nva.commons.apigateway.GatewayResponse;
import nva.commons.core.Environment;
import nva.commons.logutils.LogUtils;
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
    public void testOkResponse()
        throws Exception {
        ImportDoiHandler importDoiHandler = this.createImportHandler(environment);
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        importDoiHandler.handleRequest(createSampleRequest(), output, context);
        GatewayResponse<Summary> gatewayResponse = parseSuccessResponse(output.toString());
        assertEquals(HTTP_OK, gatewayResponse.getStatusCode());
        assertThat(gatewayResponse.getHeaders(), hasKey(CONTENT_TYPE));
        assertThat(gatewayResponse.getHeaders(), hasKey(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN));
        Summary summary = gatewayResponse.getBodyObject(Summary.class);
        assertNotNull(summary.getIdentifier());

        var isDoi = true;
        CreatePublicationRequest expectedCreateRequest = expectedCreatePublicationRequest(isDoi,
                                                                                          URI.create(VALID_DOI));

        verify(publicationPersistenceService, times(1))
            .createPublication(eq(expectedCreateRequest), any(), any());
    }

    @Test
    public void handleRequestReturnsSummaryWithIdentifierWhenUrlIsValidNonDoi()
        throws Exception {
        ImportDoiHandler importDoiHandler = this.createImportHandler(environment);
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        importDoiHandler.handleRequest(nonDoiUrlInputStream(), output, context);
        GatewayResponse<Summary> gatewayResponse = parseSuccessResponse(output.toString());
        assertEquals(HTTP_OK, gatewayResponse.getStatusCode());
        assertThat(gatewayResponse.getHeaders(), hasKey(CONTENT_TYPE));
        assertThat(gatewayResponse.getHeaders(), hasKey(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN));
        Summary summary = gatewayResponse.getBodyObject(Summary.class);
        assertNotNull(summary.getIdentifier());

        var isDoi = false;
        CreatePublicationRequest expectedCreateRequest = expectedCreatePublicationRequest(isDoi,
                                                                                          URI.create(VALID_NON_DOI));

        verify(publicationPersistenceService, times(1))
            .createPublication(eq(expectedCreateRequest), any(), any());
    }

    @Test
    public void testBadGatewayResponseWhenUrlIsInvalidNonDoi() throws Exception {
        PublicationConverter publicationConverter = mock(PublicationConverter.class);

        ImportDoiHandler importDoiHandler = handlerReceivingEmptyResponse(publicationConverter);
        importDoiHandler.handleRequest(nonDoiUrlInputStream(), output, context);
        GatewayResponse<Problem> gatewayResponse = parseFailureResponse(output);
        assertEquals(HTTP_BAD_GATEWAY, gatewayResponse.getStatusCode());
        assertThat(getProblemDetail(gatewayResponse), containsString(ImportDoiHandler.NO_METADATA_FOUND_FOR));
    }

    @Test
    public void shouldReturnInternalErrorWhenUrlToPublicationProxyIsNotValidAndContainInformativeMessage()
        throws IOException, InvalidIssnException, URISyntaxException,
               MetadataNotFoundException, InvalidIsbnException, UnsupportedDocumentTypeException {

        var logger = LogUtils.getTestingAppenderForRootLogger();
        Environment environmentWithInvalidHost = createEnvironmentWithInvalidHost();
        ImportDoiHandler importDoiHandler = this.createImportHandler(environmentWithInvalidHost);

        importDoiHandler.handleRequest(createSampleRequest(), output, context);
        var response = GatewayResponse.fromOutputStream(output, Problem.class);
        assertThat(response.getStatusCode(), is(equalTo(HTTP_INTERNAL_ERROR)));
        assertThat(logger.getMessages(), containsString("Missing host for creating URI"));
    }

    @Test
    public void testBadRequestResponse() throws Exception {
        PublicationConverter publicationConverter = mock(PublicationConverter.class);
        DoiTransformService doiTransformService = mockDoiTransformServiceReturningSuccessfulResult();
        DoiProxyService doiProxyService = mock(DoiProxyService.class);
        PublicationPersistenceService publicationPersistenceService = mock(PublicationPersistenceService.class);
        CristinProxyClient cristinProxyClient = mock(CristinProxyClient.class);
        MetadataService metadataService = mock(MetadataService.class);
        ImportDoiHandler importDoiHandler = new ImportDoiHandler(publicationConverter, doiTransformService,
                                                                 doiProxyService, publicationPersistenceService, cristinProxyClient,
                                                                 metadataService, environment);
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        importDoiHandler.handleRequest(malformedInputStream(), output, context);
        GatewayResponse<Problem> gatewayResponse = parseFailureResponse(output);
        assertEquals(HTTP_BAD_REQUEST, gatewayResponse.getStatusCode());
        assertThat(getProblemDetail(gatewayResponse), containsString(ImportDoiHandler.NULL_DOI_URL_ERROR));
    }

    @Test
    public void testInternalServerErrorResponse() throws Exception {
        PublicationConverter publicationConverter = mock(PublicationConverter.class);
        when(publicationConverter.toSummary(any())).thenThrow(new RuntimeException(SOME_ERROR_MESSAGE));
        DoiTransformService doiTransformService = mockDoiTransformServiceReturningSuccessfulResult();
        DoiProxyService doiProxyService = mockDoiProxyServiceReceivingSuccessfulResult();
        PublicationPersistenceService publicationPersistenceService = mock(PublicationPersistenceService.class);
        CristinProxyClient cristinProxyClient = mock(CristinProxyClient.class);
        MetadataService metadataService = mock(MetadataService.class);

        ImportDoiHandler importDoiHandler = new ImportDoiHandler(publicationConverter, doiTransformService,
                                                                 doiProxyService, publicationPersistenceService, cristinProxyClient,
                                                                 metadataService, environment);
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        importDoiHandler.handleRequest(createSampleRequest(), output, context);
        GatewayResponse<Problem> gatewayResponse = parseFailureResponse(output);
        assertEquals(HTTP_INTERNAL_ERROR, gatewayResponse.getStatusCode());
        assertThat(getProblemDetail(gatewayResponse), containsString(
            MESSAGE_FOR_RUNTIME_EXCEPTIONS_HIDING_IMPLEMENTATION_DETAILS_TO_API_CLIENTS));
    }

    @Test
    @DisplayName("handler returns BadGateway error when DoiProxyService returns failed response")
    public void handlerReturnsBadGatewayErrorWhenDoiProxyServiceReturnsFailedResponse()
        throws Exception {

        PublicationConverter publicationConverter = mockPublicationConverter();
        DoiTransformService doiTransformService = mockDoiTransformServiceReturningSuccessfulResult();
        DoiProxyService doiProxyService = mockDoiProxyReceivingFailedResult();
        PublicationPersistenceService publicationPersistenceService = mock(PublicationPersistenceService.class);
        CristinProxyClient cristinProxyClient = mock(CristinProxyClient.class);
        MetadataService metadataService = mock(MetadataService.class);

        ImportDoiHandler handler = new ImportDoiHandler(publicationConverter, doiTransformService, doiProxyService,
                                                        publicationPersistenceService, cristinProxyClient, metadataService,
                                                        environment);
        ByteArrayOutputStream outputStream = outputStream();
        handler.handleRequest(createSampleRequest(), outputStream, context);
        GatewayResponse<Problem> gatewayResponse = parseFailureResponse(outputStream);
        assertThat(gatewayResponse.getStatusCode(), is(equalTo(Status.BAD_GATEWAY.getStatusCode())));
        assertThat(getProblemDetail(gatewayResponse), containsString(DoiProxyService.ERROR_READING_METADATA));
    }

    @Test
    @DisplayName("handler returns BadGateway when ResourcePersistenceService returns failed response")
    public void handlerReturnsBadGatewayErrorWhenResourcePersistenceServiceReturnsFailedResponse()
        throws Exception {

        PublicationConverter publicationConverter = mockPublicationConverter();
        DoiProxyService doiProxyService = mockDoiProxyServiceReceivingSuccessfulResult();
        DoiTransformService doiTransformService = mockDoiTransformServiceReturningSuccessfulResult();
        CristinProxyClient cristinProxyClient = mock(CristinProxyClient.class);
        MetadataService metadataService = mock(MetadataService.class);

        PublicationPersistenceService publicationPersistenceService =
            mockResourcePersistenceServiceReceivingFailedResult();

        ImportDoiHandler handler = new ImportDoiHandler(publicationConverter, doiTransformService, doiProxyService,
                                                        publicationPersistenceService, cristinProxyClient, metadataService,
                                                        environment);
        ByteArrayOutputStream outputStream = outputStream();
        handler.handleRequest(createSampleRequest(), outputStream, context);
        GatewayResponse<Problem> gatewayResponse = parseFailureResponse(outputStream);
        assertThat(gatewayResponse.getStatusCode(), is(equalTo(Status.BAD_GATEWAY.getStatusCode())));
        assertThat(getProblemDetail(gatewayResponse), containsString(PublicationPersistenceService.WARNING_MESSAGE));
    }


    private ImportDoiHandler handlerReceivingEmptyResponse(PublicationConverter publicationConverter) {
        DoiTransformService doiTransformService = mock(DoiTransformService.class);
        DoiProxyService doiProxyService = mock(DoiProxyService.class);
        CristinProxyClient cristinProxyClient = mock(CristinProxyClient.class);
        MetadataService metadataService = mock(MetadataService.class);
        when(metadataService.generateCreatePublicationRequest(any())).thenReturn(Optional.empty());

        return new ImportDoiHandler(publicationConverter, doiTransformService,
                                    doiProxyService, publicationPersistenceService, cristinProxyClient, metadataService,
                                    environment);
    }

    private DoiProxyService mockDoiProxyReceivingFailedResult() {
        DataciteClient dataciteClient = mock(DataciteClient.class);
        CrossRefClient crossRefClient = mock(CrossRefClient.class);
        return new DoiProxyService(crossRefClient, dataciteClient);
    }

    private ImportDoiHandler createImportHandler(Environment environment)
        throws URISyntaxException, IOException, InvalidIssnException,
               MetadataNotFoundException, InvalidIsbnException, UnsupportedDocumentTypeException {
        PublicationConverter publicationConverter = mockPublicationConverter();
        DoiTransformService doiTransformService = mockDoiTransformServiceReturningSuccessfulResult();
        DoiProxyService doiProxyService = mockDoiProxyServiceReceivingSuccessfulResult();
        CristinProxyClient cristinProxyClient = mock(CristinProxyClient.class);
        MetadataService metadataService = mockMetadataServiceReturningSuccessfulResult();

        return new ImportDoiHandler(publicationConverter, doiTransformService,
                                    doiProxyService, publicationPersistenceService, cristinProxyClient, metadataService,
                                    environment);
    }

    private PublicationConverter mockPublicationConverter() {
        PublicationConverter publicationConverter = mock(PublicationConverter.class);
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
        HttpClient client = mock(HttpClient.class);
        HttpResponse<Object> failedResponse = mockFailedHttpResponse();
        when(client.send(any(HttpRequest.class), any(BodyHandler.class))).thenReturn(failedResponse);
        return client;
    }

    @SuppressWarnings("unchecked")
    private HttpResponse<Object> mockFailedHttpResponse() {
        HttpResponse<Object> response = mock(HttpResponse.class);
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
