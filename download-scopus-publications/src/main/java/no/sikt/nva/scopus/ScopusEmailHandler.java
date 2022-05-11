package no.sikt.nva.scopus;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.S3Event;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;

import no.unit.nva.s3.S3Driver;
import nva.commons.core.Environment;
import nva.commons.core.JacocoGenerated;
import nva.commons.core.ioutils.IoUtils;
import nva.commons.core.paths.UriWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import static nva.commons.core.attempt.Try.attempt;
import static software.amazon.awssdk.http.HttpStatusCode.OK;

public class ScopusEmailHandler implements RequestHandler<S3Event, Void> {

    public static final int SINGLE_EXPECTED_RECORD = 0;
    public static final String S3_URI_TEMPLATE = "s3://%s/%s";
    private static final Logger logger = LoggerFactory.getLogger(ScopusEmailHandler.class);
    private static final String ZIP_FILE_BUCKET = new Environment().readEnv("ZIP_FILE_BUCKET");
    public static final String ERROR_STORING_SCOPUS_ZIP_FILE_FROM_URL = "Error storing file '%s' from url: '%s'";
    public static final String SCOPUS_ZIP_FILE_STORED_FROM_URL = "File '%s' stored from url: '%s'";
    public static final String ERROR_UNEXPECTED_STATUS_CODE = "Unexpected status code '%s' for request to uri";
    private final S3Client s3Client;
    private final HttpClient httpClient;

    @JacocoGenerated
    public ScopusEmailHandler() {
        this(S3Driver.defaultS3Client().build(), HttpClient.newBuilder().build());
    }

    public ScopusEmailHandler(S3Client s3Client, HttpClient httpClient) {
        this.s3Client = s3Client;
        this.httpClient = httpClient;
    }

    @Override
    public Void handleRequest(S3Event event, Context context) {
        return attempt(() -> readFile(event))
                .map(this::processEmailFile)
                .orElseThrow(fail -> logErrorAndThrowException(fail.getException()));
    }

    private String readFile(S3Event event) {
        var s3Driver = new S3Driver(s3Client, extractBucketName(event));
        var fileUri = createS3BucketUri(event);
        return s3Driver.getFile(UriWrapper.fromUri(fileUri).toS3bucketPath());
    }

    private String extractBucketName(S3Event event) {
        return event.getRecords().get(SINGLE_EXPECTED_RECORD).getS3().getBucket().getName();
    }

    private String extractFilename(S3Event event) {
        return event.getRecords().get(SINGLE_EXPECTED_RECORD).getS3().getObject().getKey();
    }

    private URI createS3BucketUri(S3Event s3Event) {
        return URI.create(String.format(S3_URI_TEMPLATE, extractBucketName(s3Event), extractFilename(s3Event)));
    }

    private Void processEmailFile(String emailContents) {
        extractValidUrlsFromEmail(emailContents).forEach(this::downloadFile);
        return null;
    }

    private List<URI> extractValidUrlsFromEmail(String emailContents) {
        // TODO: extract valid scopus urls
        ArrayList<URI> uriList = new ArrayList<>();
        uriList.add(URI.create(emailContents));
        return uriList;
    }

    private void downloadFile(URI uri) {
        String filename = extractFilenameFromUri(uri);
        HttpRequest request = HttpRequest.newBuilder().uri(uri).build();
        try {
            HttpResponse<InputStream> httpResponse =
                    httpClient.send(request, HttpResponse.BodyHandlers.ofInputStream());
            if (httpResponse.statusCode() == OK) {
                s3Client.putObject(newPutObjectRequest(filename), createRequestBody(httpResponse.body()));
                logger.info(String.format(SCOPUS_ZIP_FILE_STORED_FROM_URL, filename, uri));
            } else {
                throw new IOException(String.format(ERROR_UNEXPECTED_STATUS_CODE, httpResponse.statusCode()));
            }
        } catch (IOException | InterruptedException e) {
            logger.error(String.format(ERROR_STORING_SCOPUS_ZIP_FILE_FROM_URL, filename, uri), e);
            throw new RuntimeException(e);
        }
    }

    private String extractFilenameFromUri(URI uri) {
        String path = uri.getPath();
        return path.substring(path.lastIndexOf('/') + 1);
    }

    private RequestBody createRequestBody(InputStream input) throws IOException {
        byte[] bytes = IoUtils.inputStreamToBytes(input);
        return RequestBody.fromBytes(bytes);
    }

    private PutObjectRequest newPutObjectRequest(String objectKey) {
        return PutObjectRequest.builder()
                .bucket(ZIP_FILE_BUCKET)
                .key(objectKey)
                .build();
    }

    private RuntimeException logErrorAndThrowException(Exception exception) {
        logger.error(exception.getMessage());
        return exception instanceof RuntimeException
                ? (RuntimeException) exception
                : new RuntimeException(exception);
    }
}
