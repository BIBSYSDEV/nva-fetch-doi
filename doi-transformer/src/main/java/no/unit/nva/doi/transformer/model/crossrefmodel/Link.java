package no.unit.nva.doi.transformer.model.crossrefmodel;

import com.fasterxml.jackson.annotation.JsonProperty;
import nva.commons.utils.JacocoGenerated;

@SuppressWarnings("PMD.ShortClassName")
public class Link {
    /*
        "URL": "http:\/\/link.springer.com\/content\/pdf\/10.1007\/s00115-004-1822-4.pdf",
        "content-type": "application\/pdf",
        "content-version": "vor",
        "intended-application": "text-mining"
     */

    @JsonProperty("URL")
    private String url;
    @JsonProperty("content-type")
    private String contentType;
    @JsonProperty("content-version")
    private String contentVersion;
    @JsonProperty("intended-application")
    private String intendedApplication;

    @JacocoGenerated
    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    @JacocoGenerated
    public String getContentType() {
        return contentType;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    @JacocoGenerated
    public String getContentVersion() {
        return contentVersion;
    }

    public void setContentVersion(String contentVersion) {
        this.contentVersion = contentVersion;
    }

    @JacocoGenerated
    public String getIntendedApplication() {
        return intendedApplication;
    }

    public void setIntendedApplication(String intendedApplication) {
        this.intendedApplication = intendedApplication;
    }
}
