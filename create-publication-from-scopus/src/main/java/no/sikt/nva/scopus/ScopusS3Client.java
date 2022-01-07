package no.sikt.nva.scopus;

import com.amazonaws.SdkClientException;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;

import java.io.InputStream;
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
    public static final String COULD_NOT_GET_FILE = "Could not get file: {}";
    public static final String COULD_NOT_LIST_FILES_FOR = "Could not list files for: {}";

    private String bucketName;
    private AmazonS3 amazonS3Client;
    static final String AWS_REGION = "AWS_REGION";
    static final String BUCKET_NAME = "BUCKET_NAME";
    public static final String CANNOT_CONNECT_TO_S3 = "Cannot connect to S3";

    @JacocoGenerated
    public ScopusS3Client() {
        initS3Client(new Environment());
    }

    @JacocoGenerated
    public ScopusS3Client(Environment environment) {
        initS3Client(environment);
    }

    /**
     * Constructor for use in test to inject.
     *
     * @param amazonS3Client aws S3 client
     * @param bucketName     name til s3 bucket
     */
    public ScopusS3Client(AmazonS3 amazonS3Client, String bucketName) {
        this.amazonS3Client = amazonS3Client;
        this.bucketName = bucketName;
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

    /**
     * Get single file from s3 by filename.
     * @param filename name of the file to be retrieved
     * @return file as inputStream
     */
    protected InputStream getFile(String filename) {
        try {
            S3Object object = amazonS3Client.getObject(bucketName, filename);
            return object.getObjectContent();
        } catch (SdkClientException e) {
            logger.error(COULD_NOT_GET_FILE, filename, e);
        }
        return null;
    }

    /**
     * Get a list of files from s3 by given prefix (structured path on s3).
     * @param prefix structured path on s3 binding files together
     * @return list of filenames
     */
    protected List<String> listFiles(String prefix) {
        List<String> fileNames = new ArrayList<>();
        try {
            ObjectListing result = amazonS3Client.listObjects(bucketName, prefix);
            List<S3ObjectSummary> objects = result.getObjectSummaries();
            objects.forEach(obj -> fileNames.add(obj.getKey()));
        } catch (SdkClientException e) {
            logger.error(COULD_NOT_LIST_FILES_FOR, prefix, e);
        }
        return fileNames;
    }


}