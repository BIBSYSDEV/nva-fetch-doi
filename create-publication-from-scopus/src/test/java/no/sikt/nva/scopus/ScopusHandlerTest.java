package no.sikt.nva.scopus;

import com.amazonaws.services.lambda.runtime.CognitoIdentity;
import com.amazonaws.services.lambda.runtime.Context;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.net.HttpHeaders;
import no.unit.nva.api.CreatePublicationRequest;
import no.unit.nva.metadata.service.MetadataService;
import no.unit.nva.model.EntityDescription;
import no.unit.nva.s3.S3Driver;
import no.unit.nva.stubs.FakeContext;
import no.unit.nva.testutils.HandlerRequestBuilder;
import nva.commons.apigateway.ApiGatewayHandler;
import nva.commons.apigateway.GatewayResponse;
import nva.commons.core.Environment;
import nva.commons.core.JsonUtils;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.apache.http.HttpHeaders.CONTENT_TYPE;
import static org.apache.http.HttpStatus.SC_BAD_REQUEST;
import static org.apache.http.HttpStatus.SC_INTERNAL_SERVER_ERROR;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasKey;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
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
        context = new FakeContext();
        when(environment.readEnv(ApiGatewayHandler.ALLOWED_ORIGIN_ENV)).thenReturn(ALL_ORIGINS);
    }

    @Test
    public void testResponseWithEmptyRequest()
            throws Exception {
        HandlerRequestBuilder<Map<String, String>> handlerRequestBuilder = new HandlerRequestBuilder<>(restServiceObjectMapper);
        String generatedString = RandomStringUtils.randomAlphanumeric(10);
        Map<String, String> queryParams = new HashMap<>();
        queryParams.put(ScopusHandler.FILE_IDENTIFIER, generatedString);
        handlerRequestBuilder.withQueryParameters(queryParams);
        InputStream inputStream = handlerRequestBuilder.build();
        ScopusHandler handler = createHandler();
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        handler.handleRequest(inputStream, output, context);
        GatewayResponse<String> gatewayResponse = GatewayResponse.fromOutputStream(output);
        assertEquals(SC_INTERNAL_SERVER_ERROR, gatewayResponse.getStatusCode());
        assertThat(gatewayResponse.getHeaders(), hasKey(CONTENT_TYPE));
        assertThat(gatewayResponse.getHeaders(), hasKey(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN));
        assertNull(handler.getSuccessStatusCode(null, null));
    }

    private ScopusHandler createHandler() {
        S3Driver s3Client = mock(S3Driver.class);
        PublicationConverter publicationConverter = mock(PublicationConverter.class);
        PublicationPersistenceService publicationPersistenceService = mock(PublicationPersistenceService.class);
        MetadataService metadataService = mockMetadataServiceReturningSuccessfulResult();

        return new ScopusHandler(s3Client, publicationConverter, publicationPersistenceService, metadataService,
                environment);
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

    private Context getMockContext() {
        Context context = mock(Context.class);
        CognitoIdentity cognitoIdentity = mock(CognitoIdentity.class);
        when(context.getIdentity()).thenReturn(cognitoIdentity);
        when(cognitoIdentity.getIdentityPoolId()).thenReturn("junit");
        return context;
    }


}