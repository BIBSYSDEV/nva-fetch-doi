package no.sikt.nva.scopus;

import com.amazonaws.services.lambda.runtime.Context;
import no.unit.nva.doi.fetch.model.RequestBody;
import no.unit.nva.doi.fetch.model.Summary;
import no.unit.nva.metadata.service.MetadataService;
import nva.commons.apigateway.ApiGatewayHandler;
import nva.commons.apigateway.RequestInfo;
import nva.commons.apigateway.exceptions.ApiGatewayException;
import nva.commons.core.Environment;
import nva.commons.core.JacocoGenerated;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

//Todo: find our own Request object
//Todo: find our own Response object
public class ScopusHandler extends ApiGatewayHandler<RequestBody, Summary> {

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
        this(new PublicationConverter(), new PublicationPersistenceService(), getMetadataService(), environment);
    }

    /**
     * Constructor for ScopusHandler.
     *
     * @param environment  environment.
     */
    public ScopusHandler(PublicationConverter publicationConverter,
                       PublicationPersistenceService publicationPersistenceService,
                       MetadataService metadataService,
                       Environment environment) {
        super(RequestBody.class, environment);
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
    protected Summary processInput(RequestBody input, RequestInfo requestInfo, Context context) throws ApiGatewayException {
        return null;
    }

    @Override
    protected Integer getSuccessStatusCode(RequestBody input, Summary output) {
        return null;
    }
}
