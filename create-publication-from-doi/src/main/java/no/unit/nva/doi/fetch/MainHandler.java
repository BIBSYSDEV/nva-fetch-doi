package no.unit.nva.doi.fetch;

import static java.util.Objects.isNull;
import static nva.commons.core.attempt.Try.attempt;
import com.amazonaws.services.lambda.runtime.Context;
import java.net.HttpURLConnection;
import no.unit.nva.doi.DataciteContentType;
import no.unit.nva.doi.DoiProxyService;
import no.unit.nva.doi.fetch.model.Summary;
import no.unit.nva.identifiers.SortableIdentifier;
import nva.commons.apigateway.ApiGatewayHandler;
import nva.commons.apigateway.RequestInfo;
import nva.commons.apigateway.exceptions.ApiGatewayException;
import nva.commons.apigateway.exceptions.BadRequestException;
import nva.commons.core.Environment;
import nva.commons.core.JacocoGenerated;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MainHandler extends ApiGatewayHandler<RequestBody, Summary> {

    public static final String PUBLICATION_API_HOST_ENV = "PUBLICATION_API_HOST";
    public static final String NULL_DOI_URL_ERROR = "doiUrl can not be null";
    public static final String NO_METADATA_FOUND_FOR = "No metadata found for: ";
    private static final Logger logger = LoggerFactory.getLogger(MainHandler.class);
    private static final Environment ENVIRONMENT = new Environment();
    //    private final transient PublicationConverter publicationConverter;
//    private final transient DoiTransformService doiTransformService;
    private final transient DoiProxyService doiProxyService;
//    private final transient PublicationPersistenceService publicationPersistenceService;
//    private final transient BareProxyClient bareProxyClient;
//    private final transient String publicationApiHost;
//    private final transient MetadataService metadataService;


    @JacocoGenerated
    public MainHandler() {
        this(new DoiProxyService(ENVIRONMENT));
//        this(new PublicationConverter(), new DoiTransformService(),
//             new DoiProxyService(environment), new PublicationPersistenceService(), new BareProxyClient(),
//             getMetadataService(), environment);
    }

    public MainHandler(DoiProxyService doiProxyService) {
        super(RequestBody.class);
        this.doiProxyService = doiProxyService;

    }

    @Override
    protected Summary processInput(RequestBody input, RequestInfo requestInfo, Context context)
        throws ApiGatewayException {
            logger.info("Some message from "+ input);
            validate(input);
            var doiProxyResult=attempt(()->doiProxyService.lookupDoiMetadata(input.getDoiUrl().toString(),
                                                        DataciteContentType.DATACITE_JSON))
                .orElseThrow();
            logger.info(doiProxyResult.getJson());
            return new Summary.Builder()
                .withCreatorName(input.getDoiUrl().toString())
                .withIdentifier(SortableIdentifier.next())
                .build();

    }

    @Override
    protected Integer getSuccessStatusCode(RequestBody input, Summary output) {
        return HttpURLConnection.HTTP_OK;
    }

//    @JacocoGenerated
//    private static MetadataService getMetadataService() {
//        return new MetadataService();
//    }
//
//    private ApiGatewayException handleError(Exception exception) {
//        if (exception instanceof ApiGatewayException) {
//            return (ApiGatewayException) exception;
//        }
//        if (exception instanceof RuntimeException) {
//            throw (RuntimeException) exception;
//        }
//        throw new RuntimeException(exception);
//    }
//
//    private CreatePublicationRequest newCreatePublicationRequest(String owner, URI customerId, URL url)
//        throws URISyntaxException, IOException, InvalidIssnException,
//               MetadataNotFoundException, InvalidIsbnException, UnsupportedDocumentTypeException {
//        CreatePublicationRequest request;
//
//        if (urlIsValidDoi(url)) {
//            logger.info("URL is a DOI");
//            request = getPublicationFromDoi(owner, customerId, url);
//        } else {
//            logger.info("URL is NOT a DOI, falling back to web metadata scraping");
//            request = getPublicationFromOtherUrl(url);
//        }
//        return request;
//    }
//
//    private boolean urlIsValidDoi(URL url) {
//        return DoiValidator.validate(url);
//    }
//
//    private CreatePublicationRequest getPublicationFromOtherUrl(URL url)
//        throws URISyntaxException, MetadataNotFoundException {
//        return metadataService.generateCreatePublicationRequest(url.toURI())
//            .orElseThrow(() -> new MetadataNotFoundException(NO_METADATA_FOUND_FOR + url));
//    }
//
//    private CreatePublicationRequest getPublicationFromDoi(String owner, URI customerId, URL doi)
//        throws URISyntaxException, IOException, InvalidIssnException,
//               MetadataNotFoundException, InvalidIsbnException, UnsupportedDocumentTypeException {
//        var publicationMetadata = getPublicationMetadataFromDoi(doi, owner, customerId);
//        Publication publication =
//            IdentityUpdater.enrichPublicationCreators(bareProxyClient, publicationMetadata);
//        return restServiceObjectMapper.convertValue(publication, CreatePublicationRequest.class);
//    }
//
//    private URI urlToPublicationProxy() {
//        return attempt(() -> UriWrapper.fromHost(publicationApiHost).getUri())
//            .orElseThrow(failure -> new IllegalStateException(failure.getException()));
//    }
//
    private void validate(RequestBody input) throws BadRequestException {
        if (isNull(input) || isNull(input.getDoiUrl())) {
            throw new BadRequestException(NULL_DOI_URL_ERROR);
        }
    }
//
//    private Publication getPublicationMetadataFromDoi(URL doiUrl,
//                                                      String owner, URI customerId)
//        throws URISyntaxException, IOException, InvalidIssnException,
//               MetadataNotFoundException, InvalidIsbnException, UnsupportedDocumentTypeException {
//
//        MetadataAndContentLocation metadataAndContentLocation = doiProxyService.lookupDoiMetadata(
//            doiUrl.toString(), DataciteContentType.DATACITE_JSON);
//
//        return doiTransformService.transformPublication(
//            metadataAndContentLocation.getJson(),
//            metadataAndContentLocation.getContentHeader(), owner, customerId);
//    }
//
//    private PublicationResponse tryCreatePublication(String authorization, URI apiUrl, CreatePublicationRequest request)
//        throws InterruptedException, IOException, CreatePublicationException {
//        return createPublication(authorization, apiUrl, request);
//    }
//
//    private PublicationResponse createPublication(String authorization, URI apiUrl, CreatePublicationRequest request)
//        throws InterruptedException, CreatePublicationException, IOException {
//        return publicationPersistenceService.createPublication(request, apiUrl, authorization);
//    }
}
