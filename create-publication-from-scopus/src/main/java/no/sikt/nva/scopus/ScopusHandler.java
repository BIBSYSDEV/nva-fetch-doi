package no.sikt.nva.scopus;

import com.amazonaws.services.lambda.runtime.Context;
import no.unit.nva.doi.fetch.model.Summary;
import no.unit.nva.metadata.service.MetadataService;
import nva.commons.apigateway.ApiGatewayHandler;
import nva.commons.apigateway.RequestInfo;
import nva.commons.apigateway.exceptions.ApiGatewayException;
import nva.commons.core.Environment;
import nva.commons.core.JacocoGenerated;

import java.io.IOException;
import java.io.InputStream;

//Todo: find our own Response object
public class ScopusHandler extends ApiGatewayHandler<Void, Summary> {

    private final transient ScopusS3Client s3Client;
    private final transient PublicationConverter publicationConverter;
    private final transient PublicationPersistenceService publicationPersistenceService;
    private final transient MetadataService metadataService;

//    private static final Logger logger = LoggerFactory.getLogger(ScopusHandler.class);

    @JacocoGenerated
    public ScopusHandler() {
        this(new Environment());
    }

    @JacocoGenerated
    public ScopusHandler(Environment environment) {
        this(new ScopusS3Client(), new PublicationConverter(), new PublicationPersistenceService(), getMetadataService(), environment);
    }

    /**
     * Constructor for ScopusHandler.
     *
     * @param environment  environment.
     */
    public ScopusHandler(ScopusS3Client s3Client,
                       PublicationConverter publicationConverter,
                       PublicationPersistenceService publicationPersistenceService,
                       MetadataService metadataService,
                       Environment environment) {
        super(Void.class, environment);
        this.s3Client = s3Client;
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
    protected Summary processInput(Void input, RequestInfo requestInfo, Context context) throws ApiGatewayException {
        InputStream inputStream = s3Client.getFile("full_format.xml");
        metadataService.toString();
        publicationConverter.toString();
        publicationPersistenceService.toString();
        return null;
    }

    @Override
    protected Integer getSuccessStatusCode(Void input, Summary output) {
        return null;
    }
}
