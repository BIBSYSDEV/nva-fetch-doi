package no.sikt.nva.scopus;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.S3Event;
import java.net.URI;
import nva.commons.core.JacocoGenerated;

public class ScopusHandler implements RequestHandler<S3Event, String> {

    @JacocoGenerated
    public ScopusHandler() {
    }

    @Override
    public String handleRequest(S3Event event, Context context) {
        String key = event.getRecords().get(0).getS3().getObject().getKey();
        String bucket = event.getRecords().get(0).getS3().getBucket().getName();
        var uri= URI.create(String.format("s3://%s/%s", bucket, key)).toString();
        System.out.println(uri);
        return uri;
    }
}
