package no.sikt.nva.scopus;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestStreamHandler;
import com.amazonaws.services.lambda.runtime.events.S3Event;
import no.unit.nva.metadata.service.MetadataService;
import no.unit.nva.s3.S3Driver;
import nva.commons.apigateway.ApiGatewayHandler;
import nva.commons.apigateway.RequestInfo;
import nva.commons.apigateway.exceptions.ApiGatewayException;
import nva.commons.core.Environment;
import nva.commons.core.JacocoGenerated;
import nva.commons.core.JsonUtils;
import nva.commons.core.ioutils.IoUtils;
import nva.commons.core.paths.UnixPath;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;

//Todo: find our own Response object
public class ScopusHandler implements RequestStreamHandler {

    protected static final String BUCKET_NAME = "BUCKET_NAME";
    public static final String FILE_IDENTIFIER = "fileIdentifier";

    private static final Logger logger = LoggerFactory.getLogger(ScopusHandler.class);


    @JacocoGenerated
    public ScopusHandler() {
    }


//    @Override
//    protected String processInput(Void input, RequestInfo requestInfo, Context context) throws ApiGatewayException {
//        String filename = requestInfo.getQueryParameter(FILE_IDENTIFIER);
//        String contents = s3Driver.getFile(UnixPath.fromString(filename));
//        // parse file to a ScopusPublication
////              ScopusPublication scopusPublication = publicationConverter.convert(contents);
//        // enrich contributors with help of nva-cristin-service
//        // enrich organizations with help of nva-cristin-service
//        // enrich journal with help of nva-publication-channels
//        // enrich publisher with help of nva-publication-channels
//        // metadataTransform ScopusPublication into CreatePublicationRequest
//        metadataService.toString();
//        // send CreatePublicationRequest to nva-publication-service
//        publicationPersistenceService.toString();
//        return contents;
//    }


    @Override
    public void handleRequest(InputStream input, OutputStream output, Context context) throws IOException {
        String streamString = IoUtils.streamToString(input);
        S3Event event = JsonUtils.dtoObjectMapper.readValue(streamString, S3Event.class);
        try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(output))) {
            writer.write(event.getRecords().get(0).getS3().getObject().getKey());
        }
    }
}
