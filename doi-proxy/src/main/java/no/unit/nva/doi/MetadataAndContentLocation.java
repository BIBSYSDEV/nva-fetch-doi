package no.unit.nva.doi;

import java.util.Collections;
import java.util.Map;
import javax.ws.rs.core.HttpHeaders;

public class MetadataAndContentLocation {

    private final String contentHeader;
    private final String json;

    public MetadataAndContentLocation(String contentHeader, String json) {
        this.contentHeader = contentHeader;
        this.json = json;
    }

    public String getContentHeader() {
        return contentHeader;
    }

    public String getJson() {
        return json;
    }

    public Map<String, String> contentLocationAsHeaderEntry() {
        return Collections.singletonMap(HttpHeaders.CONTENT_LOCATION, getContentHeader());
    }
}
