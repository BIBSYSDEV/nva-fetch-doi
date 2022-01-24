package no.sikt.nva.scopus;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.S3Event;

import java.net.URI;
import java.util.List;
import java.util.stream.Collectors;

import no.unit.nva.s3.S3Driver;
import nva.commons.core.JacocoGenerated;
import nva.commons.core.paths.UriWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.services.s3.S3Client;

public class ScopusDeleteHandler implements RequestHandler<S3Event, List<String>> {

    public static final int SINGLE_EXPECTED_RECORD = 0;
    public static final String S3_URI_TEMPLATE = "s3://%s/%s";
    public static final String DELETE_SCOPUS_IDENTIFIER_PREFIX = "DELETE-2-s2.0-";
    public static final String EMPTY_STRING = "";
    private static final Logger logger = LoggerFactory.getLogger(ScopusDeleteHandler.class);
    private final S3Client s3Client;

    @JacocoGenerated
    public ScopusDeleteHandler() {
        this(S3Driver.defaultS3Client().build());
    }

    public ScopusDeleteHandler(S3Client s3Client) {
        this.s3Client = s3Client;
    }

    @Override
    public List<String> handleRequest(S3Event event, Context context) {
        logger.info("Reading file from s3");
        return readFile(event)
                .lines()
                .map(this::trimDeleteScopusPrefix)
                .collect(Collectors.toList());
    }

    private String trimDeleteScopusPrefix(String line) {
        return line.replace(DELETE_SCOPUS_IDENTIFIER_PREFIX, EMPTY_STRING).trim();
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
}
