package no.unit.nva.doi.fetch;

import static com.google.common.net.HttpHeaders.AUTHORIZATION;
import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;
import static java.util.Collections.emptySet;
import static no.unit.nva.doi.fetch.RestApiConfig.restServiceObjectMapper;
import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static no.unit.nva.testutils.RandomDataGenerator.randomUri;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import com.amazonaws.services.lambda.runtime.CognitoIdentity;
import com.amazonaws.services.lambda.runtime.Context;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JavaType;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import no.unit.nva.doi.DoiProxyService;
import no.unit.nva.doi.MetadataAndContentLocation;
import no.unit.nva.doi.fetch.exceptions.MetadataNotFoundException;
import no.unit.nva.doi.fetch.exceptions.UnsupportedDocumentTypeException;
import no.unit.nva.doi.fetch.model.RequestBody;
import no.unit.nva.doi.transformer.DoiTransformService;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.metadata.CreatePublicationRequest;
import no.unit.nva.metadata.service.MetadataService;
import no.unit.nva.model.EntityDescription;
import no.unit.nva.model.Organization;
import no.unit.nva.model.Publication;
import no.unit.nva.model.PublicationStatus;
import no.unit.nva.model.ResourceOwner;
import no.unit.nva.model.Username;
import no.unit.nva.model.associatedartifacts.AssociatedArtifactList;
import no.unit.nva.model.associatedartifacts.AssociatedLink;
import no.unit.nva.model.exceptions.InvalidIsbnException;
import no.unit.nva.model.exceptions.InvalidIssnException;
import no.unit.nva.testutils.HandlerRequestBuilder;
import no.unit.nva.testutils.TestHeaders;
import nva.commons.apigateway.ApiGatewayHandler;
import nva.commons.apigateway.GatewayResponse;
import nva.commons.core.Environment;
import org.zalando.problem.Problem;

public class DoiHandlerTestUtils {

    static final String MAIN_TITLE = "Main title";
    public static final String VALID_DOI = "https://doi.org/10.1109/5.771073";
    public static final String VALID_NON_DOI = "http://example.org/metadata";
    public static final String ALL_ORIGINS = "*";
    public static final String INVALID_HOST_STRING = "https://\\.)_";
    static final String SOME_ERROR_MESSAGE = "SomeErrorMessage";


    CreatePublicationRequest expectedCreatePublicationRequest(boolean isDoi, URI metadataSource) {
        var expectedCreateRequest = new CreatePublicationRequest();
        expectedCreateRequest.setEntityDescription(expectedEntityDescription(isDoi, metadataSource));
        if (isDoi) { // deserialization causes all collections to be empty:
            expectedCreateRequest.setAdditionalIdentifiers(emptySet());
            expectedCreateRequest.setFundings(emptyList());
            expectedCreateRequest.setProjects(emptyList());
            expectedCreateRequest.setSubjects(emptyList());
        }
        expectedCreateRequest.setAssociatedArtifacts(
            isDoi ? new AssociatedArtifactList() : artifactsWithLink(metadataSource));

        return expectedCreateRequest;
    }

    private AssociatedArtifactList artifactsWithLink(URI metadataSource) {
        return new AssociatedArtifactList(new AssociatedLink(metadataSource, null, null));
    }

    private EntityDescription expectedEntityDescription(boolean isDoi, URI metadataSource) {
        var builder = new EntityDescription.Builder()
                          .withMainTitle(MAIN_TITLE)
                          .withMetadataSource(metadataSource);

        if (isDoi) { // deserialization causes all collections to be empty:
            builder.withTags(emptyList())
                .withContributors(emptyList())
                .withAlternativeTitles(emptyMap())
                .withAlternativeAbstracts(emptyMap());
        }
        return builder.build();
    }

    DoiTransformService mockDoiTransformServiceReturningSuccessfulResult()
        throws URISyntaxException, IOException, InvalidIssnException,
               InvalidIsbnException, UnsupportedDocumentTypeException {
        DoiTransformService service = mock(DoiTransformService.class);
        when(service.transformPublication(anyString(), anyString(), anyString(), any()))
            .thenReturn(getPublication());
        return service;
    }

    MetadataService mockMetadataServiceReturningSuccessfulResult() {

        EntityDescription entityDescription = new EntityDescription();
        entityDescription.setMainTitle(MAIN_TITLE);
        entityDescription.setAlternativeAbstracts(emptyMap());
        entityDescription.setAlternativeTitles(emptyMap());
        entityDescription.setContributors(emptyList());
        entityDescription.setTags(emptyList());

        CreatePublicationRequest request = new CreatePublicationRequest();
        request.setEntityDescription(entityDescription);

        MetadataService service = mock(MetadataService.class);
        when(service.generateCreatePublicationRequest(any()))
            .thenReturn(Optional.of(request));
        return service;
    }

