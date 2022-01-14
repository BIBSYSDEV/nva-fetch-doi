package no.sikt.nva.scopus;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.S3Event;
import nva.commons.core.JacocoGenerated;

public class ScopusHandler implements RequestHandler<S3Event, String> {

    @JacocoGenerated
    public ScopusHandler() {
    }


    @JacocoGenerated
    @Override
    public String handleRequest(S3Event event, Context context)  {
        String key = event.getRecords().get(0).getS3().getObject().getKey();
        System.out.println("Do we see something here? " + key);
        return key;
    }
}
