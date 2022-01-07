package no.sikt.nva.scopus;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.util.ArrayList;
import java.util.List;

import com.amazonaws.services.s3.model.ObjectListing;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.s3.model.S3ObjectSummary;
import nva.commons.core.Environment;
import nva.commons.core.JacocoGenerated;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ScopusS3Client {

    private static final Logger logger = LoggerFactory.getLogger(ScopusS3Client.class);

    private String bucketName;
    private AmazonS3 amazonS3Client;
    private HttpURLConnection connection;
    private static final String AWS_REGION = "AWS_REGION";
    private static final String BUCKET_NAME = "BUCKET_NAME";
    public static final String CANNOT_CONNECT_TO_S3 = "Cannot connect to S3";

    public ScopusS3Client() {
        initS3Client(new Environment());
    }

    public ScopusS3Client(Environment environment) {
        initS3Client(environment);
    }

    /**
     * Constructor for use in test to inject.
     *
     * @param amazonS3Client aws S3 client
     * @param bucketName     name til s3 bucket
     * @param connection     httpConnection
     */
    public ScopusS3Client(AmazonS3 amazonS3Client, String bucketName, HttpURLConnection connection) {
        this.amazonS3Client = amazonS3Client;
        this.bucketName = bucketName;
        this.connection = connection;
    }

    @JacocoGenerated
    private void initS3Client(Environment environment) {
        try {
            this.amazonS3Client = AmazonS3ClientBuilder.standard()
                    .withRegion(environment.readEnv(AWS_REGION))
                    .withPathStyleAccessEnabled(true)
                    .build();
            this.bucketName = environment.readEnv(BUCKET_NAME);
        } catch (Exception e) {
            logger.error(CANNOT_CONNECT_TO_S3, e);
        }
    }

    protected InputStream getFile(String filename) {
        S3Object xFile = amazonS3Client.getObject(bucketName, filename);
        return xFile.getObjectContent();
    }

    protected List<String> listFiles(String prefix) {
        List<String> fileNames = new ArrayList<>();
        ObjectListing result = amazonS3Client.listObjects(bucketName, prefix);
        List<S3ObjectSummary> objects = result.getObjectSummaries();
        objects.forEach(obj -> fileNames.add(obj.getKey()));
        return fileNames;
    }


}