    private Publication getPublication() {
        return new Publication.Builder()
                   .withIdentifier(new SortableIdentifier(UUID.randomUUID().toString()))
                   .withCreatedDate(Instant.now())
                   .withModifiedDate(Instant.now())
                   .withStatus(PublicationStatus.DRAFT)
                   .withPublisher(new Organization.Builder().withId(URI.create("http://example.org/123")).build())
                   .withEntityDescription(new EntityDescription.Builder().withMainTitle(MAIN_TITLE).build())
                   .withResourceOwner(randomResourceOwner())
                   .build();
    }

    private ResourceOwner randomResourceOwner() {
        return new ResourceOwner(new Username(randomString()), randomUri());
    }

    DoiProxyService mockDoiProxyServiceReceivingSuccessfulResult()
        throws MetadataNotFoundException, IOException, URISyntaxException {
        DoiProxyService doiProxyService = mock(DoiProxyService.class);
        when(doiProxyService.lookupDoiMetadata(anyString(), any())).thenReturn(metadataAndContentLocation());
        return doiProxyService;
    }

    private MetadataAndContentLocation metadataAndContentLocation() throws JsonProcessingException {
        return new MetadataAndContentLocation("datacite",
                                              restServiceObjectMapper.writeValueAsString(getPublication()));
    }

    Context getMockContext() {
        Context context = mock(Context.class);
        CognitoIdentity cognitoIdentity = mock(CognitoIdentity.class);
        when(context.getIdentity()).thenReturn(cognitoIdentity);
        when(cognitoIdentity.getIdentityPoolId()).thenReturn("junit");
        return context;
    }

    private InputStream createSampleRequest(URL url) throws JsonProcessingException {

        RequestBody requestBody = createSampleRequestBody(url);

        Map<String, String> requestHeaders = new HashMap<>();
        requestHeaders.put(AUTHORIZATION, "some api key");
        requestHeaders.putAll(TestHeaders.getRequestHeaders());

        return new HandlerRequestBuilder<RequestBody>(restServiceObjectMapper)
                   .withBody(requestBody)
                   .withHeaders(requestHeaders)
                   .withUserName(randomString())
                   .withCurrentCustomer(randomUri())
                   .build();
    }

    InputStream createSampleRequest() throws MalformedURLException, JsonProcessingException {
        return createSampleRequest(new URL(VALID_DOI));
    }

    InputStream nonDoiUrlInputStream() throws MalformedURLException, JsonProcessingException {
        return createSampleRequest(new URL(VALID_NON_DOI));
    }

    InputStream malformedInputStream() throws JsonProcessingException {

        Map<String, String> requestHeaders = new HashMap<>();
        requestHeaders.put(AUTHORIZATION, "some api key");
        requestHeaders.putAll(TestHeaders.getRequestHeaders());

        return new HandlerRequestBuilder<RequestBody>(restServiceObjectMapper)
                   .withHeaders(requestHeaders)
                   .withUserName(randomString())
                   .withCurrentCustomer(randomUri())
                   .build();
    }

    GatewayResponse<Problem> parseFailureResponse(OutputStream output) throws JsonProcessingException {
        return parseGatewayResponse(output.toString(), Problem.class);
    }

    String getProblemDetail(GatewayResponse<Problem> gatewayResponse) throws JsonProcessingException {
        return gatewayResponse.getBodyObject(Problem.class).getDetail();
    }

    <T> GatewayResponse<T> parseGatewayResponse(String output, Class<T> responseObjectClass)
        throws JsonProcessingException {
        JavaType typeRef = restServiceObjectMapper.getTypeFactory()
                               .constructParametricType(GatewayResponse.class, responseObjectClass);
        return restServiceObjectMapper.readValue(output, typeRef);
    }

    private RequestBody createSampleRequestBody(URL url) {
        RequestBody requestBody = new RequestBody();
        requestBody.setDoiUrl(url);
        return requestBody;
    }

    Environment createEnvironmentWithInvalidHost() {
        Environment environment = mock(Environment.class);
        when(environment.readEnv(ApiGatewayHandler.ALLOWED_ORIGIN_ENV)).thenReturn(ALL_ORIGINS);
        when(environment.readEnv(ImportDoiHandler.PUBLICATION_API_HOST_ENV)).thenReturn(INVALID_HOST_STRING);
        return environment;
    }
}
