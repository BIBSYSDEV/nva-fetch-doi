package no.unit.nva.doi;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;

public class DataciteClient {

    public static final String DATACITE_BASE_URL_STRING = "https://data.datacite.org";

    protected URL createRequestUrl(String doiUrlString, DataciteContentType dataciteContentType)
        throws MalformedURLException {
        URL dataciteBaseUrl = new URL(DATACITE_BASE_URL_STRING);
        URL doiUrl = new URL(doiUrlString);
        return new URL(dataciteBaseUrl,
                       String.join("",
                                   dataciteContentType.getContentType(),
                                   doiUrl.getPath()));
    }

    public MetadataAndContentLocation fetchMetadata(String doiUrlString, DataciteContentType dataciteContentType)
        throws IOException {
        String json = readStringFromUrl(createRequestUrl(doiUrlString, dataciteContentType));
        return new MetadataAndContentLocation(DATACITE_BASE_URL_STRING, json);
    }

    protected String readStringFromUrl(URL url) throws IOException {
        try (Scanner scanner = new Scanner(url.openStream(),
                                           StandardCharsets.UTF_8.toString())) {
            scanner.useDelimiter("\\A");
            return scanner.hasNext() ? scanner.next() : "";
        }
    }
}
