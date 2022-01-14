package no.sikt.nva.scopus;

import com.amazonaws.services.lambda.runtime.Context;
import no.unit.nva.metadata.service.MetadataService;
import no.unit.nva.s3.S3Driver;
import nva.commons.apigateway.ApiGatewayHandler;
import nva.commons.apigateway.RequestInfo;
import nva.commons.apigateway.exceptions.ApiGatewayException;
import nva.commons.core.Environment;
import nva.commons.core.JacocoGenerated;
import nva.commons.core.paths.UnixPath;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

//Todo: find our own Response object
public class ScopusHandler extends ApiGatewayHandler<Void, String> {

    protected static final String BUCKET_NAME = "BUCKET_NAME";
    public static final String FILE_IDENTIFIER = "fileIdentifier";
    private final transient S3Driver s3Driver;
    private final transient PublicationConverter publicationConverter;
    private final transient PublicationPersistenceService publicationPersistenceService;
    private final transient MetadataService metadataService;

    private static final Logger logger = LoggerFactory.getLogger(ScopusHandler.class);

    @JacocoGenerated
    public ScopusHandler() {
        this(new Environment());
    }

    @JacocoGenerated
    public ScopusHandler(Environment environment) {
        this(new S3Driver(environment.readEnv(BUCKET_NAME)), new PublicationConverter(),
                new PublicationPersistenceService(), getMetadataService(), environment);
    }

    /**
     * Constructor for ScopusHandler.
     *
     * @param environment  environment.
     */
    public ScopusHandler(S3Driver s3Driver,
                       PublicationConverter publicationConverter,
                       PublicationPersistenceService publicationPersistenceService,
                       MetadataService metadataService,
                       Environment environment) {
        super(Void.class, environment);
        this.s3Driver = s3Driver;
        this.publicationConverter = publicationConverter;
        this.publicationPersistenceService = publicationPersistenceService;
        this.metadataService = metadataService;
    }

    @JacocoGenerated
    private static MetadataService getMetadataService() {
        try {
            return new MetadataService();
        } catch (IOException e) {
            throw new RuntimeException("Error creating handler", e);
        }
    }

    @Override
    protected String processInput(Void input, RequestInfo requestInfo, Context context) throws ApiGatewayException {
        String filename = requestInfo.getQueryParameter(FILE_IDENTIFIER);
        String contents = s3Driver.getFile(UnixPath.fromString(filename));
        // parse file to a ScopusPublication
//              ScopusPublication scopusPublication = publicationConverter.convert(contents);
        // enrich contributors with help of nva-cristin-service
        // enrich organizations with help of nva-cristin-service
        // enrich journal with help of nva-publication-channels
        // enrich publisher with help of nva-publication-channels
        // metadataTransform ScopusPublication into CreatePublicationRequest
        metadataService.toString();
        // send CreatePublicationRequest to nva-publication-service
        publicationPersistenceService.toString();
        return null;
    }

    @Override
    protected Integer getSuccessStatusCode(Void input, String output) {
        return null;
    }
}
