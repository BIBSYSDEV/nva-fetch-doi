package no.sikt.nva.scopus;

import com.amazonaws.SdkClientException;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.ObjectListing;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.s3.model.S3ObjectInputStream;
import com.amazonaws.services.s3.model.S3ObjectSummary;
import org.junit.Test;
import org.junit.jupiter.api.BeforeEach;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ScopusS3ClientTest {

    public static final String PREFIX_FILENAME_1 = "prefixFilename1";
    public static final String PREFIX_FILENAME_2 = "prefixFilename2";
    public static final String FAILING = "I am failing!";
    private AmazonS3 amazonS3Client;
    private final String bucketName = "bucketName";
    private final String filename = "filename";
    private final String prefix = "prefix";

    @BeforeEach
    public void setUp() {
        amazonS3Client = mock(AmazonS3.class);
    }

    @Test
    public void testGetFile() {
        S3Object s3object = mock(S3Object.class);
        S3ObjectInputStream s3InputStream = mock(S3ObjectInputStream.class);
        ScopusS3Client client =  new ScopusS3Client(amazonS3Client, bucketName);
        when(amazonS3Client.getObject(bucketName, filename)).thenReturn(s3object);
        when(s3object.getObjectContent()).thenReturn(s3InputStream);
        InputStream inputStream = client.getFile(filename);
        assertEquals(s3InputStream, inputStream);
    }

    @Test
    public void testGetFileRunIntoException() {
        ScopusS3Client client =  new ScopusS3Client(amazonS3Client, bucketName);
        when(amazonS3Client.getObject(bucketName, filename)).thenThrow(new SdkClientException(FAILING));
        InputStream inputStream = client.getFile(filename);
        assertNull(inputStream);
    }

    @Test
    public void testListFiles() {
        ObjectListing s3objectListing = mock(ObjectListing.class);
        when(amazonS3Client.listObjects(bucketName, prefix)).thenReturn(s3objectListing);
        List<S3ObjectSummary> objects = mockS3ObjectSummaryList();
        when(s3objectListing.getObjectSummaries()).thenReturn(objects);
        ScopusS3Client client =  new ScopusS3Client(amazonS3Client, bucketName);
        List<String> filenames = client.listFiles(prefix);
        assertEquals(objects.size(), filenames.size());
        assertEquals(objects.get(0).getKey(), filenames.get(0));
        assertEquals(PREFIX_FILENAME_1, filenames.get(0));
    }

    @Test
    public void testListFilesGetException() {
        when(amazonS3Client.listObjects(bucketName, prefix)).thenThrow(new SdkClientException(FAILING));
        ScopusS3Client client =  new ScopusS3Client(amazonS3Client, bucketName);
        List<String> filenames = client.listFiles(prefix);
        assertTrue(filenames.isEmpty());
    }

    private List<S3ObjectSummary> mockS3ObjectSummaryList() {
        List<S3ObjectSummary> objects = new ArrayList<>();
        S3ObjectSummary objectSummary1 = new S3ObjectSummary();
        objectSummary1.setKey(PREFIX_FILENAME_1);
        objects.add(objectSummary1);
        S3ObjectSummary objectSummary2 = new S3ObjectSummary();
        objectSummary2.setKey(PREFIX_FILENAME_2);
        objects.add(objectSummary2);
        return objects;
    }

}