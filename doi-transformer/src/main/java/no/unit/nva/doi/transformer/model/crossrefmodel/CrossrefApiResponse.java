package no.unit.nva.doi.transformer.model.crossrefmodel;

import com.fasterxml.jackson.annotation.JsonProperty;
import nva.commons.utils.JacocoGenerated;

public class CrossrefApiResponse {

    @JsonProperty("status")
    private String status;
    @JsonProperty("message-type")
    private String messageType;
    @JsonProperty("message-version")
    private String messageVersion;
    @JsonProperty("message")
    private CrossRefDocument message;

    @JacocoGenerated
    public String getStatus() {
        return status;
    }

    public void setStatus(String input) {
        this.status = input;
    }

    @JacocoGenerated
    public String getMessageType() {
        return messageType;
    }

    public void setMessageType(String input) {
        this.messageType = input;
    }

    @JacocoGenerated
    public String getMessageVersion() {
        return messageVersion;
    }

    public void setMessageVersion(String input) {
        this.messageVersion = input;
    }

    public CrossRefDocument getMessage() {
        return message;
    }

    public void setMessage(CrossRefDocument input) {
        this.message = input;
    }
}

