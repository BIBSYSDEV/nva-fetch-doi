package no.unit.nva.doi.transformer.model.crossrefmodel;

import com.fasterxml.jackson.annotation.JsonProperty;
import nva.commons.core.JacocoGenerated;

@JacocoGenerated
public class CrossrefReview {

    @JsonProperty("running-number")
    private String runningNumber;
    @JsonProperty("revision-round")
    private String revisionRound;
    @JsonProperty("stage")
    private String stage;
    @JsonProperty("recommendation")
    private String recommendation;
    @JsonProperty("type")
    private String type;
    @JsonProperty("competing-interest-statement")
    private String competingInterestStatement;
    @JsonProperty("language")
    private String language;

    public String getRunningNumber() {
        return runningNumber;
    }

    public void setRunningNumber(String runningNumber) {
        this.runningNumber = runningNumber;
    }

    public String getRevisionRound() {
        return revisionRound;
    }

    public void setRevisionRound(String revisionRound) {
        this.revisionRound = revisionRound;
    }

    public String getStage() {
        return stage;
    }

    public void setStage(String stage) {
        this.stage = stage;
    }

    public String getRecommendation() {
        return recommendation;
    }

    public void setRecommendation(String recommendation) {
        this.recommendation = recommendation;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getCompetingInterestStatement() {
        return competingInterestStatement;
    }

    public void setCompetingInterestStatement(String competingInterestStatement) {
        this.competingInterestStatement = competingInterestStatement;
    }

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }
}
