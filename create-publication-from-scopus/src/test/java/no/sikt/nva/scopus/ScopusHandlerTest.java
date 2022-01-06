package no.sikt.nva.scopus;

import com.amazonaws.services.lambda.runtime.CognitoIdentity;
import com.amazonaws.services.lambda.runtime.Context;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.net.HttpHeaders;
import no.unit.nva.api.CreatePublicationRequest;
import no.unit.nva.doi.DoiProxyService;
import no.unit.nva.doi.fetch.exceptions.MetadataNotFoundException;
import no.unit.nva.doi.fetch.exceptions.UnsupportedDocumentTypeException;
import no.unit.nva.doi.fetch.model.Summary;
import no.unit.nva.doi.transformer.DoiTransformService;
import no.unit.nva.doi.transformer.utils.BareProxyClient;
import no.unit.nva.metadata.service.MetadataService;
import no.unit.nva.model.EntityDescription;
import no.unit.nva.model.exceptions.InvalidIsbnException;
import no.unit.nva.model.exceptions.InvalidIssnException;
import nva.commons.apigateway.ApiGatewayHandler;
import nva.commons.apigateway.GatewayResponse;
import nva.commons.core.Environment;
import nva.commons.core.JsonUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.zalando.problem.Problem;

import java.io.*;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Optional;

import static org.apache.http.HttpHeaders.CONTENT_TYPE;
import static org.apache.http.HttpStatus.SC_BAD_REQUEST;
import static org.apache.http.HttpStatus.SC_OK;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasKey;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ScopusHandlerTest {

    public static final String ALL_ORIGINS = "*";
    private Environment environment;
    private Context context;
    public static final ObjectMapper restServiceObjectMapper = JsonUtils.dtoObjectMapper;

    /**
     * Set up environment.
     */
    @BeforeEach
    public void setUp() {
        environment = mock(Environment.class);
        context = getMockContext();
        when(environment.readEnv(ApiGatewayHandler.ALLOWED_ORIGIN_ENV)).thenReturn(ALL_ORIGINS);
    }

    @Test
    public void testResponseWithEmptyRequest()
            throws Exception {
        ScopusHandler handler = createHandler(environment);
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        handler.handleRequest(handlerInputStream(), output, context);
        GatewayResponse<Summary> gatewayResponse = parseSuccessResponse(output.toString());
        assertEquals(SC_BAD_REQUEST, gatewayResponse.getStatusCode());
        assertThat(gatewayResponse.getHeaders(), hasKey(CONTENT_TYPE));
        assertThat(gatewayResponse.getHeaders(), hasKey(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN));
    }

    private GatewayResponse<Summary> parseSuccessResponse(String output) throws JsonProcessingException {
        return parseGatewayResponse(output, Summary.class);
    }

    private GatewayResponse<Problem> parseFailureResponse(OutputStream output) throws JsonProcessingException {
        return parseGatewayResponse(output.toString(), Problem.class);
    }

    private <T> GatewayResponse<T> parseGatewayResponse(String output, Class<T> responseObjectClass)
            throws JsonProcessingException {
        JavaType typeRef = restServiceObjectMapper.getTypeFactory()
                .constructParametricType(GatewayResponse.class, responseObjectClass);
        return restServiceObjectMapper.readValue(output, typeRef);
    }

    private InputStream handlerInputStream() {
        return new ByteArrayInputStream("string".getBytes());
    }

    private ScopusHandler createHandler(Environment environment) {
        PublicationConverter publicationConverter = mockPublicationConverter();
        PublicationPersistenceService publicationPersistenceService = mock(PublicationPersistenceService.class);
        MetadataService metadataService = mockMetadataServiceReturningSuccessfulResult();

        return new ScopusHandler(publicationConverter, publicationPersistenceService, metadataService, environment);
    }

    private MetadataService mockMetadataServiceReturningSuccessfulResult() {
        MetadataService service = mock(MetadataService.class);

        EntityDescription entityDescription = new EntityDescription();
        entityDescription.setMainTitle("Main title");
        CreatePublicationRequest request = new CreatePublicationRequest();
        request.setEntityDescription(entityDescription);

        when(service.generateCreatePublicationRequest(any())).thenReturn(Optional.of(request));
        return service;
    }

    private PublicationConverter mockPublicationConverter() {
        PublicationConverter publicationConverter = mock(PublicationConverter.class);
        return publicationConverter;
    }

    private Context getMockContext() {
        Context context = mock(Context.class);
        CognitoIdentity cognitoIdentity = mock(CognitoIdentity.class);
        when(context.getIdentity()).thenReturn(cognitoIdentity);
        when(cognitoIdentity.getIdentityPoolId()).thenReturn("junit");
        return context;
    }


}