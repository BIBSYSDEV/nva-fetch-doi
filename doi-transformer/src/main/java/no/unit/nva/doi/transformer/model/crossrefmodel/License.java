package no.unit.nva.doi.transformer.model.crossrefmodel;

import com.fasterxml.jackson.annotation.JsonProperty;

public class License {

    @JsonProperty("URL")
    private String url;
    @JsonProperty("start")
    private CrossrefDate start;
    @JsonProperty("delay-in-days")
    private int delayInDays;
    @JsonProperty("content-version")
    private String contentVersion;

    public String getUrl() {
        return url;
    }

    public void setUrl(String input) {
        this.url = input;
    }

    public CrossrefDate getStart() {
        return start;
    }

    public void setStart(CrossrefDate input) {
        this.start = input;
    }

    public int getDelayInDays() {
        return delayInDays;
    }

    public void setDelayInDays(int input) {
        this.delayInDays = input;
    }

    public String getContentVersion() {
        return contentVersion;
    }

    public void setContentVersion(String input) {
        this.contentVersion = input;
    }
}

