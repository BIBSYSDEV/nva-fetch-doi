package no.unit.nva.doi.transformer.utils;

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

}
