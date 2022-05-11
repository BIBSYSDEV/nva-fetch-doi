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

public class ScopusEmailHandler implements RequestHandler<S3Event, String> {

    public static final int SINGLE_EXPECTED_RECORD = 0;
    public static final String S3_URI_TEMPLATE = "s3://%s/%s";
    private static final Logger logger = LoggerFactory.getLogger(ScopusEmailHandler.class);
    private static final String ZIP_FILE_BUCKET = new Environment().readEnv("ZIP_FILE_BUCKET");
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
    public String handleRequest(S3Event event, Context context) {
        // 1. Get download url from email
        // 2. Download zip files to zip-bucket

        return attempt(() -> readFile(event))
                .map(this::processEmail)
                .orElseThrow(fail -> logErrorAndThrowException(fail.getException()));
    }

    private String readFile(S3Event event) {
        var s3Driver = new S3Driver(s3Client, extractBucketName(event));
        var fileUri = createS3BucketUri(event);
        return s3Driver.getFile(new UriWrapper(fileUri).toS3bucketPath());
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

    private String processEmail(String emailContents) {
        extractUrlsFromEmail(emailContents).forEach(this::downloadFile);
        return emailContents;
    }

    private void downloadFile(String url) {
        try {
            URI uri = URI.create(url);
            String filename = extractFilenameFromUrl(url);
            HttpRequest request = HttpRequest.newBuilder().uri(uri).build();
            InputStream inputStream = httpClient.send(request, HttpResponse.BodyHandlers.ofInputStream()).body();
            s3Client.putObject(newPutObjectRequest(filename), createRequestBody(inputStream));
        } catch (Exception ignored) {

        }

    }

    private String extractFilenameFromUrl(String url) {
        return url;
    }

    private List<String> extractUrlsFromEmail(String emailContents) {
        ArrayList<String> urls = new ArrayList<>();
        urls.add(emailContents);
        return urls;
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
