package no.unit.nva.doi.transformer.model.crossrefmodel;

import com.fasterxml.jackson.annotation.JsonProperty;
import nva.commons.utils.JacocoGenerated;

public class License {

    @JsonProperty("URL")
    private String url;
    @JsonProperty("start")
    private CrossrefDate start;
    @JsonProperty("delay-in-days")
    private int delayInDays;
    @JsonProperty("content-version")
    private String contentVersion;

    @JacocoGenerated
    public License() {
    }

    @JacocoGenerated
    public String getUrl() {
        return url;
    }

    @JacocoGenerated
    public void setUrl(String input) {
        this.url = input;
    }

    @JacocoGenerated
    public CrossrefDate getStart() {
        return start;
    }

    @JacocoGenerated
    public void setStart(CrossrefDate input) {
        this.start = input;
    }

    @JacocoGenerated
    public int getDelayInDays() {
        return delayInDays;
    }

    @JacocoGenerated
    public void setDelayInDays(int input) {
        this.delayInDays = input;
    }

    @JacocoGenerated
    public String getContentVersion() {
        return contentVersion;
    }

    @JacocoGenerated
    public void setContentVersion(String input) {
        this.contentVersion = input;
    }
}

